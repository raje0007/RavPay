package org.example.model;

import java.time.LocalDateTime;

/**
 * Represents a digital wallet associated with a RevPay user.
 * Manages the user's balance and tracks top-up / withdrawal history at a high level.
 */
public class Wallet {

    // ─── Identity ─────────────────────────────────────────────────────────────
    private int    walletId;
    private String walletNumber;          // unique wallet number (e.g. WLT-000001)

    // ─── Owner Reference ──────────────────────────────────────────────────────
    private int    userId;                // FK → User.userId
    private String accountId;            // denormalized for quick lookup

    // ─── Balance ──────────────────────────────────────────────────────────────
    private double balance;
    private double pendingBalance;        // funds held for pending outgoing transactions
    private String currency;             // ISO-4217, default "USD"

    // ─── Limits ───────────────────────────────────────────────────────────────
    private double dailySendLimit;
    private double dailySendUsed;         // resets at midnight
    private double dailyTopUpLimit;
    private double dailyTopUpUsed;        // resets at midnight
    private LocalDateTime limitsResetAt;  // next midnight reset timestamp

    // ─── Status ───────────────────────────────────────────────────────────────
    private WalletStatus status;

    // ─── Timestamps ───────────────────────────────────────────────────────────
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ═════════════════════════════════════════════════════════════════════════
    // Enum
    // ═════════════════════════════════════════════════════════════════════════

    public enum WalletStatus {
        ACTIVE,
        FROZEN,    // temporarily blocked (e.g. suspicious activity)
        CLOSED
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Constructors
    // ═════════════════════════════════════════════════════════════════════════

    public Wallet() {
        this.balance          = 0.0;
        this.pendingBalance   = 0.0;
        this.currency         = "USD";
        this.dailySendLimit   = 5000.0;
        this.dailySendUsed    = 0.0;
        this.dailyTopUpLimit  = 10000.0;
        this.dailyTopUpUsed   = 0.0;
        this.status           = WalletStatus.ACTIVE;
        this.createdAt        = LocalDateTime.now();
        this.updatedAt        = LocalDateTime.now();
        this.limitsResetAt    = LocalDateTime.now().plusDays(1)
                .withHour(0).withMinute(0).withSecond(0).withNano(0);
    }

    public Wallet(int userId, String accountId, String walletNumber) {
        this();
        this.userId      = userId;
        this.accountId   = accountId;
        this.walletNumber = walletNumber;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Business Logic
    // ═════════════════════════════════════════════════════════════════════════

    public boolean isActive() {
        return this.status == WalletStatus.ACTIVE;
    }

    /**
     * Returns the spendable (available) balance — balance minus pending outgoing funds.
     */
    public double getAvailableBalance() {
        return this.balance - this.pendingBalance;
    }

    /**
     * Check if the wallet has enough available balance for a given amount.
     */
    public boolean hasSufficientBalance(double amount) {
        return getAvailableBalance() >= amount;
    }

    /**
     * Check if a send operation is within the remaining daily limit.
     */
    public boolean isWithinDailySendLimit(double amount) {
        resetDailyLimitsIfNeeded();
        return (dailySendUsed + amount) <= dailySendLimit;
    }

    /**
     * Check if a top-up operation is within the remaining daily limit.
     */
    public boolean isWithinDailyTopUpLimit(double amount) {
        resetDailyLimitsIfNeeded();
        return (dailyTopUpUsed + amount) <= dailyTopUpLimit;
    }

    /**
     * Debit the wallet for a completed outgoing transaction.
     * Reduces balance and registers daily usage.
     */
    public void debit(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Debit amount must be positive.");
        if (!hasSufficientBalance(amount)) throw new IllegalStateException("Insufficient balance.");
        this.balance      -= amount;
        this.dailySendUsed += amount;
        this.updatedAt     = LocalDateTime.now();
    }

    /**
     * Credit the wallet for an incoming transaction or top-up.
     */
    public void credit(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Credit amount must be positive.");
        this.balance   += amount;
        this.updatedAt  = LocalDateTime.now();
    }

    /**
     * Hold funds in pending balance (e.g. awaiting recipient acceptance).
     */
    public void holdFunds(double amount) {
        if (!hasSufficientBalance(amount)) throw new IllegalStateException("Insufficient balance to hold.");
        this.pendingBalance += amount;
        this.updatedAt       = LocalDateTime.now();
    }

    /**
     * Release previously held funds back to available balance (e.g. request declined).
     */
    public void releaseFunds(double amount) {
        if (this.pendingBalance < amount) throw new IllegalStateException("Cannot release more than held.");
        this.pendingBalance -= amount;
        this.updatedAt       = LocalDateTime.now();
    }

    /**
     * Commit held funds by removing from pending AND balance (transaction completed).
     */
    public void commitHeldFunds(double amount) {
        if (this.pendingBalance < amount) throw new IllegalStateException("Cannot commit more than held.");
        this.pendingBalance -= amount;
        this.balance        -= amount;
        this.dailySendUsed  += amount;
        this.updatedAt       = LocalDateTime.now();
    }

    /**
     * Resets daily send/top-up counters if midnight has passed.
     */
    private void resetDailyLimitsIfNeeded() {
        if (LocalDateTime.now().isAfter(limitsResetAt)) {
            this.dailySendUsed  = 0.0;
            this.dailyTopUpUsed = 0.0;
            this.limitsResetAt  = LocalDateTime.now().plusDays(1)
                    .withHour(0).withMinute(0).withSecond(0).withNano(0);
        }
    }

    public double getRemainingDailySendLimit() {
        resetDailyLimitsIfNeeded();
        return dailySendLimit - dailySendUsed;
    }

    public double getRemainingDailyTopUpLimit() {
        resetDailyLimitsIfNeeded();
        return dailyTopUpLimit - dailyTopUpUsed;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Getters & Setters
    // ═════════════════════════════════════════════════════════════════════════

    public int getWalletId() { return walletId; }
    public void setWalletId(int walletId) { this.walletId = walletId; }

    public String getWalletNumber() { return walletNumber; }
    public void setWalletNumber(String walletNumber) { this.walletNumber = walletNumber; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; this.updatedAt = LocalDateTime.now(); }

    public double getPendingBalance() { return pendingBalance; }
    public void setPendingBalance(double pendingBalance) { this.pendingBalance = pendingBalance; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public double getDailySendLimit() { return dailySendLimit; }
    public void setDailySendLimit(double dailySendLimit) { this.dailySendLimit = dailySendLimit; }

    public double getDailySendUsed() { return dailySendUsed; }
    public void setDailySendUsed(double dailySendUsed) { this.dailySendUsed = dailySendUsed; }

    public double getDailyTopUpLimit() { return dailyTopUpLimit; }
    public void setDailyTopUpLimit(double dailyTopUpLimit) { this.dailyTopUpLimit = dailyTopUpLimit; }

    public double getDailyTopUpUsed() { return dailyTopUpUsed; }
    public void setDailyTopUpUsed(double dailyTopUpUsed) { this.dailyTopUpUsed = dailyTopUpUsed; }

    public LocalDateTime getLimitsResetAt() { return limitsResetAt; }
    public void setLimitsResetAt(LocalDateTime limitsResetAt) { this.limitsResetAt = limitsResetAt; }

    public WalletStatus getStatus() { return status; }
    public void setStatus(WalletStatus status) { this.status = status; this.updatedAt = LocalDateTime.now(); }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "Wallet{" +
                "walletNumber='" + walletNumber + '\'' +
                ", balance=" + balance +
                ", pendingBalance=" + pendingBalance +
                ", currency='" + currency + '\'' +
                ", status=" + status +
                '}';
    }
}

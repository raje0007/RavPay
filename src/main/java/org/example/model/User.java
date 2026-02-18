package org.example.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class representing a user in the RevPay system.
 * Both PersonalUser and BusinessUser extend this class.
 */
public abstract class User {

    // ─── Identity ────────────────────────────────────────────────────────────
    private int userId;
    private String accountId;          // unique public-facing ID (e.g. RVP-000001)
    private String username;
    private String email;
    private String phoneNumber;

    // ─── Credentials ─────────────────────────────────────────────────────────
    private String passwordHash;       // bcrypt
    private String transactionPin;     // bcrypt-hashed 4-6 digit PIN
    private String securityQuestion1;
    private String securityAnswer1;    // hashed
    private String securityQuestion2;
    private String securityAnswer2;    // hashed

    // ─── Account Status ───────────────────────────────────────────────────────
    private AccountStatus status;      // ACTIVE, LOCKED, SUSPENDED, CLOSED
    private int failedLoginAttempts;
    private LocalDateTime lockedUntil;
    private LocalDateTime lastLoginAt;
    private LocalDateTime sessionExpiresAt;
    private boolean twoFactorEnabled;
    private String pendingTwoFactorCode;       // simulated 2FA
    private LocalDateTime twoFactorCodeExpiry;

    // ─── Timestamps ───────────────────────────────────────────────────────────
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ─── Relationships ────────────────────────────────────────────────────────
    private Wallet wallet;
    private List<PaymentMethod> paymentMethods = new ArrayList<>();
    private List<Transaction>   transactions   = new ArrayList<>();
    private List<Notification>  notifications  = new ArrayList<>();

    // ─── Notification Preferences ─────────────────────────────────────────────
    private boolean notifyOnTransaction;
    private boolean notifyOnMoneyRequest;
    private boolean notifyOnCardChange;
    private boolean notifyOnLowBalance;
    private double  lowBalanceThreshold;

    // ─── Account Type ─────────────────────────────────────────────────────────
    public enum AccountStatus {
        ACTIVE, LOCKED, SUSPENDED, CLOSED
    }

    public enum AccountType {
        PERSONAL, BUSINESS
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Constructor
    // ═════════════════════════════════════════════════════════════════════════

    public User() {
        this.status                  = AccountStatus.ACTIVE;
        this.failedLoginAttempts     = 0;
        this.twoFactorEnabled        = false;
        this.notifyOnTransaction     = true;
        this.notifyOnMoneyRequest    = true;
        this.notifyOnCardChange      = true;
        this.notifyOnLowBalance      = true;
        this.lowBalanceThreshold     = 100.0;
        this.createdAt               = LocalDateTime.now();
        this.updatedAt               = LocalDateTime.now();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Abstract Methods
    // ═════════════════════════════════════════════════════════════════════════

    public abstract AccountType getAccountType();
    public abstract String getDisplayName();

    // ═════════════════════════════════════════════════════════════════════════
    // Business Logic Helpers
    // ═════════════════════════════════════════════════════════════════════════

    public boolean isActive() {
        return this.status == AccountStatus.ACTIVE;
    }

    public boolean isLocked() {
        if (this.status == AccountStatus.LOCKED) {
            if (this.lockedUntil != null && LocalDateTime.now().isAfter(this.lockedUntil)) {
                // Auto-unlock after lockout period
                this.status              = AccountStatus.ACTIVE;
                this.failedLoginAttempts = 0;
                this.lockedUntil         = null;
                return false;
            }
            return true;
        }
        return false;
    }

    public boolean isSessionValid() {
        return this.sessionExpiresAt != null &&
                LocalDateTime.now().isBefore(this.sessionExpiresAt);
    }

    public void incrementFailedLogins() {
        this.failedLoginAttempts++;
        this.updatedAt = LocalDateTime.now();
    }

    public void resetFailedLogins() {
        this.failedLoginAttempts = 0;
        this.updatedAt           = LocalDateTime.now();
    }

    public void lockAccount(int minutesToLock) {
        this.status      = AccountStatus.LOCKED;
        this.lockedUntil = LocalDateTime.now().plusMinutes(minutesToLock);
        this.updatedAt   = LocalDateTime.now();
    }

    public void refreshSession(int sessionTimeoutMinutes) {
        this.sessionExpiresAt = LocalDateTime.now().plusMinutes(sessionTimeoutMinutes);
        this.lastLoginAt      = LocalDateTime.now();
        this.updatedAt        = LocalDateTime.now();
    }

    public void invalidateSession() {
        this.sessionExpiresAt = null;
        this.updatedAt        = LocalDateTime.now();
    }

    public void addNotification(Notification notification) {
        this.notifications.add(notification);
    }

    public void addPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethods.add(paymentMethod);
        this.updatedAt = LocalDateTime.now();
    }

    public void removePaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethods.remove(paymentMethod);
        this.updatedAt = LocalDateTime.now();
    }

    public void addTransaction(Transaction transaction) {
        this.transactions.add(transaction);
    }

    public long getUnreadNotificationCount() {
        return notifications.stream().filter(n -> !n.isRead()).count();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Getters & Setters
    // ═════════════════════════════════════════════════════════════════════════

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; this.updatedAt = LocalDateTime.now(); }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; this.updatedAt = LocalDateTime.now(); }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; this.updatedAt = LocalDateTime.now(); }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; this.updatedAt = LocalDateTime.now(); }

    public String getTransactionPin() { return transactionPin; }
    public void setTransactionPin(String transactionPin) { this.transactionPin = transactionPin; this.updatedAt = LocalDateTime.now(); }

    public String getSecurityQuestion1() { return securityQuestion1; }
    public void setSecurityQuestion1(String securityQuestion1) { this.securityQuestion1 = securityQuestion1; }

    public String getSecurityAnswer1() { return securityAnswer1; }
    public void setSecurityAnswer1(String securityAnswer1) { this.securityAnswer1 = securityAnswer1; }

    public String getSecurityQuestion2() { return securityQuestion2; }
    public void setSecurityQuestion2(String securityQuestion2) { this.securityQuestion2 = securityQuestion2; }

    public String getSecurityAnswer2() { return securityAnswer2; }
    public void setSecurityAnswer2(String securityAnswer2) { this.securityAnswer2 = securityAnswer2; }

    public AccountStatus getStatus() { return status; }
    public void setStatus(AccountStatus status) { this.status = status; this.updatedAt = LocalDateTime.now(); }

    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public void setFailedLoginAttempts(int failedLoginAttempts) { this.failedLoginAttempts = failedLoginAttempts; }

    public LocalDateTime getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(LocalDateTime lockedUntil) { this.lockedUntil = lockedUntil; }

    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    public LocalDateTime getSessionExpiresAt() { return sessionExpiresAt; }
    public void setSessionExpiresAt(LocalDateTime sessionExpiresAt) { this.sessionExpiresAt = sessionExpiresAt; }

    public boolean isTwoFactorEnabled() { return twoFactorEnabled; }
    public void setTwoFactorEnabled(boolean twoFactorEnabled) { this.twoFactorEnabled = twoFactorEnabled; }

    public String getPendingTwoFactorCode() { return pendingTwoFactorCode; }
    public void setPendingTwoFactorCode(String pendingTwoFactorCode) { this.pendingTwoFactorCode = pendingTwoFactorCode; }

    public LocalDateTime getTwoFactorCodeExpiry() { return twoFactorCodeExpiry; }
    public void setTwoFactorCodeExpiry(LocalDateTime twoFactorCodeExpiry) { this.twoFactorCodeExpiry = twoFactorCodeExpiry; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Wallet getWallet() { return wallet; }
    public void setWallet(Wallet wallet) { this.wallet = wallet; }

    public List<PaymentMethod> getPaymentMethods() { return paymentMethods; }
    public void setPaymentMethods(List<PaymentMethod> paymentMethods) { this.paymentMethods = paymentMethods; }

    public List<Transaction> getTransactions() { return transactions; }
    public void setTransactions(List<Transaction> transactions) { this.transactions = transactions; }

    public List<Notification> getNotifications() { return notifications; }
    public void setNotifications(List<Notification> notifications) { this.notifications = notifications; }

    public boolean isNotifyOnTransaction() { return notifyOnTransaction; }
    public void setNotifyOnTransaction(boolean notifyOnTransaction) { this.notifyOnTransaction = notifyOnTransaction; }

    public boolean isNotifyOnMoneyRequest() { return notifyOnMoneyRequest; }
    public void setNotifyOnMoneyRequest(boolean notifyOnMoneyRequest) { this.notifyOnMoneyRequest = notifyOnMoneyRequest; }

    public boolean isNotifyOnCardChange() { return notifyOnCardChange; }
    public void setNotifyOnCardChange(boolean notifyOnCardChange) { this.notifyOnCardChange = notifyOnCardChange; }

    public boolean isNotifyOnLowBalance() { return notifyOnLowBalance; }
    public void setNotifyOnLowBalance(boolean notifyOnLowBalance) { this.notifyOnLowBalance = notifyOnLowBalance; }

    public double getLowBalanceThreshold() { return lowBalanceThreshold; }
    public void setLowBalanceThreshold(double lowBalanceThreshold) { this.lowBalanceThreshold = lowBalanceThreshold; }

    @Override
    public String toString() {
        return "User{" +
                "accountId='" + accountId + '\'' +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", status=" + status +
                ", accountType=" + getAccountType() +
                '}';
    }
}
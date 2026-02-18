package org.example.model;
import java.time.LocalDateTime;

/**
 * Represents every financial movement in RevPay:
 * payments, money requests, top-ups, withdrawals, loan disbursements, and repayments.
 */
public class Transaction {

    // ─── Identity ─────────────────────────────────────────────────────────────
    private int    transactionId;
    private String referenceNumber;      // unique human-readable ref (e.g. TXN-20240101-000001)

    // ─── Parties ──────────────────────────────────────────────────────────────
    private int    senderUserId;
    private String senderAccountId;
    private String senderDisplayName;

    private int    receiverUserId;
    private String receiverAccountId;
    private String receiverDisplayName;

    // ─── Amount ───────────────────────────────────────────────────────────────
    private double amount;
    private double fee;                   // platform transaction fee
    private double netAmount;             // amount - fee received by the receiver
    private String currency;             // ISO-4217

    // ─── Classification ───────────────────────────────────────────────────────
    private TransactionType   type;
    private TransactionStatus status;

    // ─── Metadata ─────────────────────────────────────────────────────────────
    private String note;                  // optional memo from sender
    private String failureReason;         // populated on FAILED status
    private String invoiceId;             // linked invoice reference (nullable)
    private String loanId;               // linked loan reference (nullable)
    private int    paymentMethodId;      // card/bank used for top-up or withdrawal (0 = wallet)

    // ─── Money Request Fields (type == MONEY_REQUEST) ─────────────────────────
    private LocalDateTime requestExpiresAt;   // request auto-expires after N days

    // ─── Timestamps ───────────────────────────────────────────────────────────
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;

    // ═════════════════════════════════════════════════════════════════════════
    // Enums
    // ═════════════════════════════════════════════════════════════════════════

    public enum TransactionType {
        SEND,               // peer-to-peer payment
        MONEY_REQUEST,      // request money from another user
        TOP_UP,             // add money from external card/bank to wallet
        WITHDRAWAL,         // withdraw wallet funds to bank account
        INVOICE_PAYMENT,    // payment against a business invoice
        LOAN_DISBURSEMENT,  // loan funds credited to wallet
        LOAN_REPAYMENT,     // loan repayment debited from wallet
        REFUND              // reversal of a previous transaction
    }

    public enum TransactionStatus {
        PENDING,    // awaiting action (e.g. request not yet accepted)
        PROCESSING, // payment gateway processing
        COMPLETED,  // successfully settled
        FAILED,     // processing error or declined
        CANCELLED,  // cancelled by sender before completion
        DECLINED,   // receiver declined a money request
        EXPIRED     // money request passed its expiry without response
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Constructors
    // ═════════════════════════════════════════════════════════════════════════

    public Transaction() {
        this.currency  = "USD";
        this.fee       = 0.0;
        this.status    = TransactionStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Transaction(String referenceNumber, int senderUserId, String senderAccountId,
                       int receiverUserId, String receiverAccountId,
                       double amount, TransactionType type, String note) {
        this();
        this.referenceNumber    = referenceNumber;
        this.senderUserId       = senderUserId;
        this.senderAccountId    = senderAccountId;
        this.receiverUserId     = receiverUserId;
        this.receiverAccountId  = receiverAccountId;
        this.amount             = amount;
        this.netAmount          = amount - fee;
        this.type               = type;
        this.note               = note;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Business Logic Helpers
    // ═════════════════════════════════════════════════════════════════════════

    public boolean isPending()    { return status == TransactionStatus.PENDING;    }
    public boolean isCompleted()  { return status == TransactionStatus.COMPLETED;  }
    public boolean isFailed()     { return status == TransactionStatus.FAILED;     }
    public boolean isCancelled()  { return status == TransactionStatus.CANCELLED;  }

    public boolean isMoneyRequest() { return type == TransactionType.MONEY_REQUEST; }

    public boolean isRequestExpired() {
        return isMoneyRequest()
                && requestExpiresAt != null
                && LocalDateTime.now().isAfter(requestExpiresAt);
    }

    public void markCompleted() {
        this.status      = TransactionStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.updatedAt   = LocalDateTime.now();
    }

    public void markFailed(String reason) {
        this.status        = TransactionStatus.FAILED;
        this.failureReason = reason;
        this.updatedAt     = LocalDateTime.now();
    }

    public void markCancelled() {
        this.status    = TransactionStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }

    public void markDeclined() {
        this.status    = TransactionStatus.DECLINED;
        this.updatedAt = LocalDateTime.now();
    }

    public void markExpired() {
        this.status    = TransactionStatus.EXPIRED;
        this.updatedAt = LocalDateTime.now();
    }

    public void markProcessing() {
        this.status    = TransactionStatus.PROCESSING;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Compute net amount after applying fee.
     */
    public void applyFee(double feeAmount) {
        this.fee       = feeAmount;
        this.netAmount = this.amount - feeAmount;
        this.updatedAt = LocalDateTime.now();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Getters & Setters
    // ═════════════════════════════════════════════════════════════════════════

    public int getTransactionId() { return transactionId; }
    public void setTransactionId(int transactionId) { this.transactionId = transactionId; }

    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }

    public int getSenderUserId() { return senderUserId; }
    public void setSenderUserId(int senderUserId) { this.senderUserId = senderUserId; }

    public String getSenderAccountId() { return senderAccountId; }
    public void setSenderAccountId(String senderAccountId) { this.senderAccountId = senderAccountId; }

    public String getSenderDisplayName() { return senderDisplayName; }
    public void setSenderDisplayName(String senderDisplayName) { this.senderDisplayName = senderDisplayName; }

    public int getReceiverUserId() { return receiverUserId; }
    public void setReceiverUserId(int receiverUserId) { this.receiverUserId = receiverUserId; }

    public String getReceiverAccountId() { return receiverAccountId; }
    public void setReceiverAccountId(String receiverAccountId) { this.receiverAccountId = receiverAccountId; }

    public String getReceiverDisplayName() { return receiverDisplayName; }
    public void setReceiverDisplayName(String receiverDisplayName) { this.receiverDisplayName = receiverDisplayName; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; this.netAmount = amount - fee; }

    public double getFee() { return fee; }
    public void setFee(double fee) { this.fee = fee; this.netAmount = amount - fee; }

    public double getNetAmount() { return netAmount; }
    public void setNetAmount(double netAmount) { this.netAmount = netAmount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }

    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; this.updatedAt = LocalDateTime.now(); }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public String getInvoiceId() { return invoiceId; }
    public void setInvoiceId(String invoiceId) { this.invoiceId = invoiceId; }

    public String getLoanId() { return loanId; }
    public void setLoanId(String loanId) { this.loanId = loanId; }

    public int getPaymentMethodId() { return paymentMethodId; }
    public void setPaymentMethodId(int paymentMethodId) { this.paymentMethodId = paymentMethodId; }

    public LocalDateTime getRequestExpiresAt() { return requestExpiresAt; }
    public void setRequestExpiresAt(LocalDateTime requestExpiresAt) { this.requestExpiresAt = requestExpiresAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    @Override
    public String toString() {
        return "Transaction{" +
                "referenceNumber='" + referenceNumber + '\'' +
                ", type=" + type +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", status=" + status +
                ", from='" + senderAccountId + '\'' +
                ", to='" + receiverAccountId + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
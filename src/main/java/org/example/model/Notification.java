package org.example.model;


import java.time.LocalDateTime;

/**
 * Represents an in-app notification delivered to a RevPay user.
 * Covers transaction events, money requests, card changes, low balance alerts,
 * loan updates, invoice events, and system messages.
 */
public class Notification {

    // ─── Identity ─────────────────────────────────────────────────────────────
    private int    notificationId;
    private String notificationReference;   // e.g. NTF-000001

    // ─── Recipient ────────────────────────────────────────────────────────────
    private int    recipientUserId;
    private String recipientAccountId;

    // ─── Content ──────────────────────────────────────────────────────────────
    private NotificationType  type;
    private NotificationCategory category;
    private String            title;        // short heading (e.g. "Money Received")
    private String            message;      // full notification body
    private String            actionLabel;  // optional CTA text (e.g. "View Transaction")
    private String            actionTarget; // reference ID to navigate to (txn ref, invoice no., etc.)

    // ─── Read Status ──────────────────────────────────────────────────────────
    private boolean isRead;
    private LocalDateTime readAt;

    // ─── Priority ─────────────────────────────────────────────────────────────
    private NotificationPriority priority;

    // ─── Related Entity References ────────────────────────────────────────────
    private String relatedTransactionRef; // null if not transaction-related
    private String relatedInvoiceNumber;  // null if not invoice-related
    private String relatedLoanRef;        // null if not loan-related

    // ─── Timestamps ───────────────────────────────────────────────────────────
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;       // null = no expiry

    // ═════════════════════════════════════════════════════════════════════════
    // Enums
    // ═════════════════════════════════════════════════════════════════════════

    public enum NotificationType {
        // Transaction events
        MONEY_SENT,
        MONEY_RECEIVED,
        MONEY_REQUEST_RECEIVED,
        MONEY_REQUEST_ACCEPTED,
        MONEY_REQUEST_DECLINED,
        MONEY_REQUEST_CANCELLED,
        MONEY_REQUEST_EXPIRED,

        // Wallet events
        WALLET_TOP_UP,
        WALLET_WITHDRAWAL,
        LOW_BALANCE_ALERT,

        // Payment method events
        CARD_ADDED,
        CARD_REMOVED,
        CARD_SET_AS_DEFAULT,

        // Invoice events (business)
        INVOICE_RECEIVED,
        INVOICE_PAID,
        INVOICE_OVERDUE,
        INVOICE_REMINDER,
        INVOICE_CANCELLED,

        // Loan events (business)
        LOAN_APPLICATION_SUBMITTED,
        LOAN_APPROVED,
        LOAN_REJECTED,
        LOAN_DISBURSED,
        LOAN_REPAYMENT_DUE,
        LOAN_REPAYMENT_RECORDED,
        LOAN_CLOSED,
        LOAN_DEFAULTED,

        // Security / account events
        LOGIN_SUCCESS,
        LOGIN_FAILED,
        ACCOUNT_LOCKED,
        ACCOUNT_UNLOCKED,
        PASSWORD_CHANGED,
        PIN_CHANGED,
        TWO_FACTOR_CODE_SENT,

        // System
        SYSTEM_ANNOUNCEMENT,
        PROMOTIONAL
    }

    public enum NotificationCategory {
        TRANSACTION,
        MONEY_REQUEST,
        WALLET,
        PAYMENT_METHOD,
        INVOICE,
        LOAN,
        SECURITY,
        SYSTEM
    }

    public enum NotificationPriority {
        LOW,
        NORMAL,
        HIGH,
        URGENT
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Constructors
    // ═════════════════════════════════════════════════════════════════════════

    public Notification() {
        this.isRead    = false;
        this.priority  = NotificationPriority.NORMAL;
        this.createdAt = LocalDateTime.now();
    }

    public Notification(String notificationReference, int recipientUserId, String recipientAccountId,
                        NotificationType type, NotificationCategory category,
                        String title, String message) {
        this();
        this.notificationReference = notificationReference;
        this.recipientUserId       = recipientUserId;
        this.recipientAccountId    = recipientAccountId;
        this.type                  = type;
        this.category              = category;
        this.title                 = title;
        this.message               = message;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Static Factory Methods
    // ═════════════════════════════════════════════════════════════════════════

    public static Notification moneyReceived(String ref, int userId, String accountId,
                                             String senderName, double amount, String txnRef) {
        Notification n = new Notification(ref, userId, accountId,
                NotificationType.MONEY_RECEIVED,
                NotificationCategory.TRANSACTION,
                "Money Received",
                String.format("You received $%.2f from %s.", amount, senderName));
        n.setRelatedTransactionRef(txnRef);
        n.setPriority(NotificationPriority.HIGH);
        return n;
    }

    public static Notification moneySent(String ref, int userId, String accountId,
                                         String receiverName, double amount, String txnRef) {
        Notification n = new Notification(ref, userId, accountId,
                NotificationType.MONEY_SENT,
                NotificationCategory.TRANSACTION,
                "Money Sent",
                String.format("You sent $%.2f to %s.", amount, receiverName));
        n.setRelatedTransactionRef(txnRef);
        return n;
    }

    public static Notification moneyRequestReceived(String ref, int userId, String accountId,
                                                    String requesterName, double amount, String txnRef) {
        Notification n = new Notification(ref, userId, accountId,
                NotificationType.MONEY_REQUEST_RECEIVED,
                NotificationCategory.MONEY_REQUEST,
                "Money Request",
                String.format("%s is requesting $%.2f from you.", requesterName, amount));
        n.setRelatedTransactionRef(txnRef);
        n.setActionLabel("View Request");
        n.setActionTarget(txnRef);
        n.setPriority(NotificationPriority.HIGH);
        return n;
    }

    public static Notification lowBalanceAlert(String ref, int userId, String accountId, double balance) {
        Notification n = new Notification(ref, userId, accountId,
                NotificationType.LOW_BALANCE_ALERT,
                NotificationCategory.WALLET,
                "Low Balance Alert",
                String.format("Your wallet balance is low: $%.2f. Top up to continue transacting.", balance));
        n.setPriority(NotificationPriority.HIGH);
        return n;
    }

    public static Notification cardAdded(String ref, int userId, String accountId, String maskedCard) {
        return new Notification(ref, userId, accountId,
                NotificationType.CARD_ADDED,
                NotificationCategory.PAYMENT_METHOD,
                "Card Added",
                "A new card ending in " + maskedCard + " has been added to your account.");
    }

    public static Notification cardRemoved(String ref, int userId, String accountId, String maskedCard) {
        Notification n = new Notification(ref, userId, accountId,
                NotificationType.CARD_REMOVED,
                NotificationCategory.PAYMENT_METHOD,
                "Card Removed",
                "The card ending in " + maskedCard + " has been removed from your account.");
        n.setPriority(NotificationPriority.HIGH);
        return n;
    }

    public static Notification invoicePaid(String ref, int userId, String accountId,
                                           String invoiceNumber, double amount) {
        Notification n = new Notification(ref, userId, accountId,
                NotificationType.INVOICE_PAID,
                NotificationCategory.INVOICE,
                "Invoice Paid",
                String.format("Invoice %s has been paid. Amount received: $%.2f.", invoiceNumber, amount));
        n.setRelatedInvoiceNumber(invoiceNumber);
        n.setPriority(NotificationPriority.HIGH);
        return n;
    }

    public static Notification invoiceOverdue(String ref, int userId, String accountId, String invoiceNumber) {
        Notification n = new Notification(ref, userId, accountId,
                NotificationType.INVOICE_OVERDUE,
                NotificationCategory.INVOICE,
                "Invoice Overdue",
                "Invoice " + invoiceNumber + " is past due. Please follow up with your customer.");
        n.setRelatedInvoiceNumber(invoiceNumber);
        n.setPriority(NotificationPriority.URGENT);
        return n;
    }

    public static Notification loanApproved(String ref, int userId, String accountId,
                                            String loanRef, double approvedAmount) {
        Notification n = new Notification(ref, userId, accountId,
                NotificationType.LOAN_APPROVED,
                NotificationCategory.LOAN,
                "Loan Approved",
                String.format("Your loan application %s has been approved for $%.2f.", loanRef, approvedAmount));
        n.setRelatedLoanRef(loanRef);
        n.setPriority(NotificationPriority.HIGH);
        return n;
    }

    public static Notification loanRejected(String ref, int userId, String accountId, String loanRef) {
        Notification n = new Notification(ref, userId, accountId,
                NotificationType.LOAN_REJECTED,
                NotificationCategory.LOAN,
                "Loan Application Rejected",
                "Unfortunately, your loan application " + loanRef + " was not approved at this time.");
        n.setRelatedLoanRef(loanRef);
        n.setPriority(NotificationPriority.HIGH);
        return n;
    }

    public static Notification accountLocked(String ref, int userId, String accountId) {
        Notification n = new Notification(ref, userId, accountId,
                NotificationType.ACCOUNT_LOCKED,
                NotificationCategory.SECURITY,
                "Account Locked",
                "Your account has been locked due to multiple failed login attempts. Please reset your password.");
        n.setPriority(NotificationPriority.URGENT);
        return n;
    }

    public static Notification passwordChanged(String ref, int userId, String accountId) {
        Notification n = new Notification(ref, userId, accountId,
                NotificationType.PASSWORD_CHANGED,
                NotificationCategory.SECURITY,
                "Password Changed",
                "Your account password was recently changed. If this wasn't you, contact support immediately.");
        n.setPriority(NotificationPriority.URGENT);
        return n;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Business Logic
    // ═════════════════════════════════════════════════════════════════════════

    public void markAsRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Getters & Setters
    // ═════════════════════════════════════════════════════════════════════════

    public int getNotificationId() { return notificationId; }
    public void setNotificationId(int notificationId) { this.notificationId = notificationId; }

    public String getNotificationReference() { return notificationReference; }
    public void setNotificationReference(String notificationReference) { this.notificationReference = notificationReference; }

    public int getRecipientUserId() { return recipientUserId; }
    public void setRecipientUserId(int recipientUserId) { this.recipientUserId = recipientUserId; }

    public String getRecipientAccountId() { return recipientAccountId; }
    public void setRecipientAccountId(String recipientAccountId) { this.recipientAccountId = recipientAccountId; }

    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }

    public NotificationCategory getCategory() { return category; }
    public void setCategory(NotificationCategory category) { this.category = category; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getActionLabel() { return actionLabel; }
    public void setActionLabel(String actionLabel) { this.actionLabel = actionLabel; }

    public String getActionTarget() { return actionTarget; }
    public void setActionTarget(String actionTarget) { this.actionTarget = actionTarget; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean isRead) { this.isRead = isRead; }

    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }

    public NotificationPriority getPriority() { return priority; }
    public void setPriority(NotificationPriority priority) { this.priority = priority; }

    public String getRelatedTransactionRef() { return relatedTransactionRef; }
    public void setRelatedTransactionRef(String relatedTransactionRef) { this.relatedTransactionRef = relatedTransactionRef; }

    public String getRelatedInvoiceNumber() { return relatedInvoiceNumber; }
    public void setRelatedInvoiceNumber(String relatedInvoiceNumber) { this.relatedInvoiceNumber = relatedInvoiceNumber; }

    public String getRelatedLoanRef() { return relatedLoanRef; }
    public void setRelatedLoanRef(String relatedLoanRef) { this.relatedLoanRef = relatedLoanRef; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    @Override
    public String toString() {
        return "Notification{" +
                "reference='" + notificationReference + '\'' +
                ", type=" + type +
                ", title='" + title + '\'' +
                ", priority=" + priority +
                ", isRead=" + isRead +
                ", createdAt=" + createdAt +
                '}';
    }
}
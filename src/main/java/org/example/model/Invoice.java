package org.example.model;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a business invoice issued by a BusinessUser to a customer.
 * Supports itemized line items, payment terms, and full lifecycle tracking.
 */
public class Invoice {

    // ─── Identity ─────────────────────────────────────────────────────────────
    private int    invoiceId;
    private String invoiceNumber;          // human-readable (e.g. INV-2024-000001)

    // ─── Issuer (BusinessUser) ────────────────────────────────────────────────
    private int    issuerUserId;
    private String issuerAccountId;
    private String issuerBusinessName;
    private String issuerEmail;
    private String issuerPhone;
    private String issuerAddress;

    // ─── Customer ─────────────────────────────────────────────────────────────
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String customerAddress;
    private String customerAccountId;     // null if customer is not a RevPay user

    // ─── Line Items ───────────────────────────────────────────────────────────
    private List<InvoiceItem> items = new ArrayList<>();

    // ─── Financials ───────────────────────────────────────────────────────────
    private double subtotal;             // sum of line item totals (pre-tax/discount)
    private double discountAmount;       // flat discount applied
    private double discountPercent;      // percentage discount (applied to subtotal)
    private double taxRate;              // e.g. 0.08 for 8%
    private double taxAmount;            // computed: (subtotal - discount) * taxRate
    private double totalAmount;          // subtotal - discount + tax
    private double amountPaid;           // cumulative amount received
    private double amountDue;            // totalAmount - amountPaid
    private String currency;            // ISO-4217

    // ─── Terms & Notes ────────────────────────────────────────────────────────
    private int    paymentTermsDays;     // e.g. 30 for "Net 30"
    private String paymentInstructions;  // free-text instructions for the customer
    private String notes;               // internal or customer-facing notes
    private String termsAndConditions;

    // ─── Status & Lifecycle ───────────────────────────────────────────────────
    private InvoiceStatus status;
    private String linkedTransactionRef; // TXN reference when paid via RevPay

    // ─── Timestamps ───────────────────────────────────────────────────────────
    private LocalDateTime issuedAt;
    private LocalDateTime dueAt;
    private LocalDateTime paidAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime updatedAt;

    // ─── Notification Tracking ────────────────────────────────────────────────
    private boolean notificationSentToCustomer;
    private LocalDateTime notificationSentAt;
    private int reminderCount;
    private LocalDateTime lastReminderSentAt;

    // ═════════════════════════════════════════════════════════════════════════
    // Nested Class: InvoiceItem
    // ═════════════════════════════════════════════════════════════════════════

    public static class InvoiceItem {

        private int    itemId;
        private String description;
        private int    quantity;
        private double unitPrice;
        private double totalPrice;   // quantity * unitPrice
        private String unit;         // e.g. "hr", "pc", "kg" (optional)

        public InvoiceItem() {}

        public InvoiceItem(String description, int quantity, double unitPrice) {
            this.description = description;
            this.quantity    = quantity;
            this.unitPrice   = unitPrice;
            this.totalPrice  = quantity * unitPrice;
        }

        public InvoiceItem(String description, int quantity, double unitPrice, String unit) {
            this(description, quantity, unitPrice);
            this.unit = unit;
        }

        public void recalculate() {
            this.totalPrice = this.quantity * this.unitPrice;
        }

        public int    getItemId()      { return itemId; }
        public void   setItemId(int id){ this.itemId = id; }

        public String getDescription() { return description; }
        public void   setDescription(String description) { this.description = description; }

        public int    getQuantity()    { return quantity; }
        public void   setQuantity(int quantity) { this.quantity = quantity; recalculate(); }

        public double getUnitPrice()   { return unitPrice; }
        public void   setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; recalculate(); }

        public double getTotalPrice()  { return totalPrice; }
        public void   setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }

        public String getUnit()        { return unit; }
        public void   setUnit(String unit) { this.unit = unit; }

        @Override
        public String toString() {
            return "InvoiceItem{description='" + description + "', qty=" + quantity +
                    ", unitPrice=" + unitPrice + ", total=" + totalPrice + "}";
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Enum
    // ═════════════════════════════════════════════════════════════════════════

    public enum InvoiceStatus {
        DRAFT,       // created but not yet sent to the customer
        SENT,        // sent/shared with the customer
        UNPAID,      // due but not yet paid
        PARTIALLY_PAID, // partial payment received
        PAID,        // fully settled
        OVERDUE,     // past due date without full payment
        CANCELLED,   // voided by issuer
        REFUNDED     // payment reversed
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Constructors
    // ═════════════════════════════════════════════════════════════════════════

    public Invoice() {
        this.currency                  = "USD";
        this.status                    = InvoiceStatus.DRAFT;
        this.taxRate                   = 0.0;
        this.discountAmount            = 0.0;
        this.discountPercent           = 0.0;
        this.amountPaid                = 0.0;
        this.paymentTermsDays          = 30;
        this.notificationSentToCustomer = false;
        this.reminderCount             = 0;
        this.issuedAt                  = LocalDateTime.now();
        this.updatedAt                 = LocalDateTime.now();
    }

    public Invoice(String invoiceNumber, int issuerUserId, String issuerAccountId,
                   String issuerBusinessName, String customerName, String customerEmail,
                   int paymentTermsDays) {
        this();
        this.invoiceNumber     = invoiceNumber;
        this.issuerUserId      = issuerUserId;
        this.issuerAccountId   = issuerAccountId;
        this.issuerBusinessName = issuerBusinessName;
        this.customerName      = customerName;
        this.customerEmail     = customerEmail;
        this.paymentTermsDays  = paymentTermsDays;
        this.dueAt             = this.issuedAt.plusDays(paymentTermsDays);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Business Logic
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Adds a line item and recomputes totals.
     */
    public void addItem(InvoiceItem item) {
        items.add(item);
        recalculateTotals();
    }

    /**
     * Removes a line item and recomputes totals.
     */
    public boolean removeItem(int itemId) {
        boolean removed = items.removeIf(i -> i.getItemId() == itemId);
        if (removed) recalculateTotals();
        return removed;
    }

    /**
     * Recomputes subtotal, tax, discount, total, and amount due from line items.
     */
    public void recalculateTotals() {
        this.subtotal = items.stream().mapToDouble(InvoiceItem::getTotalPrice).sum();

        double afterDiscount = this.subtotal;
        if (this.discountPercent > 0) {
            this.discountAmount = this.subtotal * (this.discountPercent / 100.0);
        }
        afterDiscount -= this.discountAmount;

        this.taxAmount   = afterDiscount * this.taxRate;
        this.totalAmount = afterDiscount + this.taxAmount;
        this.amountDue   = this.totalAmount - this.amountPaid;
        this.updatedAt   = LocalDateTime.now();
    }

    /**
     * Records a payment against this invoice.
     */
    public void recordPayment(double paymentAmount, String transactionRef) {
        this.amountPaid         += paymentAmount;
        this.amountDue           = this.totalAmount - this.amountPaid;
        this.linkedTransactionRef = transactionRef;

        if (this.amountDue <= 0.001) {
            this.status = InvoiceStatus.PAID;
            this.paidAt = LocalDateTime.now();
        } else if (this.amountPaid > 0) {
            this.status = InvoiceStatus.PARTIALLY_PAID;
        }
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isOverdue() {
        return dueAt != null
                && LocalDateTime.now().isAfter(dueAt)
                && (status == InvoiceStatus.UNPAID
                || status == InvoiceStatus.PARTIALLY_PAID
                || status == InvoiceStatus.SENT);
    }

    public void checkAndMarkOverdue() {
        if (isOverdue()) {
            this.status    = InvoiceStatus.OVERDUE;
            this.updatedAt = LocalDateTime.now();
        }
    }

    public void markSent() {
        if (this.status == InvoiceStatus.DRAFT) {
            this.status                    = InvoiceStatus.SENT;
            this.notificationSentToCustomer = true;
            this.notificationSentAt        = LocalDateTime.now();
            this.updatedAt                 = LocalDateTime.now();
        }
    }

    public void cancel() {
        this.status        = InvoiceStatus.CANCELLED;
        this.cancelledAt   = LocalDateTime.now();
        this.updatedAt     = LocalDateTime.now();
    }

    public void recordReminderSent() {
        this.reminderCount++;
        this.lastReminderSentAt = LocalDateTime.now();
        this.updatedAt          = LocalDateTime.now();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Getters & Setters
    // ═════════════════════════════════════════════════════════════════════════

    public int getInvoiceId() { return invoiceId; }
    public void setInvoiceId(int invoiceId) { this.invoiceId = invoiceId; }

    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }

    public int getIssuerUserId() { return issuerUserId; }
    public void setIssuerUserId(int issuerUserId) { this.issuerUserId = issuerUserId; }

    public String getIssuerAccountId() { return issuerAccountId; }
    public void setIssuerAccountId(String issuerAccountId) { this.issuerAccountId = issuerAccountId; }

    public String getIssuerBusinessName() { return issuerBusinessName; }
    public void setIssuerBusinessName(String issuerBusinessName) { this.issuerBusinessName = issuerBusinessName; }

    public String getIssuerEmail() { return issuerEmail; }
    public void setIssuerEmail(String issuerEmail) { this.issuerEmail = issuerEmail; }

    public String getIssuerPhone() { return issuerPhone; }
    public void setIssuerPhone(String issuerPhone) { this.issuerPhone = issuerPhone; }

    public String getIssuerAddress() { return issuerAddress; }
    public void setIssuerAddress(String issuerAddress) { this.issuerAddress = issuerAddress; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public String getCustomerAddress() { return customerAddress; }
    public void setCustomerAddress(String customerAddress) { this.customerAddress = customerAddress; }

    public String getCustomerAccountId() { return customerAccountId; }
    public void setCustomerAccountId(String customerAccountId) { this.customerAccountId = customerAccountId; }

    public List<InvoiceItem> getItems() { return items; }
    public void setItems(List<InvoiceItem> items) { this.items = items; recalculateTotals(); }

    public double getSubtotal() { return subtotal; }
    public double getDiscountAmount() { return discountAmount; }

    public void setDiscountAmount(double discountAmount) {
        this.discountAmount  = discountAmount;
        this.discountPercent = 0.0;
        recalculateTotals();
    }

    public double getDiscountPercent() { return discountPercent; }

    public void setDiscountPercent(double discountPercent) {
        this.discountPercent = discountPercent;
        this.discountAmount  = 0.0;
        recalculateTotals();
    }

    public double getTaxRate() { return taxRate; }
    public void setTaxRate(double taxRate) { this.taxRate = taxRate; recalculateTotals(); }

    public double getTaxAmount() { return taxAmount; }
    public double getTotalAmount() { return totalAmount; }

    public double getAmountPaid() { return amountPaid; }
    public void setAmountPaid(double amountPaid) { this.amountPaid = amountPaid; this.amountDue = totalAmount - amountPaid; }

    public double getAmountDue() { return amountDue; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public int getPaymentTermsDays() { return paymentTermsDays; }
    public void setPaymentTermsDays(int paymentTermsDays) { this.paymentTermsDays = paymentTermsDays; this.dueAt = issuedAt.plusDays(paymentTermsDays); }

    public String getPaymentInstructions() { return paymentInstructions; }
    public void setPaymentInstructions(String paymentInstructions) { this.paymentInstructions = paymentInstructions; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getTermsAndConditions() { return termsAndConditions; }
    public void setTermsAndConditions(String termsAndConditions) { this.termsAndConditions = termsAndConditions; }

    public InvoiceStatus getStatus() { return status; }
    public void setStatus(InvoiceStatus status) { this.status = status; this.updatedAt = LocalDateTime.now(); }

    public String getLinkedTransactionRef() { return linkedTransactionRef; }
    public void setLinkedTransactionRef(String linkedTransactionRef) { this.linkedTransactionRef = linkedTransactionRef; }

    public LocalDateTime getIssuedAt() { return issuedAt; }
    public void setIssuedAt(LocalDateTime issuedAt) { this.issuedAt = issuedAt; }

    public LocalDateTime getDueAt() { return dueAt; }
    public void setDueAt(LocalDateTime dueAt) { this.dueAt = dueAt; }

    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }

    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public boolean isNotificationSentToCustomer() { return notificationSentToCustomer; }
    public void setNotificationSentToCustomer(boolean notificationSentToCustomer) { this.notificationSentToCustomer = notificationSentToCustomer; }

    public LocalDateTime getNotificationSentAt() { return notificationSentAt; }
    public void setNotificationSentAt(LocalDateTime notificationSentAt) { this.notificationSentAt = notificationSentAt; }

    public int getReminderCount() { return reminderCount; }
    public void setReminderCount(int reminderCount) { this.reminderCount = reminderCount; }

    public LocalDateTime getLastReminderSentAt() { return lastReminderSentAt; }
    public void setLastReminderSentAt(LocalDateTime lastReminderSentAt) { this.lastReminderSentAt = lastReminderSentAt; }

    @Override
    public String toString() {
        return "Invoice{" +
                "invoiceNumber='" + invoiceNumber + '\'' +
                ", customer='" + customerName + '\'' +
                ", total=" + totalAmount +
                ", amountDue=" + amountDue +
                ", status=" + status +
                ", dueAt=" + dueAt +
                '}';
    }
}
package org.example.model;
import java.time.LocalDateTime;

/**
 * Represents a saved payment method (credit/debit card or bank account)
 * associated with a RevPay user.
 * Card numbers are stored AES-256 encrypted; only the last 4 digits are
 * stored in plain text for display purposes.
 */
public class PaymentMethod {

    // ─── Identity ─────────────────────────────────────────────────────────────
    private int    paymentMethodId;
    private String paymentMethodReference;  // unique reference (e.g. PM-000001)

    // ─── Owner ────────────────────────────────────────────────────────────────
    private int    userId;
    private String accountId;              // denormalized for quick lookup

    // ─── Type ─────────────────────────────────────────────────────────────────
    private PaymentMethodType type;

    // ─── Card Fields (populated when type is CREDIT_CARD or DEBIT_CARD) ───────
    private String encryptedCardNumber;    // AES-256 encrypted full PAN
    private String maskedCardNumber;       // e.g. "**** **** **** 4242"  (plain, for display)
    private String cardHolderName;
    private String expiryMonth;            // "MM"
    private String expiryYear;            // "YYYY"
    private String encryptedCvv;           // AES-256 encrypted; ideally not stored long-term
    private CardNetwork cardNetwork;       // VISA, MASTERCARD, etc.
    private CardCategory cardCategory;    // PERSONAL, BUSINESS

    // ─── Bank Account Fields (populated when type is BANK_ACCOUNT) ────────────
    private String bankName;
    private String encryptedAccountNumber; // AES-256 encrypted
    private String maskedAccountNumber;    // e.g. "****5678"
    private String routingNumber;          // ABA routing (can be stored plain)
    private String accountHolderName;
    private BankAccountType bankAccountType; // CHECKING, SAVINGS

    // ─── Status & Preferences ─────────────────────────────────────────────────
    private boolean isDefault;             // user's preferred payment method
    private boolean isActive;             // soft-delete flag
    private String  nickname;             // user-given label (e.g. "My Chase Card")

    // ─── Billing Address ──────────────────────────────────────────────────────
    private String billingAddressLine1;
    private String billingAddressLine2;
    private String billingCity;
    private String billingState;
    private String billingPostalCode;
    private String billingCountry;

    // ─── Timestamps ───────────────────────────────────────────────────────────
    private LocalDateTime addedAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastUsedAt;

    // ═════════════════════════════════════════════════════════════════════════
    // Enums
    // ═════════════════════════════════════════════════════════════════════════

    public enum PaymentMethodType {
        CREDIT_CARD,
        DEBIT_CARD,
        BANK_ACCOUNT
    }

    public enum CardNetwork {
        VISA,
        MASTERCARD,
        AMERICAN_EXPRESS,
        DISCOVER,
        OTHER
    }

    public enum CardCategory {
        PERSONAL,
        BUSINESS
    }

    public enum BankAccountType {
        CHECKING,
        SAVINGS
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Constructors
    // ═════════════════════════════════════════════════════════════════════════

    public PaymentMethod() {
        this.isDefault = false;
        this.isActive  = true;
        this.addedAt   = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // ─── Card factory ─────────────────────────────────────────────────────────
    public static PaymentMethod createCard(int userId, String accountId,
                                           PaymentMethodType type,
                                           String encryptedCardNumber,
                                           String maskedCardNumber,
                                           String cardHolderName,
                                           String expiryMonth, String expiryYear,
                                           String encryptedCvv,
                                           CardNetwork cardNetwork) {
        PaymentMethod pm = new PaymentMethod();
        pm.userId              = userId;
        pm.accountId           = accountId;
        pm.type                = type;
        pm.encryptedCardNumber = encryptedCardNumber;
        pm.maskedCardNumber    = maskedCardNumber;
        pm.cardHolderName      = cardHolderName;
        pm.expiryMonth         = expiryMonth;
        pm.expiryYear          = expiryYear;
        pm.encryptedCvv        = encryptedCvv;
        pm.cardNetwork         = cardNetwork;
        pm.cardCategory        = (type == PaymentMethodType.CREDIT_CARD || type == PaymentMethodType.DEBIT_CARD)
                ? CardCategory.PERSONAL : null;
        return pm;
    }

    // ─── Bank account factory ─────────────────────────────────────────────────
    public static PaymentMethod createBankAccount(int userId, String accountId,
                                                  String bankName,
                                                  String encryptedAccountNumber,
                                                  String maskedAccountNumber,
                                                  String routingNumber,
                                                  String accountHolderName,
                                                  BankAccountType bankAccountType) {
        PaymentMethod pm = new PaymentMethod();
        pm.userId                 = userId;
        pm.accountId              = accountId;
        pm.type                   = PaymentMethodType.BANK_ACCOUNT;
        pm.bankName               = bankName;
        pm.encryptedAccountNumber = encryptedAccountNumber;
        pm.maskedAccountNumber    = maskedAccountNumber;
        pm.routingNumber          = routingNumber;
        pm.accountHolderName      = accountHolderName;
        pm.bankAccountType        = bankAccountType;
        return pm;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Business Logic Helpers
    // ═════════════════════════════════════════════════════════════════════════

    public boolean isCard() {
        return type == PaymentMethodType.CREDIT_CARD || type == PaymentMethodType.DEBIT_CARD;
    }

    public boolean isBankAccount() {
        return type == PaymentMethodType.BANK_ACCOUNT;
    }

    /**
     * Returns a short display label combining nickname (if set) and masked number.
     */
    public String getDisplayLabel() {
        if (nickname != null && !nickname.isBlank()) {
            return nickname;
        }
        if (isCard()) {
            return (cardNetwork != null ? cardNetwork.name() : "Card") + " " + maskedCardNumber;
        }
        return (bankName != null ? bankName : "Bank") + " " + maskedAccountNumber;
    }

    public boolean isExpired() {
        if (!isCard() || expiryMonth == null || expiryYear == null) return false;
        try {
            int month = Integer.parseInt(expiryMonth);
            int year  = Integer.parseInt(expiryYear);
            LocalDateTime expiry = LocalDateTime.of(year, month, 1, 23, 59, 59)
                    .withDayOfMonth(
                            java.time.YearMonth.of(year, month).lengthOfMonth());
            return LocalDateTime.now().isAfter(expiry);
        } catch (Exception e) {
            return false;
        }
    }

    public void recordUsage() {
        this.lastUsedAt = LocalDateTime.now();
        this.updatedAt  = LocalDateTime.now();
    }

    public void deactivate() {
        this.isActive  = false;
        this.isDefault = false;
        this.updatedAt = LocalDateTime.now();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Getters & Setters
    // ═════════════════════════════════════════════════════════════════════════

    public int getPaymentMethodId() { return paymentMethodId; }
    public void setPaymentMethodId(int paymentMethodId) { this.paymentMethodId = paymentMethodId; }

    public String getPaymentMethodReference() { return paymentMethodReference; }
    public void setPaymentMethodReference(String paymentMethodReference) { this.paymentMethodReference = paymentMethodReference; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }

    public PaymentMethodType getType() { return type; }
    public void setType(PaymentMethodType type) { this.type = type; }

    public String getEncryptedCardNumber() { return encryptedCardNumber; }
    public void setEncryptedCardNumber(String encryptedCardNumber) { this.encryptedCardNumber = encryptedCardNumber; }

    public String getMaskedCardNumber() { return maskedCardNumber; }
    public void setMaskedCardNumber(String maskedCardNumber) { this.maskedCardNumber = maskedCardNumber; }

    public String getCardHolderName() { return cardHolderName; }
    public void setCardHolderName(String cardHolderName) { this.cardHolderName = cardHolderName; }

    public String getExpiryMonth() { return expiryMonth; }
    public void setExpiryMonth(String expiryMonth) { this.expiryMonth = expiryMonth; }

    public String getExpiryYear() { return expiryYear; }
    public void setExpiryYear(String expiryYear) { this.expiryYear = expiryYear; }

    public String getEncryptedCvv() { return encryptedCvv; }
    public void setEncryptedCvv(String encryptedCvv) { this.encryptedCvv = encryptedCvv; }

    public CardNetwork getCardNetwork() { return cardNetwork; }
    public void setCardNetwork(CardNetwork cardNetwork) { this.cardNetwork = cardNetwork; }

    public CardCategory getCardCategory() { return cardCategory; }
    public void setCardCategory(CardCategory cardCategory) { this.cardCategory = cardCategory; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getEncryptedAccountNumber() { return encryptedAccountNumber; }
    public void setEncryptedAccountNumber(String encryptedAccountNumber) { this.encryptedAccountNumber = encryptedAccountNumber; }

    public String getMaskedAccountNumber() { return maskedAccountNumber; }
    public void setMaskedAccountNumber(String maskedAccountNumber) { this.maskedAccountNumber = maskedAccountNumber; }

    public String getRoutingNumber() { return routingNumber; }
    public void setRoutingNumber(String routingNumber) { this.routingNumber = routingNumber; }

    public String getAccountHolderName() { return accountHolderName; }
    public void setAccountHolderName(String accountHolderName) { this.accountHolderName = accountHolderName; }

    public BankAccountType getBankAccountType() { return bankAccountType; }
    public void setBankAccountType(BankAccountType bankAccountType) { this.bankAccountType = bankAccountType; }

    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean isDefault) { this.isDefault = isDefault; this.updatedAt = LocalDateTime.now(); }

    public boolean isActive() { return isActive; }
    public void setActive(boolean isActive) { this.isActive = isActive; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getBillingAddressLine1() { return billingAddressLine1; }
    public void setBillingAddressLine1(String billingAddressLine1) { this.billingAddressLine1 = billingAddressLine1; }

    public String getBillingAddressLine2() { return billingAddressLine2; }
    public void setBillingAddressLine2(String billingAddressLine2) { this.billingAddressLine2 = billingAddressLine2; }

    public String getBillingCity() { return billingCity; }
    public void setBillingCity(String billingCity) { this.billingCity = billingCity; }

    public String getBillingState() { return billingState; }
    public void setBillingState(String billingState) { this.billingState = billingState; }

    public String getBillingPostalCode() { return billingPostalCode; }
    public void setBillingPostalCode(String billingPostalCode) { this.billingPostalCode = billingPostalCode; }

    public String getBillingCountry() { return billingCountry; }
    public void setBillingCountry(String billingCountry) { this.billingCountry = billingCountry; }

    public LocalDateTime getAddedAt() { return addedAt; }
    public void setAddedAt(LocalDateTime addedAt) { this.addedAt = addedAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(LocalDateTime lastUsedAt) { this.lastUsedAt = lastUsedAt; }

    @Override
    public String toString() {
        return "PaymentMethod{" +
                "reference='" + paymentMethodReference + '\'' +
                ", type=" + type +
                ", display='" + getDisplayLabel() + '\'' +
                ", isDefault=" + isDefault +
                ", isActive=" + isActive +
                '}';
    }
}
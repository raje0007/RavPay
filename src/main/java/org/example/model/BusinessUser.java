package org.example.model;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a business account user in RevPay.
 * Extends the base User class with business-specific capabilities:
 * loans, invoices, analytics, and business payment acceptance.
 */
public class BusinessUser extends User {

    // ─── Business Information ─────────────────────────────────────────────────
    private String businessName;
    private BusinessType businessType;
    private String taxId;                      // encrypted at storage layer
    private String businessAddress;
    private String businessEmail;
    private String businessPhoneNumber;
    private String websiteUrl;
    private String businessDescription;

    // ─── Verification ─────────────────────────────────────────────────────────
    private VerificationStatus verificationStatus;
    private String verificationDocumentPath;   // path/reference to uploaded doc
    private LocalDateTime verifiedAt;
    private String verifiedBy;                 // admin ID who verified

    // ─── Owner / Representative ───────────────────────────────────────────────
    private String ownerFirstName;
    private String ownerLastName;
    private String ownerDateOfBirth;           // "YYYY-MM-DD"

    // ─── Business Financials ──────────────────────────────────────────────────
    private double monthlyRevenue;             // self-reported, used for loan assessment
    private double annualRevenue;
    private int    yearsInBusiness;

    // ─── Relationships ────────────────────────────────────────────────────────
    private List<Invoice>      invoices        = new ArrayList<>();
    private List<Loan>         loans           = new ArrayList<>();
    private List<Transaction>  receivedPayments = new ArrayList<>();  // accepted payments from customers

    // ─── Analytics Cache (computed values refreshed periodically) ─────────────
    private double totalRevenue;
    private double outstandingInvoicesTotal;
    private int    totalInvoicesIssued;
    private int    totalInvoicesPaid;
    private LocalDateTime analyticsLastRefreshed;

    // ═════════════════════════════════════════════════════════════════════════
    // Enums
    // ═════════════════════════════════════════════════════════════════════════

    public enum BusinessType {
        SOLE_PROPRIETORSHIP,
        PARTNERSHIP,
        LIMITED_LIABILITY_COMPANY,
        CORPORATION,
        NON_PROFIT,
        FREELANCER,
        OTHER
    }

    public enum VerificationStatus {
        PENDING,       // documents submitted, awaiting review
        VERIFIED,      // approved
        REJECTED,      // documents rejected
        UNVERIFIED     // no documents submitted yet
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Constructors
    // ═════════════════════════════════════════════════════════════════════════

    public BusinessUser() {
        super();
        this.verificationStatus = VerificationStatus.UNVERIFIED;
    }

    public BusinessUser(String businessName, BusinessType businessType,
                        String taxId, String businessAddress,
                        String ownerFirstName, String ownerLastName,
                        String username, String email, String phoneNumber,
                        String passwordHash, String transactionPin,
                        String securityQuestion1, String securityAnswer1,
                        String securityQuestion2, String securityAnswer2) {
        super();
        this.businessName        = businessName;
        this.businessType        = businessType;
        this.taxId               = taxId;
        this.businessAddress     = businessAddress;
        this.ownerFirstName      = ownerFirstName;
        this.ownerLastName       = ownerLastName;
        this.verificationStatus  = VerificationStatus.UNVERIFIED;
        setUsername(username);
        setEmail(email);
        setPhoneNumber(phoneNumber);
        setPasswordHash(passwordHash);
        setTransactionPin(transactionPin);
        setSecurityQuestion1(securityQuestion1);
        setSecurityAnswer1(securityAnswer1);
        setSecurityQuestion2(securityQuestion2);
        setSecurityAnswer2(securityAnswer2);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Abstract Method Implementations
    // ═════════════════════════════════════════════════════════════════════════

    @Override
    public AccountType getAccountType() {
        return AccountType.BUSINESS;
    }

    @Override
    public String getDisplayName() {
        return businessName;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Business Logic Helpers
    // ═════════════════════════════════════════════════════════════════════════

    public boolean isVerified() {
        return this.verificationStatus == VerificationStatus.VERIFIED;
    }

    public String getOwnerFullName() {
        return ownerFirstName + " " + ownerLastName;
    }

    public void addInvoice(Invoice invoice) {
        this.invoices.add(invoice);
        refreshAnalyticsCache();
    }

    public void addLoan(Loan loan) {
        this.loans.add(loan);
    }

    public void addReceivedPayment(Transaction transaction) {
        this.receivedPayments.add(transaction);
        refreshAnalyticsCache();
    }

    /**
     * Recomputes cached analytics values from live data.
     */
    public void refreshAnalyticsCache() {
        this.totalInvoicesIssued = invoices.size();

        this.totalInvoicesPaid = (int) invoices.stream()
                .filter(i -> i.getStatus() == Invoice.InvoiceStatus.PAID)
                .count();

        this.outstandingInvoicesTotal = invoices.stream()
                .filter(i -> i.getStatus() == Invoice.InvoiceStatus.UNPAID
                        || i.getStatus() == Invoice.InvoiceStatus.OVERDUE)
                .mapToDouble(Invoice::getTotalAmount)
                .sum();

        this.totalRevenue = receivedPayments.stream()
                .filter(t -> t.getStatus() == Transaction.TransactionStatus.COMPLETED)
                .mapToDouble(Transaction::getAmount)
                .sum();

        this.analyticsLastRefreshed = LocalDateTime.now();
    }

    public List<Invoice> getUnpaidInvoices() {
        List<Invoice> unpaid = new ArrayList<>();
        for (Invoice i : invoices) {
            if (i.getStatus() == Invoice.InvoiceStatus.UNPAID
                    || i.getStatus() == Invoice.InvoiceStatus.OVERDUE) {
                unpaid.add(i);
            }
        }
        return unpaid;
    }

    public List<Loan> getActiveLoans() {
        List<Loan> active = new ArrayList<>();
        for (Loan l : loans) {
            if (l.getStatus() == Loan.LoanStatus.ACTIVE) {
                active.add(l);
            }
        }
        return active;
    }

    public double getTotalOutstandingLoanBalance() {
        return loans.stream()
                .filter(l -> l.getStatus() == Loan.LoanStatus.ACTIVE)
                .mapToDouble(Loan::getRemainingBalance)
                .sum();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Getters & Setters
    // ═════════════════════════════════════════════════════════════════════════

    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }

    public BusinessType getBusinessType() { return businessType; }
    public void setBusinessType(BusinessType businessType) { this.businessType = businessType; }

    public String getTaxId() { return taxId; }
    public void setTaxId(String taxId) { this.taxId = taxId; }

    public String getBusinessAddress() { return businessAddress; }
    public void setBusinessAddress(String businessAddress) { this.businessAddress = businessAddress; }

    public String getBusinessEmail() { return businessEmail; }
    public void setBusinessEmail(String businessEmail) { this.businessEmail = businessEmail; }

    public String getBusinessPhoneNumber() { return businessPhoneNumber; }
    public void setBusinessPhoneNumber(String businessPhoneNumber) { this.businessPhoneNumber = businessPhoneNumber; }

    public String getWebsiteUrl() { return websiteUrl; }
    public void setWebsiteUrl(String websiteUrl) { this.websiteUrl = websiteUrl; }

    public String getBusinessDescription() { return businessDescription; }
    public void setBusinessDescription(String businessDescription) { this.businessDescription = businessDescription; }

    public VerificationStatus getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(VerificationStatus verificationStatus) { this.verificationStatus = verificationStatus; }

    public String getVerificationDocumentPath() { return verificationDocumentPath; }
    public void setVerificationDocumentPath(String verificationDocumentPath) { this.verificationDocumentPath = verificationDocumentPath; }

    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }

    public String getVerifiedBy() { return verifiedBy; }
    public void setVerifiedBy(String verifiedBy) { this.verifiedBy = verifiedBy; }

    public String getOwnerFirstName() { return ownerFirstName; }
    public void setOwnerFirstName(String ownerFirstName) { this.ownerFirstName = ownerFirstName; }

    public String getOwnerLastName() { return ownerLastName; }
    public void setOwnerLastName(String ownerLastName) { this.ownerLastName = ownerLastName; }

    public String getOwnerDateOfBirth() { return ownerDateOfBirth; }
    public void setOwnerDateOfBirth(String ownerDateOfBirth) { this.ownerDateOfBirth = ownerDateOfBirth; }

    public double getMonthlyRevenue() { return monthlyRevenue; }
    public void setMonthlyRevenue(double monthlyRevenue) { this.monthlyRevenue = monthlyRevenue; }

    public double getAnnualRevenue() { return annualRevenue; }
    public void setAnnualRevenue(double annualRevenue) { this.annualRevenue = annualRevenue; }

    public int getYearsInBusiness() { return yearsInBusiness; }
    public void setYearsInBusiness(int yearsInBusiness) { this.yearsInBusiness = yearsInBusiness; }

    public List<Invoice> getInvoices() { return invoices; }
    public void setInvoices(List<Invoice> invoices) { this.invoices = invoices; }

    public List<Loan> getLoans() { return loans; }
    public void setLoans(List<Loan> loans) { this.loans = loans; }

    public List<Transaction> getReceivedPayments() { return receivedPayments; }
    public void setReceivedPayments(List<Transaction> receivedPayments) { this.receivedPayments = receivedPayments; }

    public double getTotalRevenue() { return totalRevenue; }
    public double getOutstandingInvoicesTotal() { return outstandingInvoicesTotal; }
    public int getTotalInvoicesIssued() { return totalInvoicesIssued; }
    public int getTotalInvoicesPaid() { return totalInvoicesPaid; }
    public LocalDateTime getAnalyticsLastRefreshed() { return analyticsLastRefreshed; }

    @Override
    public String toString() {
        return "BusinessUser{" +
                "accountId='" + getAccountId() + '\'' +
                ", businessName='" + businessName + '\'' +
                ", businessType=" + businessType +
                ", verificationStatus=" + verificationStatus +
                ", status=" + getStatus() +
                '}';
    }
}
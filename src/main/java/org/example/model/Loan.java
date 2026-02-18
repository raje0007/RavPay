package org.example.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a business loan application and its lifecycle in RevPay.
 * Covers application, approval/rejection, disbursement, repayment schedule,
 * and closure.
 */
public class Loan {

    // ─── Identity ─────────────────────────────────────────────────────────────
    private int    loanId;
    private String loanReferenceNumber;    // e.g. LN-2024-000001

    // ─── Applicant ────────────────────────────────────────────────────────────
    private int    borrowerUserId;
    private String borrowerAccountId;
    private String borrowerBusinessName;

    // ─── Application Details ──────────────────────────────────────────────────
    private double  requestedAmount;
    private String  purpose;              // reason for the loan
    private String  purposeDetails;       // extended description
    private LoanPurposeCategory purposeCategory;
    private String  supportingDocumentPath; // uploaded document reference

    // ─── Financial Snapshot at Application ───────────────────────────────────
    private double  monthlyRevenueDeclared;
    private double  annualRevenueDeclared;
    private int     yearsInBusinessDeclared;
    private double  existingDebtAmount;   // other loans/liabilities declared

    // ─── Approved Terms ───────────────────────────────────────────────────────
    private double  approvedAmount;
    private double  interestRate;         // annual percentage rate (e.g. 0.12 for 12%)
    private int     termMonths;           // loan duration in months
    private double  monthlyRepaymentAmount;
    private LocalDateTime repaymentStartDate;

    // ─── Running Balances ─────────────────────────────────────────────────────
    private double  principalRemaining;
    private double  interestAccrued;
    private double  totalRepaid;
    private double  remainingBalance;     // principalRemaining + interestAccrued

    // ─── Repayment Schedule ───────────────────────────────────────────────────
    private List<RepaymentEntry> repaymentSchedule = new ArrayList<>();

    // ─── Status ───────────────────────────────────────────────────────────────
    private LoanStatus status;
    private String     rejectionReason;
    private String     reviewedBy;        // admin ID

    // ─── Timestamps ───────────────────────────────────────────────────────────
    private LocalDateTime appliedAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime disbursedAt;
    private LocalDateTime closedAt;
    private LocalDateTime updatedAt;
    private LocalDateTime nextRepaymentDueAt;

    // ─── Delinquency ──────────────────────────────────────────────────────────
    private int     missedPayments;
    private boolean isDelinquent;

    // ═════════════════════════════════════════════════════════════════════════
    // Nested Class: RepaymentEntry
    // ═════════════════════════════════════════════════════════════════════════

    public static class RepaymentEntry {
        private int           installmentNumber;
        private LocalDateTime dueDate;
        private double        scheduledAmount;
        private double        paidAmount;
        private LocalDateTime paidAt;
        private RepaymentStatus repaymentStatus;
        private String        transactionRef;

        public enum RepaymentStatus { PENDING, PAID, PARTIAL, MISSED, WAIVED }

        public RepaymentEntry() { this.repaymentStatus = RepaymentStatus.PENDING; }

        public RepaymentEntry(int installmentNumber, LocalDateTime dueDate, double scheduledAmount) {
            this();
            this.installmentNumber = installmentNumber;
            this.dueDate           = dueDate;
            this.scheduledAmount   = scheduledAmount;
        }

        public boolean isPaid() { return repaymentStatus == RepaymentStatus.PAID; }

        public boolean isOverdue() {
            return repaymentStatus == RepaymentStatus.PENDING
                    && LocalDateTime.now().isAfter(dueDate);
        }

        // Getters & Setters
        public int getInstallmentNumber() { return installmentNumber; }
        public void setInstallmentNumber(int n) { this.installmentNumber = n; }
        public LocalDateTime getDueDate() { return dueDate; }
        public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }
        public double getScheduledAmount() { return scheduledAmount; }
        public void setScheduledAmount(double scheduledAmount) { this.scheduledAmount = scheduledAmount; }
        public double getPaidAmount() { return paidAmount; }
        public void setPaidAmount(double paidAmount) { this.paidAmount = paidAmount; }
        public LocalDateTime getPaidAt() { return paidAt; }
        public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
        public RepaymentStatus getRepaymentStatus() { return repaymentStatus; }
        public void setRepaymentStatus(RepaymentStatus s) { this.repaymentStatus = s; }
        public String getTransactionRef() { return transactionRef; }
        public void setTransactionRef(String transactionRef) { this.transactionRef = transactionRef; }

        @Override
        public String toString() {
            return "Installment#" + installmentNumber + "{due=" + dueDate +
                    ", amount=" + scheduledAmount + ", status=" + repaymentStatus + "}";
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Enums
    // ═════════════════════════════════════════════════════════════════════════

    public enum LoanStatus {
        APPLIED,      // application submitted, awaiting review
        UNDER_REVIEW, // admin reviewing the application
        APPROVED,     // approved but not yet disbursed
        REJECTED,     // application denied
        ACTIVE,       // disbursed and repayment ongoing
        CLOSED,       // fully repaid
        DEFAULTED,    // severely delinquent, declared in default
        CANCELLED     // cancelled by applicant before disbursement
    }

    public enum LoanPurposeCategory {
        WORKING_CAPITAL,
        EQUIPMENT_PURCHASE,
        INVENTORY,
        EXPANSION,
        MARKETING,
        TECHNOLOGY,
        DEBT_CONSOLIDATION,
        REAL_ESTATE,
        OTHER
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Constructors
    // ═════════════════════════════════════════════════════════════════════════

    public Loan() {
        this.status         = LoanStatus.APPLIED;
        this.missedPayments = 0;
        this.isDelinquent   = false;
        this.totalRepaid    = 0.0;
        this.appliedAt      = LocalDateTime.now();
        this.updatedAt      = LocalDateTime.now();
    }

    public Loan(String loanReferenceNumber, int borrowerUserId, String borrowerAccountId,
                String borrowerBusinessName, double requestedAmount,
                String purpose, LoanPurposeCategory purposeCategory) {
        this();
        this.loanReferenceNumber  = loanReferenceNumber;
        this.borrowerUserId       = borrowerUserId;
        this.borrowerAccountId    = borrowerAccountId;
        this.borrowerBusinessName = borrowerBusinessName;
        this.requestedAmount      = requestedAmount;
        this.purpose              = purpose;
        this.purposeCategory      = purposeCategory;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Business Logic
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Approves the loan with specified terms and generates a repayment schedule.
     */
    public void approve(double approvedAmount, double interestRate,
                        int termMonths, LocalDateTime repaymentStartDate, String reviewedBy) {
        this.approvedAmount      = approvedAmount;
        this.interestRate        = interestRate;
        this.termMonths          = termMonths;
        this.repaymentStartDate  = repaymentStartDate;
        this.reviewedBy          = reviewedBy;
        this.status              = LoanStatus.APPROVED;
        this.reviewedAt          = LocalDateTime.now();
        this.updatedAt           = LocalDateTime.now();

        // Compute simple monthly repayment (flat interest for simplicity)
        double totalInterest           = approvedAmount * interestRate * (termMonths / 12.0);
        double totalPayable            = approvedAmount + totalInterest;
        this.monthlyRepaymentAmount    = totalPayable / termMonths;
        this.principalRemaining        = approvedAmount;
        this.remainingBalance          = totalPayable;

        // Generate repayment schedule
        this.repaymentSchedule.clear();
        for (int i = 1; i <= termMonths; i++) {
            LocalDateTime dueDate = repaymentStartDate.plusMonths(i - 1);
            repaymentSchedule.add(new RepaymentEntry(i, dueDate, monthlyRepaymentAmount));
        }
        this.nextRepaymentDueAt = repaymentStartDate;
    }

    /**
     * Rejects the loan application.
     */
    public void reject(String rejectionReason, String reviewedBy) {
        this.status           = LoanStatus.REJECTED;
        this.rejectionReason  = rejectionReason;
        this.reviewedBy       = reviewedBy;
        this.reviewedAt       = LocalDateTime.now();
        this.updatedAt        = LocalDateTime.now();
    }

    /**
     * Marks the loan as disbursed (funds credited to borrower's wallet).
     */
    public void disburse() {
        this.status      = LoanStatus.ACTIVE;
        this.disbursedAt = LocalDateTime.now();
        this.updatedAt   = LocalDateTime.now();
    }

    /**
     * Records a repayment against the next pending installment.
     */
    public void recordRepayment(double amount, String transactionRef) {
        for (RepaymentEntry entry : repaymentSchedule) {
            if (entry.getRepaymentStatus() == RepaymentEntry.RepaymentStatus.PENDING) {
                entry.setPaidAmount(amount);
                entry.setPaidAt(LocalDateTime.now());
                entry.setTransactionRef(transactionRef);

                if (amount >= entry.getScheduledAmount()) {
                    entry.setRepaymentStatus(RepaymentEntry.RepaymentStatus.PAID);
                } else {
                    entry.setRepaymentStatus(RepaymentEntry.RepaymentStatus.PARTIAL);
                }

                this.totalRepaid      += amount;
                this.remainingBalance -= amount;
                this.principalRemaining = Math.max(0, this.principalRemaining - amount);
                this.updatedAt         = LocalDateTime.now();

                // Advance next repayment date
                updateNextRepaymentDue();

                // Check if fully repaid
                if (this.remainingBalance <= 0.01) {
                    this.status    = LoanStatus.CLOSED;
                    this.closedAt  = LocalDateTime.now();
                }
                return;
            }
        }
    }

    /**
     * Records a missed payment and updates delinquency status.
     */
    public void recordMissedPayment(int installmentNumber) {
        for (RepaymentEntry entry : repaymentSchedule) {
            if (entry.getInstallmentNumber() == installmentNumber) {
                entry.setRepaymentStatus(RepaymentEntry.RepaymentStatus.MISSED);
                break;
            }
        }
        this.missedPayments++;
        this.isDelinquent = true;
        if (this.missedPayments >= 3) {
            this.status = LoanStatus.DEFAULTED;
        }
        this.updatedAt = LocalDateTime.now();
    }

    private void updateNextRepaymentDue() {
        for (RepaymentEntry entry : repaymentSchedule) {
            if (entry.getRepaymentStatus() == RepaymentEntry.RepaymentStatus.PENDING) {
                this.nextRepaymentDueAt = entry.getDueDate();
                return;
            }
        }
        this.nextRepaymentDueAt = null; // all paid
    }

    public int getPaidInstallmentsCount() {
        return (int) repaymentSchedule.stream()
                .filter(e -> e.getRepaymentStatus() == RepaymentEntry.RepaymentStatus.PAID)
                .count();
    }

    public int getRemainingInstallmentsCount() {
        return (int) repaymentSchedule.stream()
                .filter(e -> e.getRepaymentStatus() == RepaymentEntry.RepaymentStatus.PENDING)
                .count();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Getters & Setters
    // ═════════════════════════════════════════════════════════════════════════

    public int getLoanId() { return loanId; }
    public void setLoanId(int loanId) { this.loanId = loanId; }

    public String getLoanReferenceNumber() { return loanReferenceNumber; }
    public void setLoanReferenceNumber(String loanReferenceNumber) { this.loanReferenceNumber = loanReferenceNumber; }

    public int getBorrowerUserId() { return borrowerUserId; }
    public void setBorrowerUserId(int borrowerUserId) { this.borrowerUserId = borrowerUserId; }

    public String getBorrowerAccountId() { return borrowerAccountId; }
    public void setBorrowerAccountId(String borrowerAccountId) { this.borrowerAccountId = borrowerAccountId; }

    public String getBorrowerBusinessName() { return borrowerBusinessName; }
    public void setBorrowerBusinessName(String borrowerBusinessName) { this.borrowerBusinessName = borrowerBusinessName; }

    public double getRequestedAmount() { return requestedAmount; }
    public void setRequestedAmount(double requestedAmount) { this.requestedAmount = requestedAmount; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public String getPurposeDetails() { return purposeDetails; }
    public void setPurposeDetails(String purposeDetails) { this.purposeDetails = purposeDetails; }

    public LoanPurposeCategory getPurposeCategory() { return purposeCategory; }
    public void setPurposeCategory(LoanPurposeCategory purposeCategory) { this.purposeCategory = purposeCategory; }

    public String getSupportingDocumentPath() { return supportingDocumentPath; }
    public void setSupportingDocumentPath(String supportingDocumentPath) { this.supportingDocumentPath = supportingDocumentPath; }

    public double getMonthlyRevenueDeclared() { return monthlyRevenueDeclared; }
    public void setMonthlyRevenueDeclared(double monthlyRevenueDeclared) { this.monthlyRevenueDeclared = monthlyRevenueDeclared; }

    public double getAnnualRevenueDeclared() { return annualRevenueDeclared; }
    public void setAnnualRevenueDeclared(double annualRevenueDeclared) { this.annualRevenueDeclared = annualRevenueDeclared; }

    public int getYearsInBusinessDeclared() { return yearsInBusinessDeclared; }
    public void setYearsInBusinessDeclared(int yearsInBusinessDeclared) { this.yearsInBusinessDeclared = yearsInBusinessDeclared; }

    public double getExistingDebtAmount() { return existingDebtAmount; }
    public void setExistingDebtAmount(double existingDebtAmount) { this.existingDebtAmount = existingDebtAmount; }

    public double getApprovedAmount() { return approvedAmount; }
    public void setApprovedAmount(double approvedAmount) { this.approvedAmount = approvedAmount; }

    public double getInterestRate() { return interestRate; }
    public void setInterestRate(double interestRate) { this.interestRate = interestRate; }

    public int getTermMonths() { return termMonths; }
    public void setTermMonths(int termMonths) { this.termMonths = termMonths; }

    public double getMonthlyRepaymentAmount() { return monthlyRepaymentAmount; }
    public void setMonthlyRepaymentAmount(double monthlyRepaymentAmount) { this.monthlyRepaymentAmount = monthlyRepaymentAmount; }

    public LocalDateTime getRepaymentStartDate() { return repaymentStartDate; }
    public void setRepaymentStartDate(LocalDateTime repaymentStartDate) { this.repaymentStartDate = repaymentStartDate; }

    public double getPrincipalRemaining() { return principalRemaining; }
    public void setPrincipalRemaining(double principalRemaining) { this.principalRemaining = principalRemaining; }

    public double getInterestAccrued() { return interestAccrued; }
    public void setInterestAccrued(double interestAccrued) { this.interestAccrued = interestAccrued; }

    public double getTotalRepaid() { return totalRepaid; }
    public void setTotalRepaid(double totalRepaid) { this.totalRepaid = totalRepaid; }

    public double getRemainingBalance() { return remainingBalance; }
    public void setRemainingBalance(double remainingBalance) { this.remainingBalance = remainingBalance; }

    public List<RepaymentEntry> getRepaymentSchedule() { return repaymentSchedule; }
    public void setRepaymentSchedule(List<RepaymentEntry> repaymentSchedule) { this.repaymentSchedule = repaymentSchedule; }

    public LoanStatus getStatus() { return status; }
    public void setStatus(LoanStatus status) { this.status = status; this.updatedAt = LocalDateTime.now(); }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }

    public LocalDateTime getAppliedAt() { return appliedAt; }
    public void setAppliedAt(LocalDateTime appliedAt) { this.appliedAt = appliedAt; }

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }

    public LocalDateTime getDisbursedAt() { return disbursedAt; }
    public void setDisbursedAt(LocalDateTime disbursedAt) { this.disbursedAt = disbursedAt; }

    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getNextRepaymentDueAt() { return nextRepaymentDueAt; }
    public void setNextRepaymentDueAt(LocalDateTime nextRepaymentDueAt) { this.nextRepaymentDueAt = nextRepaymentDueAt; }

    public int getMissedPayments() { return missedPayments; }
    public void setMissedPayments(int missedPayments) { this.missedPayments = missedPayments; }

    public boolean isDelinquent() { return isDelinquent; }
    public void setDelinquent(boolean isDelinquent) { this.isDelinquent = isDelinquent; }

    @Override
    public String toString() {
        return "Loan{" +
                "loanReferenceNumber='" + loanReferenceNumber + '\'' +
                ", borrower='" + borrowerBusinessName + '\'' +
                ", requestedAmount=" + requestedAmount +
                ", approvedAmount=" + approvedAmount +
                ", status=" + status +
                ", remainingBalance=" + remainingBalance +
                '}';
    }
}
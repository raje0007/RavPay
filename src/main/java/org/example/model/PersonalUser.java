package org.example.model;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a personal (individual) account user in RevPay.
 * Extends the base User class with personal-specific fields.
 */
public class PersonalUser extends User {

    // ─── Personal Information ─────────────────────────────────────────────────
    private String firstName;
    private String lastName;
    private String dateOfBirth;        // stored as "YYYY-MM-DD"
    private String address;
    private String profilePictureUrl;

    // ─── Money Requests ───────────────────────────────────────────────────────
    private List<Transaction> sentRequests      = new ArrayList<>();  // MONEY_REQUEST type sent by this user
    private List<Transaction> receivedRequests  = new ArrayList<>();  // MONEY_REQUEST type received by this user

    // ═════════════════════════════════════════════════════════════════════════
    // Constructors
    // ═════════════════════════════════════════════════════════════════════════

    public PersonalUser() {
        super();
    }

    public PersonalUser(String firstName, String lastName, String username,
                        String email, String phoneNumber, String passwordHash,
                        String transactionPin,
                        String securityQuestion1, String securityAnswer1,
                        String securityQuestion2, String securityAnswer2) {
        super();
        this.firstName = firstName;
        this.lastName  = lastName;
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
        return AccountType.PERSONAL;
    }

    @Override
    public String getDisplayName() {
        return firstName + " " + lastName;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Business Logic Helpers
    // ═════════════════════════════════════════════════════════════════════════

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public void addSentRequest(Transaction request) {
        this.sentRequests.add(request);
    }

    public void addReceivedRequest(Transaction request) {
        this.receivedRequests.add(request);
    }

    public void removeSentRequest(Transaction request) {
        this.sentRequests.remove(request);
    }

    public long getPendingReceivedRequestCount() {
        return receivedRequests.stream()
                .filter(t -> t.getStatus() == Transaction.TransactionStatus.PENDING)
                .count();
    }

    public long getPendingSentRequestCount() {
        return sentRequests.stream()
                .filter(t -> t.getStatus() == Transaction.TransactionStatus.PENDING)
                .count();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Getters & Setters
    // ═════════════════════════════════════════════════════════════════════════

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getProfilePictureUrl() { return profilePictureUrl; }
    public void setProfilePictureUrl(String profilePictureUrl) { this.profilePictureUrl = profilePictureUrl; }

    public List<Transaction> getSentRequests() { return sentRequests; }
    public void setSentRequests(List<Transaction> sentRequests) { this.sentRequests = sentRequests; }

    public List<Transaction> getReceivedRequests() { return receivedRequests; }
    public void setReceivedRequests(List<Transaction> receivedRequests) { this.receivedRequests = receivedRequests; }

    @Override
    public String toString() {
        return "PersonalUser{" +
                "accountId='" + getAccountId() + '\'' +
                ", name='" + getFullName() + '\'' +
                ", email='" + getEmail() + '\'' +
                ", status=" + getStatus() +
                '}';
    }
}

package com.example.edi.common.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "eligibility_responses")
public class EligibilityResponse {

    @Id
    private String id;
    private String status;
    private String errorMessage;
    private String filePath;
    private LocalDateTime receivedAt;
    private String payerName;
    private String payerId;
    private String subscriberFirstName;
    private String subscriberLastName;
    private String memberId;
    private String groupNumber;
    private String eligibilityStatus;
    private LocalDate coverageStartDate;
    private LocalDate coverageEndDate;
    private List<BenefitDetail> benefits;

    public EligibilityResponse() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public LocalDateTime getReceivedAt() { return receivedAt; }
    public void setReceivedAt(LocalDateTime receivedAt) { this.receivedAt = receivedAt; }

    public String getPayerName() { return payerName; }
    public void setPayerName(String payerName) { this.payerName = payerName; }

    public String getPayerId() { return payerId; }
    public void setPayerId(String payerId) { this.payerId = payerId; }

    public String getSubscriberFirstName() { return subscriberFirstName; }
    public void setSubscriberFirstName(String subscriberFirstName) { this.subscriberFirstName = subscriberFirstName; }

    public String getSubscriberLastName() { return subscriberLastName; }
    public void setSubscriberLastName(String subscriberLastName) { this.subscriberLastName = subscriberLastName; }

    public String getMemberId() { return memberId; }
    public void setMemberId(String memberId) { this.memberId = memberId; }

    public String getGroupNumber() { return groupNumber; }
    public void setGroupNumber(String groupNumber) { this.groupNumber = groupNumber; }

    public String getEligibilityStatus() { return eligibilityStatus; }
    public void setEligibilityStatus(String eligibilityStatus) { this.eligibilityStatus = eligibilityStatus; }

    public LocalDate getCoverageStartDate() { return coverageStartDate; }
    public void setCoverageStartDate(LocalDate coverageStartDate) { this.coverageStartDate = coverageStartDate; }

    public LocalDate getCoverageEndDate() { return coverageEndDate; }
    public void setCoverageEndDate(LocalDate coverageEndDate) { this.coverageEndDate = coverageEndDate; }

    public List<BenefitDetail> getBenefits() { return benefits; }
    public void setBenefits(List<BenefitDetail> benefits) { this.benefits = benefits; }
}

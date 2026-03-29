package com.example.edi.common.document;

import java.math.BigDecimal;

public class BenefitDetail {

    private String benefitType;
    private String coverageLevel;
    private String serviceType;
    private Boolean inNetwork;
    private BigDecimal amount;
    private BigDecimal percent;
    private String timePeriod;
    private String message;

    public BenefitDetail() {}

    public String getBenefitType() { return benefitType; }
    public void setBenefitType(String benefitType) { this.benefitType = benefitType; }

    public String getCoverageLevel() { return coverageLevel; }
    public void setCoverageLevel(String coverageLevel) { this.coverageLevel = coverageLevel; }

    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }

    public Boolean getInNetwork() { return inNetwork; }
    public void setInNetwork(Boolean inNetwork) { this.inNetwork = inNetwork; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getPercent() { return percent; }
    public void setPercent(BigDecimal percent) { this.percent = percent; }

    public String getTimePeriod() { return timePeriod; }
    public void setTimePeriod(String timePeriod) { this.timePeriod = timePeriod; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}

package com.example.edi.insuranceresponse.service;

import com.example.edi.common.document.BenefitDetail;
import com.example.edi.common.document.EligibilityResponse;
import com.example.edi.insuranceresponse.domain.loop.BenefitInfo;
import com.example.edi.insuranceresponse.domain.loop.EDI271Response;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class EDI271Mapper {

    public EligibilityResponse map(EDI271Response response, String filePath, LocalDateTime receivedAt) {
        EligibilityResponse entity = new EligibilityResponse();

        entity.setStatus("COMPLETED");
        entity.setFilePath(filePath);
        entity.setReceivedAt(receivedAt);

        if (response.subscriberInfo() != null) {
            entity.setSubscriberFirstName(response.subscriberInfo().firstName());
            entity.setSubscriberLastName(response.subscriberInfo().lastName());
            entity.setMemberId(response.subscriberInfo().memberId());
            entity.setGroupNumber(response.subscriberInfo().groupNumber());
            entity.setEligibilityStatus(response.subscriberInfo().eligibilityStatus());
        }

        if (response.payerInfo() != null) {
            entity.setPayerName(response.payerInfo().name());
            entity.setPayerId(response.payerInfo().payerId());
        }

        if (response.benefits() != null) {
            List<BenefitDetail> details = response.benefits().stream()
                    .map(this::mapBenefitInfo)
                    .toList();
            entity.setBenefits(details);

            response.benefits().stream()
                    .filter(b -> b.coverageStartDate() != null || b.coverageEndDate() != null)
                    .findFirst()
                    .ifPresent(b -> {
                        entity.setCoverageStartDate(b.coverageStartDate());
                        entity.setCoverageEndDate(b.coverageEndDate());
                    });
        }

        return entity;
    }

    private BenefitDetail mapBenefitInfo(BenefitInfo info) {
        BenefitDetail detail = new BenefitDetail();
        detail.setBenefitType(info.benefitType());
        detail.setCoverageLevel(info.coverageLevel());
        detail.setServiceType(info.serviceType());
        detail.setInNetwork(info.inNetwork());
        detail.setAmount(info.amount());
        detail.setPercent(info.percent());
        detail.setTimePeriod(info.timePeriod());
        detail.setMessage(info.message());
        return detail;
    }
}

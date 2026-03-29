package com.example.edi.insurancerequest.domain.loop;

import com.example.edi.common.edi.loop.FunctionalGroup;
import com.example.edi.common.edi.loop.InterchangeEnvelope;
import com.example.edi.common.edi.loop.TransactionHeader;

import java.util.List;

public record EDI270Inquiry(
    InterchangeEnvelope envelope,
    FunctionalGroup functionalGroup,
    TransactionHeader transactionHeader,
    List<InformationSourceGroup> informationSourceGroups
) {}

package com.example.edi.claims.service;

import com.example.edi.common.document.Company;
import com.example.edi.common.document.Patient;
import com.example.edi.common.document.PlaceOfService;
import com.example.edi.common.document.Visit;
import org.springframework.stereotype.Service;

@Service
public class EDI837Service {

    /**
     * @deprecated This method will be replaced by EDI837Generator in Task 8.
     */
    @Deprecated
    public String to837(Company company, Patient patient, Visit visit, PlaceOfService pos) {
        throw new UnsupportedOperationException("Deprecated: use EDI837Generator instead");
    }
}

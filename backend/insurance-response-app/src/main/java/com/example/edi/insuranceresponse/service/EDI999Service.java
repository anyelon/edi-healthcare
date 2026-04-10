package com.example.edi.insuranceresponse.service;

import com.example.edi.common.edi.ack.EDI999Acknowledgment;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class EDI999Service {

    private final EDI999Parser edi999Parser;

    public EDI999Service(EDI999Parser edi999Parser) {
        this.edi999Parser = edi999Parser;
    }

    public List<EDI999Acknowledgment> processFile(MultipartFile file) throws IOException {
        return edi999Parser.parse(file.getInputStream());
    }
}

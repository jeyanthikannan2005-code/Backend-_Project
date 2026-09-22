package com.example.smartinvent.controller;

import com.example.smartinvent.service.QrCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class QrCodeController {
    @Autowired
    private QrCodeService qrCodeService;

    @GetMapping("/upi-qr")
    public Map<String, String> upiQr(
            @RequestParam double amount,
            @RequestParam(required = false) String invoiceNumber) {
        return qrCodeService.generate(amount, invoiceNumber);
    }
}

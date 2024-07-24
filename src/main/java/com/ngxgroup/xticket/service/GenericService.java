package com.ngxgroup.xticket.service;

import com.ngxgroup.xticket.payload.XTicketPayload;
import java.util.concurrent.CompletableFuture;

/**
 *
 * @author briano
 */
public interface GenericService {

    String encryptString(String textToEncrypt);

    String decryptString(String textToDecrypt);

    String getTOTP(String secretKey);

    String generateRequestId();

    String generateTOTPSecretKey();

    CompletableFuture<String> sendEmail(XTicketPayload requestPayload, String principal);
    
    String generateFileName();
    
    CompletableFuture<String> logResponse(String username, Object refNo, String auditClass, String auditCategory, String auditAction, String oldValue, String newValue);
}

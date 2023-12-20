package com.ngxgroup.xpolicy.service;

import com.ngxgroup.xpolicy.payload.XPolicyPayload;
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

    CompletableFuture<String> sendEmail(XPolicyPayload requestPayload, String principal);
}

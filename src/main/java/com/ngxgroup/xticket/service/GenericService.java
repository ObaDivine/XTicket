package com.ngxgroup.xticket.service;

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

    String generateFileName();

    CompletableFuture<String> pushNotification(String responseCode, String responseMessage, String sessionId);

    CompletableFuture<String> logResponse(String username, Object refNo, String auditClass, String auditCategory, String auditAction, String oldValue, String newValue);

    void generateLog(String app, String logMessage, String logType, String logLevel, String requestId);

    String httpGet(String url);

    String httpPost(String url, String requestBody, String randomId);
}

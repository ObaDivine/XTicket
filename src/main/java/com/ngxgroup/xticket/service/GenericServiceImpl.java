package com.ngxgroup.xticket.service;

import com.ngxgroup.xticket.controller.PushNotificationController;
import com.ngxgroup.xticket.model.AuditLog;
import com.ngxgroup.xticket.payload.XTicketPayload;
import com.ngxgroup.xticket.repository.XTicketRepository;
import de.taimos.totp.TOTP;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.time.LocalDateTime;
import java.util.Random;
import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.binary.Hex;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 *
 * @author briano
 */
@Service
public class GenericServiceImpl implements GenericService {

    @Autowired
    XTicketRepository xticketRepository;
    @Autowired
    PushNotificationController pushNotification;
    @Value("${xticket.encryption.key.web}")
    private String encryptionKey;
    private static SecretKeySpec secretKey;
    private static byte[] key;
    static final Logger logger = Logger.getLogger(GenericServiceImpl.class.getName());

    @Override
    public String decryptString(String textToDecrypt) {
        try {
            setKey(encryptionKey.trim());
            Cipher cipher = Cipher.getInstance("AES/GCM/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            String decryptedResponse = new String(cipher.doFinal(java.util.Base64.getDecoder().decode(textToDecrypt.trim())));
            String[] splitString = decryptedResponse.split(":");
            StringJoiner rawString = new StringJoiner(":");
            for (String str : splitString) {
                rawString.add(str.trim());
            }
            return rawString.toString();
        } catch (Exception ex) {
            return ex.getMessage();
        }
    }

    @Override
    public String getTOTP(String secretKey) {
        Base32 base32 = new Base32();
        byte[] bytes = base32.decode(secretKey);
        String hexKey = Hex.encodeHexString(bytes);
        return TOTP.getOTP(hexKey);
    }

    @Override
    public String generateRequestId() {
        SecureRandom secureRnd = new SecureRandom();
        int max = 12;
        String alphaNumericString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder mnemonic = new StringBuilder(max);
        for (int i = 0; i < max; i++) {
            mnemonic.append(alphaNumericString.charAt(secureRnd.nextInt(alphaNumericString.length())));
        }
        return mnemonic.toString();
    }

    @Override
    public String encryptString(String textToEncrypt) {
        try {
            String secret = encryptionKey;
            setKey(secret);
            Cipher cipher = Cipher.getInstance("AES/GCM/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return java.util.Base64.getEncoder().encodeToString(cipher.doFinal(textToEncrypt.trim().getBytes("UTF-8")));
        } catch (Exception ex) {
            return ex.getMessage();
        }
    }

    public static void setKey(String myKey) {
        MessageDigest sha = null;
        try {
            key = myKey.getBytes("UTF-8");
            sha = MessageDigest.getInstance("SHA-1");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16);
            secretKey = new SecretKeySpec(key, "AES");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public String generateTOTPSecretKey() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        Base32 base32 = new Base32();
        return base32.encodeToString(bytes);
    }

    @Override
    public String generateFileName() {
        try {
            int leftLimit = 48; // numeral '0'
            int rightLimit = 122; // letter 'z'
            int targetStringLength = 25;
            Random random = new SecureRandom();

            var generatedString = random.ints(leftLimit, rightLimit + 1)
                    .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                    .limit(targetStringLength)
                    .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                    .toString().toUpperCase();
            return generatedString;
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public CompletableFuture<String> logResponse(String username, Object refNo, String auditClass, String auditCategory, String auditAction, String oldValue, String newValue) {
        AuditLog newLog = new AuditLog();
        newLog.setAuditAction(auditAction);
        newLog.setAuditCategory(auditCategory);
        newLog.setAuditClass(auditClass);
        newLog.setCreatedAt(LocalDateTime.now());
        newLog.setNewValue(newValue);
        newLog.setOldValue(oldValue);
        newLog.setRefNo(String.valueOf(refNo));
        newLog.setUsername(username);
        xticketRepository.createAuditLog(newLog);
        return CompletableFuture.completedFuture("Success");
    }

    @Override
    public CompletableFuture<String> pushNotification(String responseCode, String responseMessage, String sessionId) {
        XTicketPayload requestPayload = new XTicketPayload();
        requestPayload.setResponseCode(responseCode);
        requestPayload.setResponseMessage(responseMessage);
        requestPayload.setSessionId(sessionId);
        //Push the notification
        pushNotification.pushNotification(requestPayload);
        return CompletableFuture.completedFuture("Success");
    }

}

package com.ngxgroup.xticket.service;

import com.google.gson.Gson;
import com.ngxgroup.xticket.payload.LogPayload;
import com.ngxgroup.xticket.payload.XTicketPayload;
import de.taimos.totp.TOTP;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Properties;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.activation.FileDataSource;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.binary.Hex;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 *
 * @author briano
 */
@Service
public class GenericServiceImpl implements GenericService {

    @Autowired
    Gson gson;
    @Value("${xticket.encryption.key.web}")
    private String encryptionKey;
    @Value("${email.login}")
    private String mailLogin;
    @Value("${email.from}")
    private String mailFrom;
    @Value("${email.password}")
    private String mailPassword;
    @Value("${email.host}")
    private String mailHost;
    @Value("${email.port}")
    private String mailPort;
    @Value("${email.protocol}")
    private String mailProtocol;
    @Value("${email.trust}")
    private String mailTrust;
    private static SecretKeySpec secretKey;
    private static byte[] key;
    static final Logger logger = Logger.getLogger(XTicketServiceImpl.class.getName());

    @Override
    public String decryptString(String textToDecrypt) {
        try {
            setKey(encryptionKey.trim());
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
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
        String ALPHA_NUMERIC_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder mnemonic = new StringBuilder(max);
        for (int i = 0; i < max; i++) {
            mnemonic.append(ALPHA_NUMERIC_STRING.charAt(secureRnd.nextInt(ALPHA_NUMERIC_STRING.length())));
        }
        return mnemonic.toString();
    }

    @Override
    public String encryptString(String textToEncrypt) {
        try {
            String secret = encryptionKey;
            setKey(secret);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
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

    @Async
    @Override
    public CompletableFuture<String> sendEmail(XTicketPayload requestPayload, String principal) {
        LogPayload log = new LogPayload();
        log.setUsername(principal);
        log.setSource("Send Mail");
        try {
            JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
            mailSender.setHost(mailHost);
            mailSender.setPort(Integer.parseInt(mailPort));

            mailSender.setUsername(mailLogin);
            mailSender.setPassword(mailPassword);

            Properties props = mailSender.getJavaMailProperties();
            props.put("mail.transport.protocol", mailProtocol);
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "false");
            props.put("mail.debug", "true");
            props.put("mail.smtp.ssl.trust", mailTrust);

            MimeMessage emailDetails = mailSender.createMimeMessage();
            emailDetails.setFrom(mailFrom);
            emailDetails.setRecipients(Message.RecipientType.TO, requestPayload.getRecipientEmail());
            emailDetails.setSubject(requestPayload.getEmailSubject());

            BodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setContent(requestPayload.getEmailBody(), "text/html");

            if (requestPayload.getAttachmentFilePath() != null && !requestPayload.getAttachmentFilePath().equalsIgnoreCase("")) {
                //Add the attachment
                MimeBodyPart attachmentBodyPart = new MimeBodyPart();
                DataSource source = new FileDataSource(requestPayload.getAttachmentFilePath());
                attachmentBodyPart.setDataHandler(new DataHandler(source));
                attachmentBodyPart.setFileName(requestPayload.getAttachmentFilePath());

                Multipart multipart = new MimeMultipart();
                multipart.addBodyPart(messageBodyPart);
                multipart.addBodyPart(attachmentBodyPart);
                emailDetails.setContent(multipart);
            }
            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);
            emailDetails.setContent(multipart);

            mailSender.send(emailDetails);
            log.setMessage(requestPayload.getEmailSubject() + " mail sent to " + requestPayload.getRecipientEmail());
            log.setSeverity("INFO");
            logger.log(Level.SEVERE, gson.toJson(log));
        } catch (Exception ex) {
            log.setMessage(ex.getMessage());
            log.setSeverity("SEVERE");
            logger.log(Level.SEVERE, gson.toJson(log));
        }
        return CompletableFuture.completedFuture("Success");
    }
}

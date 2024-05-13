package com.ngxgroup.xticket.payload;

import com.ngxgroup.xticket.model.AppUser;
import com.ngxgroup.xticket.model.AuditLog;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * @author briano
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class XTicketPayload {

    @Value("${xticket.default.email.domain}")
    private String companyEmailDomain;
    private String email;
    private String responseCode;
    private String responseMessage;
    private String username;
    private String password;
    private String newPassword;
    private String confirmPassword;
    private String lastName;
    private String otherName;
    private String mobileNumber;
    private String gender;
    private boolean internal;
    private boolean requireServiceRequestForm;
    private boolean requireChangeRequestForm;
    private String termsService;
    private String ticketGroupCode;
    private String ticketGroupName;
    private String ticketTypeCode;
    private String ticketTypeName;
    private String status;
    private String createdAt;
    private String createdBy;
    private int id = 0;
    private List<XTicketPayload> data;
    private String escalationEmails;
    private int slaMins;
    private String recipientEmail;
    private String emailSubject;
    private String emailBody;
    private String attachmentFilePath;
    private String groupName;
    private String roleName;
    private String rolesToUpdate;
    private String roleExist;
    private String lastLogin;
    private String newValue;
    private String action;
    private String message;
    private String subject;
    private List<MultipartFile> uploadedFiles;
}

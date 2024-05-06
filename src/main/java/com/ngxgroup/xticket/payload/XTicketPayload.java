package com.ngxgroup.xticket.payload;

import com.ngxgroup.xticket.model.AppUser;
import com.ngxgroup.xticket.model.AuditLog;
import com.ngxgroup.xticket.model.TicketGroup;
import com.ngxgroup.xticket.model.TicketReopened;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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

    private String email;
    private String responseCode;
    private String responseMessage;
    private String password;
    private String confirmPassword;
    private String fullName;
    private String mobileNumber;
    
    //To be filtered
    private String qrCodeImage;
    private String policyType;
    private String policyName;
    private String policyDescription;
    private String policyAuthor;
    private String policyCode;
    private String policyId;
    private String fileSize;
    private String lastReview;
    private String policyImage;
    private String policyDocumentId;
    private boolean underReview;
    private String expiryDate;
    private int accessLevel = 1;
    private boolean mustRead;
    private int id = 0;
    private String company;
    private String division;
    private String department;
    private String updateCompany;
    private String updateDivision;
    private String updateDepartment;
    private MultipartFile fileUpload;
    private String userType;
    private boolean enabled;
    private boolean locked;
    private boolean policyChampion;
    private boolean failedLogin = false;
    private int failedLoginCount = 0;
    private String reviewedBy;
    private String reviewedAt;
    private String reviewComment;
    private String reportType;
    private String startDate;
    private String endDate;
    private String reportCategory;
    private String reportPage;
    private List<TicketGroup> policies;
    private List<AuditLog> userActivity;
    private List<AppUser> appUsers;
    private String groupName;
    private String roleName;
    private String rolesToUpdate;
    private String roleExist;
    private String roleGroup;
    private String recipientEmail;
    private String emailSubject;
    private String emailBody;
    private String attachmentFilePath;
    private String comment;
    private String reference;
}

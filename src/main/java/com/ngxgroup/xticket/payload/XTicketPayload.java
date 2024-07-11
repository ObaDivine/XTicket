package com.ngxgroup.xticket.payload;

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

    String adAuthDomains;
    String email;
    String responseCode;
    String responseMessage;
    String username;
    String password;
    String newPassword;
    String confirmPassword;
    String lastName;
    String otherName;
    String mobileNumber;
    String gender;
    boolean internal;
    boolean agent;
    boolean requireServiceRequestForm;
    boolean requireChangeRequestForm;
    String termsService;
    String ticketId;
    String entityCode;
    String entityName;
    String fromEntity;
    String toEntity;
    String serviceUnitCode;
    String serviceUnitName;
    String ticketGroupCode;
    String ticketGroupName;
    String ticketTypeCode;
    String ticketTypeName;
    String ticketSlaName;
    String ticketAgent;
    String ticketStatusCode;
    String ticketStatusName;
    boolean pauseSLA;
    int ticketCount;
    int ticketSla;
    boolean ticketLocked;
    boolean ticketReopened;
    boolean ticketReassigned;
    char ticketSlaPeriod;
    String initialSla;
    String newSla;
    boolean slaViolated;
    int reopenedId = 0;
    String reopenedAt;
    String reopenedBy;
    boolean reopened;
    String reassignedAt;
    String reassignedBy;
    String reassignedTo;
    String status;
    String createdAt;
    String createdBy;
    String closedAt;
    String closedBy;
    int id = 0;
    List<XTicketPayload> data;
    List<XTicketPayload> reopenedTickets;
    List<XTicketPayload> reassignedTickets;
    List<XTicketPayload> ticketEscalations;
    List<XTicketPayload> ticketComments;
    List<XTicketPayload> uploadDocuments;
    String escalationEmails;
    boolean escalated;
    boolean ticketOpen;
    boolean locked;
    boolean activated;
    int slaMins;
    String slaExpiry;
    String recipientEmail;
    String emailSubject;
    String emailBody;
    String attachmentFilePath;
    String groupName;
    String roleName;
    String rolesToUpdate;
    String roleExist;
    String lastLogin;
    String newValue;
    String action;
    String message;
    String messageFrom;
    String subject;
    List<MultipartFile> uploadedFiles;
    String priority;
    boolean reply;
    //Used for ECharts
    String name;
    int value;
    String source;
    int fileIndex;
    String startDate;
    String endDate;
    String timeElapsed;
    String documentLink;
    String originalFileName;
    String passwordChangeDate;
    String resetTime;
}

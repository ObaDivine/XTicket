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

    private String adAuthDomains;
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
    private boolean agent;
    private boolean requireServiceRequestForm;
    private boolean requireChangeRequestForm;
    private String termsService;
    private String ticketId;
    private String entityCode;
    private String entityName;
    private String fromEntity;
    private String toEntity;
    private String serviceUnitCode;
    private String serviceUnitName;
    private String ticketGroupCode;
    private String ticketGroupName;
    private String ticketTypeCode;
    private String ticketTypeName;
    private String ticketSlaName;
    private String ticketAgent;
    private String ticketStatusCode;
    private String ticketStatusName;
    private boolean pauseSLA;
    private int ticketCount;
    private int ticketSla;
    private boolean ticketLocked;
    private boolean ticketReopened;
    private boolean ticketReassigned;
    private char ticketSlaPeriod;
    private String initialSla;
    private String newSla;
    private boolean slaViolated;
    private int reopenedId = 0;
    private String reopenedAt;
    private String reopenedBy;
    private boolean reopened;
    private String reassignedAt;
    private String reassignedBy;
    private String reassignedTo;
    private String status;
    private String createdAt;
    private String createdBy;
    private String closedAt;
    private String closedBy;
    private int id = 0;
    private List<XTicketPayload> data;
    private List<XTicketPayload> reopenedTickets;
    private List<XTicketPayload> reassignedTickets;
    private List<XTicketPayload> ticketEscalations;
    private List<XTicketPayload> ticketComments;
    private List<XTicketPayload> uploadDocuments;
    private String escalationEmails;
    private boolean escalated;
    private boolean ticketOpen;
    private int slaMins;
    private String slaExpiry;
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
    private String messageFrom;
    private String subject;
    private List<MultipartFile> uploadedFiles;
    private String priority;
    private boolean reply;
    //Used for ECharts
    private String name;
    private int value;
    private String source;
    private int fileIndex;
    private String startDate;
    private String endDate;
    private String timeElapsed;
    private String documentLink;
    private String originalFileName;
}

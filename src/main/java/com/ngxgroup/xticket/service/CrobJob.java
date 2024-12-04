package com.ngxgroup.xticket.service;

import com.ngxgroup.xticket.model.AppUser;
import com.ngxgroup.xticket.model.AutomatedTicket;
import com.ngxgroup.xticket.model.EmailTemp;
import com.ngxgroup.xticket.model.Emails;
import com.ngxgroup.xticket.model.TicketEscalations;
import com.ngxgroup.xticket.model.TicketStatus;
import com.ngxgroup.xticket.model.Tickets;
import com.ngxgroup.xticket.payload.XTicketPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.ngxgroup.xticket.repository.XTicketRepository;
import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.activation.FileDataSource;
import jakarta.mail.Address;
import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;

/**
 *
 * @author bokon
 */
@Service
public class CrobJob {

    @Autowired(required = false)
    private List<SessionRegistry> sessionRegistries;
    @Autowired
    XTicketRepository xticketRepository;
    @Autowired
    XTicketService xticketService;
    @Value("${xticket.host}")
    private String host;
    @Value("${xticket.company.name}")
    private String companyName;
    @Value("${xticket.company.email}")
    private String companyEmail;
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
//    @Value("${xticket.email.escalation.interval}")
//    private int escalationSla;
    @Value("${xticket.slaexpiry.notification}")
    private int slaExpiryNotificationInterval;
//    @Value("${xticket.escalation.wait.time}")
//    private long escalationWaitTime;
//    @Value("${xticket.escalation.wait.unit}")
//    private String escalationWaitUnit;
    @Value("${server.servlet.session.timeout}")
    private int sessionTimeout;
    DateTimeFormatter timeDtf = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Scheduled(cron = "${xticket.cron.job.ticketrun}")
    public void setEscalateViolatedSLA() {
        //Ftech all the tickets that are open
        TicketStatus openStatus = xticketRepository.getTicketStatusUsingCode("OPEN");
        List<Tickets> openTickets = xticketRepository.getTicketsByStatus(openStatus);
        if (openTickets != null) {
            for (Tickets t : openTickets) {
                if (LocalDateTime.now().isAfter(t.getSlaExpiry())) {
                    //Check if the escalation is complete
                    if (t.getEscalationIndex() < t.getTicketType().getEscalationEmails().split(",").length) {
                        int escalationIndex = t.getEscalationIndex();
                        String recipient = "";
                        String carbonCopyEmail = "";
                        String recipientEmail = t.getTicketType().getEscalationEmails().split(",")[escalationIndex];
                        String escalationSla = t.getTicketType().getEscalationSla().split(",")[escalationIndex];
                        String sla = escalationSla.replace("M", "").replace("H", "").replace("D", "");
                        String slaDuration = escalationSla.replace(sla, "");

                        AppUser recipientUser = xticketRepository.getAppUserUsingEmail(t.getTicketType().getEscalationEmails().split(",")[escalationIndex]);
                        if (recipientUser != null) {
                            recipient = "Dear " + recipientUser.getLastName() + ", " + recipientUser.getOtherName();
                        } else {
                            recipient = "Dear Sir/Madam,";
                        }
                        String slaTime = timeDtf.format(t.getSlaExpiry().toLocalTime());
                        String slaDate = t.getSlaExpiry().getMonth().toString() + " " + t.getSlaExpiry().getDayOfMonth() + ", " + t.getSlaExpiry().getYear();
                        String message = "<h4>" + recipient + "</h4>\n"
                                + "<p>An SLA for <b>" + t.getTicketType().getTicketTypeName() + "</b> ticket with an ID <b>" + t.getTicketId()
                                + "</b> and priority <b>" + t.getTicketType().getSla().getPriority() + "</b> has been violated by <b>" + t.getTicketAgent().getAgent().getLastName() + ", " + t.getTicketAgent().getAgent().getOtherName()
                                + "</b> in <b>" + t.getTicketType().getServiceUnit().getServiceUnitName() + ".</b></p>"
                                + "<p>The ticket expired at <b>" + slaTime + "</b> on <b>" + slaDate + "</b></p>"
                                + "<p>To view the ticket details or take action, kindly login into NGX X-Ticket by <a href=\"" + host + "/xticket" + "\">clicking here</a></p>"
                                + "<p>For support and enquiries, email: " + companyEmail + ".</p>"
                                + "<p>Best wishes,</p>"
                                + "<p>" + companyName + "</p>";

                        //Check if its the first escalation
                        long timeElapsedAfterSlaExpiry = 0;
                        if (escalationIndex == 0) {
                            carbonCopyEmail = t.getTicketAgent().getAgent().getEmail();
                            //Check if the required wait time is exhausted
                            if (slaDuration.equalsIgnoreCase("M")) {
                                timeElapsedAfterSlaExpiry = Duration.between(t.getSlaExpiry(), LocalDateTime.now()).toMinutes();
                            } else if (slaDuration.equalsIgnoreCase("H")) {
                                timeElapsedAfterSlaExpiry = Duration.between(t.getSlaExpiry(), LocalDateTime.now()).toHours();
                            } else {
                                timeElapsedAfterSlaExpiry = Duration.between(t.getSlaExpiry(), LocalDateTime.now()).toDays();
                            }
                        } else {
                            int currentEscalationIndex = t.getEscalationIndex();
                            carbonCopyEmail = t.getTicketType().getEscalationEmails().split(",")[currentEscalationIndex - 1];
                            //Check if the required wait time is exhausted
                            if (slaDuration.equalsIgnoreCase("M")) {
                                timeElapsedAfterSlaExpiry = Duration.between(t.getEscalatedAt(), LocalDateTime.now()).toMinutes();
                            } else if (slaDuration.equalsIgnoreCase("H")) {
                                timeElapsedAfterSlaExpiry = Duration.between(t.getEscalatedAt(), LocalDateTime.now()).toHours();
                            } else {
                                timeElapsedAfterSlaExpiry = Duration.between(t.getEscalatedAt(), LocalDateTime.now()).toDays();
                            }
                        }

                        //Check if to send mail now or wait
                        if (sla.equalsIgnoreCase("0")) {
                            //Run the escalation now
                            t.setEscalationIndex(t.getEscalationIndex() + 1);
                            t.setEscalated(true);
                            t.setEscalatedAt(LocalDateTime.now());
                            t.setSlaViolated(true);
                            t.setSlaViolatedAt(LocalDateTime.now());
                            t.setTicketAgentViolated(t.getTicketAgent());
                            xticketRepository.updateTicket(t);

                            //Add to the ticket escalations
                            TicketEscalations newEscalation = new TicketEscalations();
                            newEscalation.setCreatedAt(LocalDateTime.now());
                            newEscalation.setEscalatedTo(recipientEmail);
                            newEscalation.setSlaExpiresAt(t.getSlaExpiry());
                            newEscalation.setTicket(t);
                            xticketRepository.createTicketEscalation(newEscalation);

                            //Escalate the email and push email notification
                            EmailTemp emailTemp = new EmailTemp();
                            emailTemp.setCreatedAt(LocalDateTime.now());
                            emailTemp.setEmail(recipientEmail.trim());
                            emailTemp.setError("");
                            emailTemp.setMessage(message);
                            emailTemp.setStatus("Pending");
                            emailTemp.setSubject("Ticket SLA Violation Notification");
                            emailTemp.setTryCount(0);
                            emailTemp.setCarbonCopy(carbonCopyEmail.trim());
                            emailTemp.setFileAttachment("");
                            xticketRepository.createEmailTemp(emailTemp);
                        } else {
                            if (timeElapsedAfterSlaExpiry >= Long.parseLong(sla)) {
                                //Update the escalation index
                                t.setEscalationIndex(t.getEscalationIndex() + 1);
                                t.setEscalatedAt(LocalDateTime.now());
                                xticketRepository.updateTicket(t);

                                //Add to the ticket escalations
                                TicketEscalations newEscalation = new TicketEscalations();
                                newEscalation.setCreatedAt(LocalDateTime.now());
                                newEscalation.setEscalatedTo(recipientEmail);
                                newEscalation.setSlaExpiresAt(t.getSlaExpiry());
                                newEscalation.setTicket(t);
                                xticketRepository.createTicketEscalation(newEscalation);

                                //Escalate the email and push email notification
                                EmailTemp emailTemp = new EmailTemp();
                                emailTemp.setCreatedAt(LocalDateTime.now());
                                emailTemp.setEmail(recipientEmail.trim());
                                emailTemp.setError("");
                                emailTemp.setMessage(message);
                                emailTemp.setStatus("Pending");
                                emailTemp.setSubject("Ticket SLA Violation Notification");
                                emailTemp.setTryCount(0);
                                emailTemp.setCarbonCopy(carbonCopyEmail.trim());
                                emailTemp.setFileAttachment("");
                                xticketRepository.createEmailTemp(emailTemp);
                            }
                        }
                    }
                }

                //Notify agents for tickets about to violate SLA
                long timeElapsed = Duration.between(LocalDateTime.now(), t.getSlaExpiry()).toMinutes();
                if ((timeElapsed <= slaExpiryNotificationInterval) && !t.isAgentNotifiedOfExpiry()) {
                    //Send notification email to the agent
                    String slaTime = timeDtf.format(t.getSlaExpiry().toLocalTime());
                    String slaDate = t.getSlaExpiry().getMonth().toString() + " " + t.getSlaExpiry().getDayOfMonth() + ", " + t.getSlaExpiry().getYear();
                    String message = "<h4>Dear " + t.getTicketAgent().getAgent().getLastName() + ",</h4>\n"
                            + "<p>An SLA for <b>" + t.getTicketType().getTicketTypeName() + "</b> ticket with an ID <b>" + t.getTicketId()
                            + "</b> is about to be violated. The priority is <b>" + t.getTicketType().getSla().getPriority() + ".</b></p>"
                            + "<p>The ticket is set to expire by <b>" + slaTime + "</b> on <b>" + slaDate + "</b></p>"
                            + "<p>To view the ticket details or take action, kindly login to NGX X-Ticket by <a href=\"" + host + "/xticket" + "\">clicking here</a></p>"
                            + "<p>For support and enquiries, email: " + companyEmail + ".</p>\n"
                            + "<p>Best wishes,</p>"
                            + "<p>" + companyName + "</p>";

                    EmailTemp emailTemp = new EmailTemp();
                    emailTemp.setCreatedAt(LocalDateTime.now());
                    emailTemp.setEmail(t.getTicketAgent().getAgent().getEmail().trim());
                    emailTemp.setError("");
                    emailTemp.setMessage(message);
                    emailTemp.setStatus("Pending");
                    emailTemp.setSubject("Ticket SLA Violation Notification");
                    emailTemp.setTryCount(0);
                    emailTemp.setCarbonCopy("");
                    emailTemp.setFileAttachment("");
                    xticketRepository.createEmailTemp(emailTemp);
                    t.setAgentNotifiedOfExpiry(true);
                    xticketRepository.updateTicket(t);
                }
            }
        }
    }

    @Scheduled(fixedDelay = 1000)
    public void automatedTicket() {
        //Fetch automated tickets
        List<AutomatedTicket> automatedTickets = xticketRepository.getTodayAutomatedTicket();
        if (automatedTickets != null) {
            for (AutomatedTicket t : automatedTickets) {
                //Check if the next run date is today
                if (t.getRuntime().compareTo(LocalTime.now()) <= 0) {
                    //Trigger ticket push
                    XTicketPayload requestPayload = new XTicketPayload();
                    requestPayload.setTicketTypeCode(t.getTicketType().getTicketTypeCode());
                    requestPayload.setInternal(t.getTicketType().isInternal());
                    requestPayload.setMessage(t.getMessage());
                    requestPayload.setSubject(t.getSubject());
                    requestPayload.setUploadedFiles(null);
                    requestPayload.setAutomated(true); //Set the automation
                    xticketService.createTicket(requestPayload, t.getServiceRequester());

                    //Set the next run date
                    LocalDate nextRun = null;
                    switch (t.getFrequency()) {
                        case "Daily" -> {
                            nextRun = LocalDate.now().plusDays(1);
                        }
                        case "Weekly" -> {
                            nextRun = LocalDate.now().plusWeeks(1);
                        }
                        case "Monthly" -> {
                            nextRun = LocalDate.now().plusMonths(1);
                        }
                        case "Annual" -> {
                            nextRun = LocalDate.now().plusYears(1);
                        }
                        case "BiAnnual" -> {
                            nextRun = LocalDate.now().plusDays(183);
                        }
                        case "Quarterly" -> {
                            nextRun = LocalDate.now().plusDays(90);
                        }
                    }

                    if (t.getEndDate() == null || t.getEndDate().isEqual(nextRun) || t.getEndDate().isAfter(nextRun)) {
                        //No end date
                        t.setNextRun(nextRun);
                        t.setRunCount(t.getRunCount() + 1);
                        xticketRepository.updateAutomatedTicket(t);
                    } else {
                        //Disable the auto ticket
                        t.setStatus("Disabled");
                        xticketRepository.updateAutomatedTicket(t);
                    }
                }
            }
        }
    }

    @Scheduled(fixedDelay = 1000)
    public void sendEmail() {
        //Fetch all the pending emails
        List<EmailTemp> pendingEmail = xticketRepository.getPendingEmails();
        if (pendingEmail != null) {
            for (EmailTemp email : pendingEmail) {
                String[] recipient = null;
                String[] cc = null;
                if (email.getEmail() == null || email.getEmail().equalsIgnoreCase("")) {
                    recipient = new String[]{""};
                } else {
                    recipient = email.getEmail().split(",");
                }

                if (email.getCarbonCopy() == null || email.getCarbonCopy().equalsIgnoreCase("")) {
                    cc = new String[]{""};
                } else {
                    cc = email.getCarbonCopy().split(",");
                }

                String[] response = sendEmail(recipient, email.getSubject(), email.getMessage(), cc, email.getFileAttachment());
                if (response[0].equalsIgnoreCase("Success")) {
                    Emails pemEmail = new Emails();
                    BeanUtils.copyProperties(email, pemEmail);
                    pemEmail.setId(null);
                    pemEmail.setStatus("Completed");
                    xticketRepository.createEmail(pemEmail);
                    //Delete from the pending email
                    xticketRepository.deleteEmailTemp(email);
                } else {
                    email.setTryCount(email.getTryCount() + 1);
                    email.setError(response[1]);
                    xticketRepository.updateEmailTemp(email);
                }
            }
        }
    }

    private String[] sendEmail(String[] recipient, String subject, String emailBody, String[] carbonCopy, String fileAttachment) {
        try {
            JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
            mailSender.setHost(mailHost);
            mailSender.setPort(Integer.parseInt(mailPort));

            mailSender.setUsername(mailLogin);
            mailSender.setPassword(mailPassword);

            Properties props = mailSender.getJavaMailProperties();
            props.put("mail.transport.protocol", mailProtocol);
            props.put("mail.smtp.auth", true);
            props.put("mail.smtp.starttls.enable", true);
            props.put("mail.debug", "true");
            props.put("mail.smtp.ssl.trust", mailTrust);
            props.put("mail.smtp.ssl.enable", false);

            MimeMessage emailDetails = mailSender.createMimeMessage();
            Address[] addresses = {};
            List<Address> recipientList = new ArrayList<>();
            for (String addr : recipient) {
                if (addr.matches("^[a-zA-Z0-9_!#$%&’*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$")) {
                    Address address = new InternetAddress(addr.trim());
                    recipientList.add(address);
                }
            }

            if (carbonCopy.length > 0) {
                Address[] cc = {};
                List<Address> carbonCopyList = new ArrayList<>();
                for (String addr : carbonCopy) {
                    if (addr.matches("^[a-zA-Z0-9_!#$%&’*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$")) {
                        Address address = new InternetAddress(addr.trim());
                        carbonCopyList.add(address);
                    }
                }
                if (!carbonCopyList.isEmpty()) {
                    emailDetails.setRecipients(Message.RecipientType.CC, carbonCopyList.toArray(cc));
                }
            }

            //Check if there is a valid recipient set
            if (!recipientList.isEmpty()) {
                emailDetails.setFrom(mailFrom);
                emailDetails.setRecipients(Message.RecipientType.TO, recipientList.toArray(addresses));
                emailDetails.setSubject(subject);
                BodyPart messageBodyPart = new MimeBodyPart();
                messageBodyPart.setContent(emailBody, "text/html");

                if (fileAttachment != null && !fileAttachment.equalsIgnoreCase("")) {
                    //Add the attachment
                    MimeBodyPart attachmentBodyPart = new MimeBodyPart();
                    DataSource source = new FileDataSource(fileAttachment);
                    attachmentBodyPart.setDataHandler(new DataHandler(source));
                    attachmentBodyPart.setFileName(fileAttachment);

                    Multipart multipart = new MimeMultipart();
                    multipart.addBodyPart(messageBodyPart);
                    multipart.addBodyPart(attachmentBodyPart);
                    emailDetails.setContent(multipart);
                } else {
                    Multipart multipart = new MimeMultipart();
                    multipart.addBodyPart(messageBodyPart);
                    emailDetails.setContent(multipart);
                }

                mailSender.send(emailDetails);
                return new String[]{"Success", ""};
            } else {
                return new String[]{"Failed", "No recipient"};
            }
        } catch (Exception ex) {
            return new String[]{"Failed", ex.getMessage()};
        }
    }

}

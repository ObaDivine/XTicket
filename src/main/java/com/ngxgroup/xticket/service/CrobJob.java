package com.ngxgroup.xticket.service;

import com.ngxgroup.xticket.model.TicketEscalations;
import com.ngxgroup.xticket.model.TicketStatus;
import com.ngxgroup.xticket.model.Tickets;
import com.ngxgroup.xticket.payload.XTicketPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.ngxgroup.xticket.repository.XTicketRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;

/**
 *
 * @author bokon
 */
@Service
public class CrobJob {

    @Autowired
    GenericService genericService;
    @Autowired
    XTicketRepository xticketRepository;
    @Value("${xticket.host}")
    private String host;
    @Value("${xticket.company.name}")
    private String companyName;
    @Value("${xticket.company.email}")
    private String companyEmail;
    @Value("${xticket.email.escalation.interval}")
    private int escalationInterval;
    @Value("${xticket.slaexpiry.notification}")
    private int slaExpiryNotificationInterval;
    DateTimeFormatter timeDtf = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Scheduled(cron = "${xticket.cron.job}")
    public void setEscalateViolatedSLA() {
        //Ftech all the tickets that are open
        TicketStatus openStatus = xticketRepository.getTicketStatusUsingCode("OPEN");
        List<Tickets> openTickets = xticketRepository.getTicketsByStatus(openStatus);
        if (openTickets != null) {
            for (Tickets t : openTickets) {
                //Check if the SLA expiry is exceeded
                if (LocalDateTime.now().isAfter(t.getSlaExpiry())) {
                    //Fetch emails for escalation
                    String[] escalationEmails = t.getTicketType().getEscalationEmails().split(",");
                    if (escalationEmails.length > 0) {
                        //Check if time to run the next escalation
                        long timeElapsed = Duration.between(t.getEscalatedAt() == null ? LocalDateTime.now() : t.getEscalatedAt().toLocalTime(), LocalDateTime.now()).toMinutes();
                        if (timeElapsed >= escalationInterval && (t.getEscalationIndex() + 1) <= escalationEmails.length) {
                            String carbonCopyEmail = "";
                            //Determine the carbon copy recipient
                            if (t.getEscalationIndex() == 0) {
                                carbonCopyEmail = t.getTicketAgent().getAgent().getEmail();
                            } else {
                                carbonCopyEmail = escalationEmails[t.getEscalationIndex() - 1];
                            }
                            //Escalate the email and push email notification
                            sendEmail(escalationEmails[t.getEscalationIndex()], carbonCopyEmail, t);

                            //Update the escalation index
                            t.setEscalationIndex(t.getEscalationIndex() + 1);
                            xticketRepository.updateTicket(t);

                            //Add to the ticket escalations
                            TicketEscalations newEscalation = new TicketEscalations();
                            newEscalation.setCreatedAt(LocalDateTime.now());
                            newEscalation.setEscalatedTo(escalationEmails[t.getEscalationIndex()]);
                            newEscalation.setSlaExpiresAt(t.getSlaExpiry());
                            newEscalation.setTicket(t);
                            xticketRepository.createTicketEscalation(newEscalation);
                        }
                    }
                    //Update the ticket escalation 
                    if (!t.isEscalated()) {
                        t.setEscalated(true);
                        t.setEscalatedAt(LocalDateTime.now());
                        t.setSlaViolated(true);
                        t.setSlaViolatedAt(LocalDateTime.now());
                        xticketRepository.updateTicket(t);
                    }
                }

                //Notify agents for tickets about to violate SLA
                long timeElapsed = Duration.between(LocalDateTime.now(), t.getSlaExpiry()).toMinutes();
                if ((timeElapsed <= slaExpiryNotificationInterval) && !t.isAgentNotifiedOfExpiry()) {
                    //Send notification email to the agent
                    ticketNearSLAViolationSendEmail(t.getTicketAgent().getAgent().getEmail(), t);
                    t.setAgentNotifiedOfExpiry(true);
                    xticketRepository.updateTicket(t);
                }
            }
        }
    }

    private void sendEmail(String escalationEmail, String carbonCopy, Tickets ticket) {
        XTicketPayload mailPayload = new XTicketPayload();
        mailPayload.setRecipientEmail(escalationEmail);
        mailPayload.setCarbonCopyEmail(carbonCopy);
        mailPayload.setEmailSubject("Ticket SLA Violation Notification");
        String slaTime = timeDtf.format(ticket.getSlaExpiry().toLocalTime());
        String slaDate = ticket.getSlaExpiry().getMonth().toString() + " " + ticket.getSlaExpiry().getDayOfMonth() + ", " + ticket.getSlaExpiry().getYear();
        String message = "<h4>To Whom It May Concern,</h4>\n"
                + "<p>An SLA for <b>" + ticket.getTicketType().getTicketTypeName() + "</b> ticket with an ID <b>" + ticket.getTicketId()
                + "</b> and priority <b>" + ticket.getTicketType().getSla().getPriority() + "</b> has been violated by <b>" + ticket.getTicketAgent().getAgent().getLastName() + ", " + ticket.getTicketAgent().getAgent().getOtherName()
                + "</b> in <b>" + ticket.getTicketType().getServiceUnit().getServiceUnitName() + ".</b></p>"
                + "<p>The ticket expired at <b>" + slaTime + "</b> on <b>" + slaDate + "</b></p>"
                + "<p>To view the ticket details or take action, kindly login into NGX X-Ticket by <a href=\"" + host + "/xticket" + "\">clicking here</a></p>"
                + "<p>For support and enquiries, email: " + companyEmail + ".</p>\n"
                + "<p>Best wishes,</p>"
                + "<p>" + companyName + "</p>";
        mailPayload.setEmailBody(message);
        genericService.sendEmail(mailPayload, escalationEmail);
    }

    private void ticketNearSLAViolationSendEmail(String escalationEmail, Tickets ticket) {
        XTicketPayload mailPayload = new XTicketPayload();
        mailPayload.setRecipientEmail(escalationEmail);
        mailPayload.setEmailSubject("Ticket SLA Violation Notification");
        String slaTime = timeDtf.format(ticket.getSlaExpiry().toLocalTime());
        String slaDate = ticket.getSlaExpiry().getMonth().toString() + " " + ticket.getSlaExpiry().getDayOfMonth() + ", " + ticket.getSlaExpiry().getYear();
        String message = "<h4>Dear " + ticket.getTicketAgent().getAgent().getLastName() + ",</h4>\n"
                + "<p>An SLA for <b>" + ticket.getTicketType().getTicketTypeName() + "</b> ticket with an ID <b>" + ticket.getTicketId()
                + "</b> is about to be violated. The priority is <b>" + ticket.getTicketType().getSla().getPriority() + ".</b></p>"
                + "<p>The ticket is set to expire by <b>" + slaTime + "</b> on <b>" + slaDate + "</b></p>"
                + "<p>To view the ticket details or take action, kindly login to NGX X-Ticket by <a href=\"" + host + "/xticket" + "\">clicking here</a></p>"
                + "<p>For support and enquiries, email: " + companyEmail + ".</p>\n"
                + "<p>Best wishes,</p>"
                + "<p>" + companyName + "</p>";
        mailPayload.setEmailBody(message);
        genericService.sendEmail(mailPayload, escalationEmail);
    }
}

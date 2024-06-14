package com.ngxgroup.xticket.service;

import com.ngxgroup.xticket.model.Tickets;
import com.ngxgroup.xticket.payload.XTicketPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.ngxgroup.xticket.repository.XTicketRepository;
import java.time.Duration;
import java.time.LocalDate;
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
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Scheduled(cron = "${xticket.cron.job}")
    public void setExpiryPolicies() {
        //Ftech all the tickets that are open
        List<Tickets> openTickets = xticketRepository.getOpenTickets();
        if (openTickets != null) {
            for (Tickets t : openTickets) {
                //Check if the SLA expiry is exceeded
                if (LocalDateTime.now().isAfter(t.getSlaExpiry())) {
                    //Fetch emails for escalation
                    String[] escalationEmails = t.getTicketType().getEscalationEmails().split(",");
                    if (escalationEmails.length > 0) {
                        //Check if time to run the next escalation
                        long timeElapsed = Duration.between(t.getEscalatedAt() == null ? LocalDateTime.now() : t.getEscalatedAt().toLocalTime(), LocalDateTime.now().toLocalTime()).toMinutes();
                        if (timeElapsed >= escalationInterval) {
                            //Check if maximum escalation is reached
                            if ((t.getEscalationIndex() + 1) <= escalationEmails.length) {
                                //Escalate the email and push email notification
                                sendEmail(escalationEmails[t.getEscalationIndex()], t);

                                //Update the escalation index
                                t.setEscalationIndex(t.getEscalationIndex() + 1);
                                xticketRepository.updateTicket(t);
                            }
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
            }
        }
    }

    private void sendEmail(String escalationEmail, Tickets ticket) {
        XTicketPayload mailPayload = new XTicketPayload();
        mailPayload.setRecipientEmail(escalationEmail);
        mailPayload.setSubject("Ticket SLA Violation Notification");
        String slaExpiry = dtf.format(ticket.getSlaExpiry());
        String message = "<h4>To Whom It May Concern</h4>\n"
                + "<p>A ticket SLA has been violated as follows;</p>\n"
                + "<p>Date Created: " + LocalDate.now().toString() + "</p>\n"
                + "<p>Initiated By: " + ticket.getCreatedBy().getLastName() + ", " + ticket.getCreatedBy().getOtherName() + "</p>\n"
                + "<p>Ticket ID: " + ticket.getTicketId() + "</p>\n"
                + "<p>Ticket Group: " + ticket.getTicketGroup().getTicketGroupName() + "</p>\n"
                + "<p>Ticket Type: " + ticket.getTicketType().getTicketTypeName() + "</p>\n"
                + "<p>Ticket Priority: " + ticket.getTicketType().getSla().getTicketSlaName() + "</p>\n"
                + "<p>SLA Expiry: " + slaExpiry + "</p>\n"
                + "<p>To view the ticket details or take action <a href=\"" + host + "/xticket" + "\">click here</a></p>"
                + "<p>For support and enquiries, email: " + companyEmail + ".</p>\n"
                + "<p>Best wishes,</p>"
                + "<p>" + companyName + "</p>";
        mailPayload.setEmailBody(message);
        genericService.sendEmail(mailPayload, escalationEmail);
    }
}

package com.ngxgroup.xticket.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 *
 * @author bokon
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "automated_ticket")
public class AutomatedTicket implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "created_by")
    private String createdBy;
    @Column(name = "service_requester")
    private String serviceRequester;
    @Column(name = "service_provider")
    private String serviceProvider;
    @Column(name = "subject", length = 5000)
    private String subject;
    @Column(name = "message", length = 5000)
    private String message;
    @Column(name = "frequency")
    private String frequency;
    @Column(name = "start_date")
    private LocalDate startDate;
    @Column(name = "end_date")
    private LocalDate endDate;
    @Column(name = "schedule", length = 5000)
    private String schedule;
    @Column(name = "escalation_emails", length = 5000)
    private String escalationEmails;
    @ManyToOne
    private TicketType ticketType;
    @Column(name = "status")
    private String status;
    @Column(name = "next_run_index")
    private int nextRunIndex = 0;
}

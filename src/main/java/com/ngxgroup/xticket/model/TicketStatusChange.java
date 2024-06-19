package com.ngxgroup.xticket.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 *
 * @author briano
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "ticket_status_change")
public class TicketStatusChange implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @Column(name = "changed_at")
    private LocalDateTime changedAt;
    @ManyToOne
    private AppUser changedBy;
    @ManyToOne
    private Tickets ticket;
    @ManyToOne
    private TicketAgent ticketAgent;
    @Column(name = "reason_for_change")
    private String reasonForChange;
    @ManyToOne
    private TicketStatus ticketStatus;
    @Column(name = "priority")
    private String priority;
    @Column(name = "sla")
    private String sla;
    @Column(name = "sla_expiry")
    private LocalDateTime slaExpiry;
}

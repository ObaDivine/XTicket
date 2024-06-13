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
@Table(name = "ticket_reopened")
public class TicketReopened implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @Column(name = "reopened_at")
    private LocalDateTime reopenedAt;
    @ManyToOne
    private AppUser reopenedBy;
    @ManyToOne
    private Tickets ticket;
    @ManyToOne
    private TicketAgent ticketAgent;
    @Column(name = "reason_for_reopening")
    private String reasonForReopening;
    @Column(name = "closed_at")
    private LocalDateTime closedAt;
    @ManyToOne
    private AppUser closedBy;
    @Column(name = "priority")
    private String priority;
    @Column(name = "sla")
    private String sla;
    @Column(name = "sla_expiry")
    private LocalDateTime slaExpiry;
}

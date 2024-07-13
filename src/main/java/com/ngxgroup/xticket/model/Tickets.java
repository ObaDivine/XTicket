package com.ngxgroup.xticket.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
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
@Table(name = "tickets")
public class Tickets implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @OneToOne
    private AppUser createdBy;
    @Column(name = "closed_at")
    private LocalDateTime closedAt;
    @OneToOne
    private AppUser closedBy;
    @ManyToOne
    private TicketAgent ticketAgent;
    @Column(name = "ticket_id")
    private String ticketId;
    @Column(name = "ticket_pause")
    private LocalDateTime ticketPause;
    @Column(name = "ticket_locked")
    private boolean ticketLocked = false;
    @Column(name = "ticket_reopened")
    private boolean ticketReopen = false;
    @ManyToOne
    private TicketReopened ticketReopened;
    @Column(name = "ticket_reassigned")
    private boolean ticketReassign = false;
    @ManyToOne
    private TicketReassign ticketReassigned;
    @Column(name = "internal")
    private boolean internal = true;
    @ManyToOne
    private TicketGroup ticketGroup;
    @ManyToOne
    private TicketType ticketType;
    @ManyToOne
    private TicketStatus ticketStatus;
    @Column(name = "escalated")
    private boolean escalated = false;
    @Column(name = "escalated_at")
    private LocalDateTime escalatedAt;
    @Column(name = "escalation_index")
    private int escalationIndex = 0;
    @Column(name = "ticket_source")
    private String ticketSource;
    @Column(name = "priority")
    private String priority;
    @Column(name = "sla")
    private String sla;
    @Column(name = "sla_expiry")
    private LocalDateTime slaExpiry;
    @Column(name = "sla_violated")
    private boolean slaViolated = false;
    @Column(name = "sla_violated_at")
    private LocalDateTime slaViolatedAt;
    @ManyToOne
    private TicketAgent ticketAgentViolated;
    @Column(name = "subject", length = 5000)
    private String subject;
    @Column(name = "message", length = 5000)
    private String message;
    @Column(name = "attached_file")
    private boolean attachedFile = false;
    @Column(name = "file_index")
    private int fileIndex = 1;
    @ManyToOne
    private Entities entity;
    @Column(name = "agent_notified_of_expiry")
    private boolean agentNotifiedOfExpiry = false;
    @Column(name = "rating")
    private int rating = 0;
    @Column(name = "rating_comment", length = 5000)
    private String ratingComment;
}

package com.ngxgroup.xticket.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
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
    @ManyToOne
    private AppUser createdBy;
    @Column(name = "closed_at")
    private LocalDateTime closedAt;
    @ManyToOne
    private AppUser closedBy;
    @Column(name = "ticket_id")
    private String ticketId;
    @Column(name = "ticket_open")
    private boolean ticketOpen;
    @Column(name = "ticket_reopened")
    private boolean ticketReopened;
    @Column(name = "internal")
    private boolean internal;
    @ManyToOne
    private TicketGroup ticketGroup;
    @ManyToOne
    private TicketType ticketType;
    @Column(name = "escalated")
    private boolean escalated;
}

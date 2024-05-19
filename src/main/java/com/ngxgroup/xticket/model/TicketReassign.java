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
@Table(name = "ticket_reassign")
public class TicketReassign implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @Column(name = "reassigned_at")
    private LocalDateTime reassignedAt;
    @ManyToOne
    private AppUser reassignedBy;
    @ManyToOne
    private AppUser reassignedTo;
    @ManyToOne
    private Tickets ticket;
    @Column(name = "reason_for_reassigning")
    private String reasonForReassigning;
    @Column(name = "initial_sla")
    private LocalDateTime initialSla;
    @Column(name = "new_sla")
    private LocalDateTime newSla;
}

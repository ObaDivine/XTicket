package com.ngxgroup.xticket.model;

import java.io.Serializable;
import java.time.LocalDate;
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
@Table(name = "ticket-reopened")
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
    @Column(name = "reason_for_reopening")
    private String reasonForReopening;
    @Column(name = "closed_at")
    private LocalDateTime closedAt;
    @ManyToOne
    private AppUser closedBy;
}

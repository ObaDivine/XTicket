package com.ngxgroup.xticket.model;

import java.io.Serializable;
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
@Table(name = "ticket_type")
public class TicketType implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "created_by")
    private String createdBy;
    @ManyToOne
    private TicketGroup ticketGroup;
    @Column(name = "internal")
    private boolean internal;
    @Column(name = "ticket_type_code")
    private String ticketTypeCode;
    @Column(name = "ticket_type_name")
    private String ticketTypeName;
    @ManyToOne
    private TicketSla sla;
    @Column(name = "escalation_emails", length = 5000)
    private String escalationEmails;
    @Column(name = "email_escalation_index")
    private int emailEscalationIndex;
    @Column(name = "status")
    private String status;
    @Column(name = "require_change_request_form")
    private boolean requireChangeRequestForm;
    @Column(name = "require_service_request_form")
    private boolean requireServiceRequestForm;
    @ManyToOne
    private ServiceUnit serviceUnit;
}

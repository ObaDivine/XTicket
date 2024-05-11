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
@Table(name = "ticket_type")
public class TicketType implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @ManyToOne
    private AppUser createdBy;
    @ManyToOne
    private TicketGroup ticketGroup;
    @Column(name = "internal")
    private boolean internal;
    @Column(name = "ticket_type_code")
    private String ticketTypeCode;
    @Column(name = "ticket_type_name")
    private String ticketTypeName;
    @Column(name = "sla_mins")
    private int slaMins;
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

}

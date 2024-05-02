package com.ngxgroup.xticket.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 *
 * @author Brian A. Okon okon.brian@gmail.com
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "audit_log")
public class AuditLog implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Long id;
    @Column(name = "Username")
    private String username;
    @Column(name = "AuditAction")
    private String auditAction;
    @Column(name = "AuditCategory")
    private String auditCategory;
    @Column(name = "AuditClass")
    private String auditClass;
    @Column(name = "RefNo")
    private String refNo;
    @Column(name = "OldValue", length = 5000)
    private String oldValue;
    @Column(name = "NewValue", length = 5000)
    private String newValue;
    @Column(name = "CreatedAt")
    private LocalDateTime createdAt;
}

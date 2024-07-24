package com.ngxgroup.xticket.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
    @Column(name = "username")
    private String username;
    @Column(name = "audit_action")
    private String auditAction;
    @Column(name = "audit_category")
    private String auditCategory;
    @Column(name = "audit_class")
    private String auditClass;
    @Column(name = "ref_no")
    private String refNo;
    @Column(name = "old_value", length = 5000)
    private String oldValue;
    @Column(name = "new_value", length = 5000)
    private String newValue;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}

package com.ngxgroup.xticket.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import javax.persistence.Basic;
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
 * @author bokon
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "app_users")
public class AppUser implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Long id;
    @Column(name = "email")
    private String email;
    @Column(name = "created_by")
    private String createdBy;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "updated_by")
    private String updatedBy;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    @Column(name = "locked")
    private boolean isLocked = false;
    @Column(name = "expired")
    private boolean isExpired = false;
    @Column(name = "enabled")
    private boolean isEnabled = true;
    @Column(name = "reset_time")
    private LocalDateTime resetTime;
    @Column(name = "last_login")
    private LocalDate lastLogin;
    @ManyToOne
    private RoleGroups role;
    @Column(name = "login_fail_count")
    private int loginFailCount = 0;
    @Column(name = "two_factor_secretkey")
    private String twoFactorSecretKey;
    @ManyToOne
    private Tickets company;
    @ManyToOne
    private TicketComment division;
    @ManyToOne
    private TicketUpload department;
    @Column(name = "access_level")
    private int accessLevel = 1;
    @Column(name = "name")
    private String name;
    @Column(name = "user_type")
    private String userType = "USER";
    @Column(name = "user_id")
    private String userId;
    @Column(name = "policy_champion")
    private boolean policyChampion = false;
}

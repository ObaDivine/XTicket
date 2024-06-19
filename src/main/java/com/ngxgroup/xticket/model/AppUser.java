package com.ngxgroup.xticket.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import jakarta.persistence.Basic;
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
    @Column(name = "last_name")
    private String lastName;
    @Column(name = "other_name")
    private String otherName;
    @Column(name = "email")
    private String email;
    @Column(name = "mobile_number")
    private String mobileNumber;
    @Column(name = "gender")
    private String gender;
    @Column(name = "created_by")
    private String createdBy;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "updated_by")
    private String updatedBy;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    @Column(name = "password")
    private String password;
    @Column(name = "locked")
    private boolean isLocked = false;
    @Column(name = "activated")
    private boolean isActivated = false;
    @Column(name = "reset_time")
    private LocalDateTime resetTime;
    @Column(name = "last_login")
    private LocalDateTime lastLogin;
    @ManyToOne
    private RoleGroups role;
    @Column(name = "login_fail_count")
    private int loginFailCount = 0;
    @Column(name = "internal")
    private boolean internal = false;
    @Column(name = "agent")
    private boolean agent = false;
    @Column(name = "online")
    private boolean online = false;
    @Column(name = "password_change_date")
    private LocalDate passwordChangeDate;
    @Column(name = "activation_id")
    private String activationId;
    @ManyToOne
    private Entities entity;
}

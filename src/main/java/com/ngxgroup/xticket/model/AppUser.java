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
    private LocalDate lastLogin;
    @ManyToOne
    private RoleGroups role;
    @Column(name = "login_fail_count")
    private int loginFailCount = 0;
    @Column(name = "internal")
    private boolean internal = false;
    @Column(name = "technician")
    private boolean technician = false;
    @Column(name = "password_change_date")
    private LocalDate passwordChangeDate;
    @Column(name = "activation_id")
    private String activationId;
}

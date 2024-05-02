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
@Table(name = "policy")
public class Policy implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "created_by")
    private String createdBy;
    @Column(name = "last_review")
    private LocalDate lastReview;
    @Column(name = "policy_name")
    private String policyName;
    @Column(name = "policy_code")
    private String policyCode;
    @Column(name = "policy_description", length = 500)
    private String policyDescription;
    @Column(name = "policy_author")
    private String policyAuthor;
    @Column(name = "policy_document_id")
    private String policyDocumentId;
    @Column(name = "policy_document_ext")
    private String policyDocumentExt;
    @Column(name = "under_review")
    private boolean underReview = false;
    @Column(name = "expiry_date")
    private LocalDate expiryDate;
    @Column(name = "expired")
    private boolean expired = false;
    @ManyToOne
    private PolicyType policyType;
    @ManyToOne
    private Company company;
    @ManyToOne
    private Division division;
    @ManyToOne
    private Department department;
    @Column(name = "file_size")
    private String fileSize;
    @Column(name = "access_level")
    private int accessLevel = 1;
    @Column(name = "must_read")
    private boolean mustRead = false;
}

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
 * @author bokon
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "policy_review")
public class PolicyReview implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @ManyToOne
    private AppUser appUser;
    @ManyToOne
    private Policy policy;
    @Column(name = "reviewed_by")
    private String reviewedBy;
    @Column(name = "reviewed_at")
    private String reviewedAt;
    @Column(name = "review_comment", length = 1000)
    private String reviewComment;
}

package com.ngxgroup.xticket.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;
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
@Table(name = "email_temp")
public class EmailTemp implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "email")
    private String email = "";
    @Column(name = "message", length = 2000)
    private String message;
    @Column(name = "subject")
    private String subject;
    @Column(name = "status")
    private String status;
    @Column(name = "error", length = 1000)
    private String error;
    @Column(name = "try_count")
    private int tryCount;
    @Column(name = "carbon_copy")
    private String carbonCopy;
    @Column(name = "file_attachment")
    private String fileAttachment;
}

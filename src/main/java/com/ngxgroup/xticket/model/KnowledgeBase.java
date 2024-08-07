package com.ngxgroup.xticket.model;

import java.io.Serializable;
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
 * @author briano
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "knowledge_base")
public class KnowledgeBase implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Long id;
    @Column(name = "tag")
    private String tag;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "created_by")
    private String createdBy;
    @Column(name = "header")
    private String header;
    @Column(name = "body", length = 10000)
    private String body;
    @Column(name = "video_link")
    private String videoLink;
    @ManyToOne
    private KnowledgeBaseCategory knowledgeBaseCategory;
    @Column(name = "popular_article")
    private boolean popularArticle;
    @Column(name = "latest_article")
    private boolean latestArticle;
}

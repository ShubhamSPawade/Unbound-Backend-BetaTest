package com.unbound.backend.entity;

import lombok.*;
import jakarta.persistence.*;

@Entity
@Table(name = "event")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer eid;

    @ManyToOne
    @JoinColumn(name = "cid", referencedColumnName = "cid", nullable = false)
    private College college;

    @ManyToOne
    @JoinColumn(name = "fid", referencedColumnName = "fid")
    private Fest fest;

    @Column(nullable = false)
    private String ename;

    @Column(columnDefinition = "TEXT")
    private String edescription;

    @Column(nullable = false)
    private String eventDate;

    @Column(nullable = false)
    private Integer fees = 0;

    private String location;

    @Column(nullable = false)
    private Integer capacity;

    @Column(nullable = false)
    private Boolean teamIsAllowed = false;

    @Column(length = 100)
    private String category; // e.g., Technical, Cultural, Sports, etc.

    @Column(length = 20)
    private String mode; // Online, Offline

    @Column(length = 255)
    private String posterUrl; // URL or path to event poster/banner

    @Column(length = 255)
    private String posterThumbnailUrl; // URL or path to event poster thumbnail

    @Column(nullable = false)
    private boolean posterApproved = false;
} 
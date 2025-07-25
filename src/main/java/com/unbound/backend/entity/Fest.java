package com.unbound.backend.entity;

import lombok.*;
import jakarta.persistence.*;

@Entity
@Table(name = "fest")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Fest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer fid;

    @ManyToOne
    @JoinColumn(name = "cid", referencedColumnName = "cid", nullable = false)
    private College college;

    @Column(nullable = false)
    private String fname;

    @Column(columnDefinition = "TEXT")
    private String fdescription;

    @Column(nullable = false)
    private String startDate;

    @Column(nullable = false)
    private String endDate;
} 
package com.unbound.backend.entity;

import lombok.*;
import jakarta.persistence.*;

@Entity
@Table(name = "college")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class College {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer cid;

    @OneToOne
    @JoinColumn(name = "uid", referencedColumnName = "uid", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private String cname;

    @Column(columnDefinition = "TEXT")
    private String cdescription;

    @Column(columnDefinition = "TEXT")
    private String address;

    private String contactEmail;
} 
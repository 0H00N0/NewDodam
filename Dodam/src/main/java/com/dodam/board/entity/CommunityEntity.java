package com.dodam.board.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "community")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CommunityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "community_seq")
    @SequenceGenerator(
        name = "community_seq",
        sequenceName = "COMMUNITY_SEQ",
        allocationSize = 1
    )
    @Column(name = "ID")
    private Long id;

    @Column(name = "BSUB", nullable = false, length = 255)
    private String bsub;

    @Column(name = "BCONTENT", nullable = false, length = 4000)
    private String bcontent;

    @Column(name = "MNIC", nullable = false, length = 255)
    private String mnic;
}
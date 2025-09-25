package com.dodam.member.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "memtype")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MemtypeEntity {

    @Id
    @Column(name = "mtnum")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long mtnum;

    @Column(name = "mtcode", nullable = false)   // 0/1/2/3
    private Integer mtcode;

    @Column(name = "mtname", nullable = false, length = 50) // "일반","SuperAdmin","Staff","Deliveryman"
    private String mtname;
}

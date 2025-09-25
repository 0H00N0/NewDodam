package com.dodam.discount.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "discount")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Discount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "disnum")
    private Long disNum;

    @Column(name = "dislevel", nullable = false)
    private Integer disLevel;

    @Column(name = "disvalue", nullable = false)
    private Integer disValue;
}
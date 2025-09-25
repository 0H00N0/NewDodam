package com.dodam.reservation.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "reservation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "resernum")
    private Long reserNum;

    @Column(name = "reserlevel", nullable = false)
    private Integer reserLevel;
    
    @Column(name="resername",nullable = false)
    private String reserName;
    
}
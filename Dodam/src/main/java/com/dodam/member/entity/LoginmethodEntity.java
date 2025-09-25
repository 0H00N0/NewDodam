package com.dodam.member.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "loginmethod")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LoginmethodEntity {

    @Id
    @Column(name = "lmnum")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long lmnum;

    @Column(name = "lmtype", nullable = false, length = 50)
    private String lmtype; // 예) "LOCAL", "KAKAO", "NAVER", "GOOGLE"
}

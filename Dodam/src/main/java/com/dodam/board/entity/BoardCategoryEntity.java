package com.dodam.board.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "boardcategory") // 게시판 카테고리 테이블
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoardCategoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bcanum", nullable = false)
    private Long bcanum; // 카테고리 번호 (PK)

    @Column(name = "bcaname", length = 255)
    private String bcaname; // 카테고리 이름 (공지사항, 자유게시판 등)
}
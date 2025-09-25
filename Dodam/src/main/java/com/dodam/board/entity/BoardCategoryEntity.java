package com.dodam.board.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "BoardCategory") // 'BoardCategory' 테이블과 매핑
public class BoardCategoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bcnum")
    private Long bcnum; // 카테고리 번호 (Primary Key)

    @Column(name = "bcname", length = 255)
    private String bcname; // 카테고리 내용

    // --- 관계 매핑 (양방향) ---
    
    // BoardCategory(1) : Board(N) -> 일대다 관계
    // 'mappedBy'는 BoardEntity에 있는 'boardCategory' 필드를 가리킵니다.
    @OneToMany(mappedBy = "boardCategory", cascade = CascadeType.ALL)
    private List<BoardEntity> boards = new ArrayList<>();
}
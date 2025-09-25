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
@Table(name = "BoardState") // 데이터베이스의 'BoardState' 테이블과 매핑
public class BoardStateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bsnum")
    private Long bsnum; // 게시판 상태번호 (Primary Key)

    @Column(name = "bsname", length = 255)
    private String bsname; // 상태내용

    // --- 관계 매핑 (양방향) ---
    
    // BoardState(1) : Board(N) -> 일대다 관계
    // 'mappedBy'는 BoardEntity에 있는 'boardState' 필드를 가리키며, 이 관계의 주인이 아님을 명시합니다.
    @OneToMany(mappedBy = "boardState", cascade = CascadeType.ALL)
    private List<BoardEntity> boards = new ArrayList<>();
}
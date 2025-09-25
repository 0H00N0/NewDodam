package com.dodam.board.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor // JPA는 기본 생성자가 필요합니다.
@Table(name = "Board") // 데이터베이스의 'Board' 테이블과 매핑
public class BoardEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bnum")
    private Long bnum; // 글번호 (Primary Key)

    @Column(name = "mnum", nullable = false)
    private Long mnum; // 회원번호

    @Column(name = "tnum")
    private Long tnum; // 타입번호

    @Column(name = "btitle", length = 255)
    private String btitle; // 제목

    @Lob // 내용처럼 긴 텍스트는 @Lob으로 매핑합니다.
    @Column(name = "bcontent")
    private String bcontent; // 내용

    @CreationTimestamp // 엔티티 생성 시 자동으로 현재 시간이 기록됩니다.
    @Column(name = "bdate")
    private LocalDateTime bdate; // 작성일

    @UpdateTimestamp // 엔티티 수정 시 자동으로 현재 시간이 기록됩니다.
    @Column(name = "bedate")
    private LocalDateTime bedate; // 수정일
    
    @Column(name = "mid", nullable = false)
    private String mid; // 회원ID

    @Column(name = "mnic", nullable = false)
    private String mnic; // 회원닉네임
	
    @Column(name = "boardcode")
    private String code;
    // --- 관계 매핑 ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "boardid", nullable = false)  // ← referencedColumnName 제거!
    private BoardEntity board;
    
    // Board(N) : BoardCategory(1) -> 다대일 관계
    @ManyToOne(fetch = FetchType.LAZY) // 지연 로딩으로 성능 최적화
    @JoinColumn(name = "bcnum") // 외래 키(FK) 컬럼 지정
    private BoardCategoryEntity boardCategory;

    // Board(N) : BoardState(1) -> 다대일 관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bsnum") // 외래 키(FK) 컬럼 지정
    private BoardStateEntity boardState;
    
}
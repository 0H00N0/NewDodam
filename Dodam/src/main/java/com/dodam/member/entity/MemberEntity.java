package com.dodam.member.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "member")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MemberEntity {

    @Id
    @Column(name = "mnum")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long mnum;

    // FK: 가입방법
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lmnum", referencedColumnName = "lmnum", nullable = false) // ★lmnum로 수정
    private LoginmethodEntity loginmethod;

    // FK: 회원타입(0/1/2/3)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mtnum", referencedColumnName = "mtnum", nullable = false)
    private MemtypeEntity memtype;

    @Column(name = "mid",   nullable = false, length = 100 , unique = true)  private String mid;    // ID
    @Column(name = "mpw",   nullable = false, length = 255)  private String mpw;    // (개발용)평문 또는 BCrypt
    @Column(name = "mname", nullable = false, length = 100)  private String mname;  // 이름
    @Column(name = "memail", length = 255)                   private String memail; // 이메일 (NULL 가능)
    @Column(name = "mtel",  nullable = false, length = 30)   private String mtel;   // 전화
    @Column(name = "maddr", nullable = false, length = 255)  private String maddr;  // 상세주소
    @Column(name = "mpost", nullable = false)                private Long mpost;    // 우편번호
    @Column(name = "mbirth", nullable = true)            	 private LocalDate mbirth; // 생일
    @Column(name = "mreg", nullable = true)					 private LocalDate mreg; // 구독시작일
    @Column(name = "mnic",   length = 100)                   private String mnic;   // 닉네임 (NULL)

    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }
    
    @PreUpdate
    void onUpdate() { this.updatedAt = LocalDateTime.now(); }
    
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL)
    private List<ChildEntity> children;
}

package com.dodam.member.entity;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

	@Entity
	@Table(name = "child")
	@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChildEntity {

	    @Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    @Column(name = "chnum")
	    private Long chnum; // 자녀번호 (PK)

	    @ManyToOne(fetch = FetchType.LAZY) // 여러 자녀가 한 명의 회원에 속할 수 있음
	    @JoinColumn(name = "mnum", nullable = false)
	    private MemberEntity member; // 회원번호 (FK)

	    @Column(name = "chname", nullable = false, length = 50)
	    private String chname; // 자녀 성함

	    @Column(name = "chbirth", nullable = false)
	    private LocalDate chbirth; // 자녀 생년월일
	}

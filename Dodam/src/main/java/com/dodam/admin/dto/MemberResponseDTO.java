package com.dodam.admin.dto;

import com.dodam.member.entity.MemberEntity;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class MemberResponseDTO {

    private Long mnum;
    private String mid;
    private String mname;
    private String mnic;
    private String memail;
    private String mtel;
    private String maddr;
    private Long mpost;
    private LocalDate mbirth;
    private LocalDate mreg;
    private String roleName; // "일반", "SuperAdmin" 등
    private String lmtype;   // "LOCAL", "KAKAO" 등
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Entity를 DTO로 변환하는 정적 팩토리 메서드
    public static MemberResponseDTO fromEntity(MemberEntity entity) {
        return MemberResponseDTO.builder()
                .mnum(entity.getMnum())
                .mid(entity.getMid())
                .mname(entity.getMname())
                .mnic(entity.getMnic())
                .memail(entity.getMemail())
                .mtel(entity.getMtel())
                .maddr(entity.getMaddr())
                .mpost(entity.getMpost())
                .mbirth(entity.getMbirth())
                .mreg(entity.getMreg())
                .roleName(entity.getMemtype() != null ? entity.getMemtype().getMtname() : null)
                .lmtype(entity.getLoginmethod() != null ? entity.getLoginmethod().getLmtype() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
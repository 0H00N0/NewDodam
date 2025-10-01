package com.dodam.admin.dto;

import com.dodam.member.entity.MemberEntity;
import com.dodam.member.entity.MemberEntity.MemStatus;

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
    private String roleName;   // "일반", "SuperAdmin" 등
    private String lmtype;     // "LOCAL", "KAKAO" 등
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ✅ 신규 추가 필드
    private MemStatus memstatus;     // ACTIVE / DELETED
    private LocalDateTime deletedAt;     // 탈퇴일
    private String deletedReason;    // 탈퇴 사유

    // Entity -> DTO 변환
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
                // ✅ 추가 매핑
                .memstatus(entity.getMemstatus())
                .deletedAt(entity.getDeletedAt())
                .deletedReason(entity.getDeletedReason())
                .build();
    }
}

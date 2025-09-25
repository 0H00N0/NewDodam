// src/main/java/com/dodam/delivery/service/DeliveryService.java
package com.dodam.delivery.service;


import com.dodam.delivery.dto.DeliveryDTO.AreaDTO;
import com.dodam.delivery.dto.DeliveryDTO.AssignmentDTO;
import com.dodam.delivery.dto.DeliveryDTO.ChargesDTO;
import com.dodam.delivery.dto.DeliveryDTO.CustomerDTO;
import com.dodam.delivery.dto.DeliveryDTO.DeliveryMeDTO;
import com.dodam.delivery.dto.DeliveryDTO.MapPointDTO;
import com.dodam.delivery.dto.DeliveryDTO.OperationResult;
import com.dodam.delivery.dto.DeliveryDTO.PerformanceDTO;
import com.dodam.delivery.dto.DeliveryDTO.ProductCheckDTO;
import com.dodam.delivery.dto.DeliveryDTO.ReturnCreateDTO;
import com.dodam.delivery.entity.DeliverymanEntity;
import com.dodam.delivery.repository.DeliverymanRepository;
import com.dodam.member.entity.MemberEntity;
import com.dodam.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final MemberRepository memberRepository;
    private final DeliverymanRepository deliverymanRepository;

    /** 세션 인증 필터(SessionAuthFilter)가 principal로 mid를 넣으므로, mid로 회원 로드 */
    private MemberEntity currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new AccessDeniedException("NO_SESSION");
        }
        String mid = String.valueOf(auth.getPrincipal());
        return memberRepository.findByMid(mid)
                .orElseThrow(() -> new AccessDeniedException("NO_SESSION"));
    }

    /** memtype.mtcode == 3 (배송기사)만 허용 */
    private void ensureDeliveryman(MemberEntity me) {
        Integer code = (me.getMemtype() != null) ? me.getMemtype().getMtcode() : null;
        if (code == null || code != 3) throw new AccessDeniedException("DELIVERYMAN_ONLY");
    }

    public DeliveryMeDTO me() {
        MemberEntity me = currentUser(); ensureDeliveryman(me);
        DeliverymanEntity d = deliverymanRepository.findByMember_Mid(me.getMid())
                .orElseThrow(() -> new AccessDeniedException("NOT_REGISTERED_DELIVERYMAN"));
        Integer remain = (d.getDayoff() != null) ? d.getDayoff() : 0;
        return new DeliveryMeDTO(d.getDelnum(), me.getMnum(), me.getMname(), d.getLocation(), remain);
    }

    public List<AssignmentDTO> today() {
        MemberEntity me = currentUser(); ensureDeliveryman(me);
        // TODO: 실제 당일 배정 데이터로 교체
        return List.of(
            new AssignmentDTO(1L,"ASSIGNED","서울 강남구 테헤란로 123","홍길동","010-1111-2222","10:30"),
            new AssignmentDTO(2L,"IN_TRANSIT","서울 서초구 반포대로 45","김철수","010-3333-4444","11:20")
        );
    }

    public List<AreaDTO> areas() {
        MemberEntity me = currentUser(); ensureDeliveryman(me);
        // TODO: 기사 담당 구역 조회로 교체
        return List.of(new AreaDTO(1L,"강남구"), new AreaDTO(2L,"서초구"));
    }

    public List<MapPointDTO> mapPoints() {
        MemberEntity me = currentUser(); ensureDeliveryman(me);
        // TODO: 당일 경로/좌표로 교체
        return List.of(new MapPointDTO(37.4979,127.0276,"강남역"));
    }

    public List<CustomerDTO> customers(String q) {
        MemberEntity me = currentUser(); ensureDeliveryman(me);
        // TODO: 검색 조건(q) 반영한 고객/주문 배송지 조회
        return List.of();
    }

    public ProductCheckDTO productCheck(String key) {
        MemberEntity me = currentUser(); ensureDeliveryman(me);
        // TODO: key(바코드/주문번호/상품ID) 검증 로직
        return new ProductCheckDTO(0L,"샘플 상품","A", true, "확인되었습니다");
    }

    public OperationResult createReturn(ReturnCreateDTO dto) {
        MemberEntity me = currentUser(); ensureDeliveryman(me);
        // TODO: 반납 생성/상태 변경
        return new OperationResult(true, "반납이 등록되었습니다.");
    }

    public PerformanceDTO performance(java.time.LocalDate from, java.time.LocalDate to) {
        MemberEntity me = currentUser(); ensureDeliveryman(me);
        // TODO: 기간별 실적 집계
        return new PerformanceDTO(0,0,0,0);
    }

    public ChargesDTO charges(java.time.LocalDate from, java.time.LocalDate to) {
        MemberEntity me = currentUser(); ensureDeliveryman(me);
        // TODO: (완료건수 × deliveryman.delcost) + 보너스 정책
        BigDecimal fee = BigDecimal.ZERO;
        BigDecimal bonus = BigDecimal.ZERO;
        return new ChargesDTO(0, fee, bonus, fee.add(bonus));
    }
}

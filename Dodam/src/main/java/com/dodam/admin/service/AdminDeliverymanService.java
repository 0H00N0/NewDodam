package com.dodam.admin.service;

import com.dodam.admin.dto.DeliverymanRequestDTO;
import com.dodam.admin.dto.DeliverymanResponseDTO;
import com.dodam.delivery.entity.DeliverymanEntity;
import com.dodam.delivery.repository.DeliverymanRepository;
import com.dodam.member.entity.MemberEntity;
import com.dodam.member.repository.MemberRepository;
import com.dodam.product.entity.ProductEntity;
import com.dodam.product.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminDeliverymanService {

	private final DeliverymanRepository deliverymanRepository;
	private final ProductRepository productRepository;
	private final MemberRepository memberRepository;

	public DeliverymanResponseDTO create(DeliverymanRequestDTO dto) {
		ProductEntity product = productRepository.findById(dto.getPronum())
				.orElseThrow(() -> new EntityNotFoundException("상품(pronum) 없음: " + dto.getPronum()));
		MemberEntity member = memberRepository.findById(dto.getMnum())
				.orElseThrow(() -> new EntityNotFoundException("회원(mnum) 없음: " + dto.getMnum()));

		// (권장) 배송기사 권한 검증: memtype.mtcode == 3
		if (member.getMemtype() == null || member.getMemtype().getMtcode() != 3) {
			throw new IllegalStateException("해당 회원은 배송기사 권한이 아닙니다(memtype!=3).");
		}

		DeliverymanEntity e = DeliverymanEntity.builder().product(product).member(member).dayoff(dto.getDayoff())
				.delcost(dto.getDelcost()).location(dto.getLocation()).build();

		return DeliverymanResponseDTO.fromEntity(deliverymanRepository.save(e));
	}

	@Transactional(readOnly = true)
	public List<DeliverymanResponseDTO> findAll() {
		return deliverymanRepository.findAll().stream().map(DeliverymanResponseDTO::fromEntity)
				.collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public DeliverymanResponseDTO findById(Long delnum) {
		DeliverymanEntity e = deliverymanRepository.findById(delnum)
				.orElseThrow(() -> new EntityNotFoundException("배송기사 없음: " + delnum));
		return DeliverymanResponseDTO.fromEntity(e);
	}

	public DeliverymanResponseDTO update(Long delnum, DeliverymanRequestDTO dto) {
		DeliverymanEntity e = deliverymanRepository.findById(delnum)
				.orElseThrow(() -> new EntityNotFoundException("배송기사 없음: " + delnum));

		if (dto.getPronum() != null) {
			ProductEntity product = productRepository.findById(dto.getPronum())
					.orElseThrow(() -> new EntityNotFoundException("상품(pronum) 없음: " + dto.getPronum()));
			e.setProduct(product);
		}
		if (dto.getMnum() != null) {
			MemberEntity member = memberRepository.findById(dto.getMnum())
					.orElseThrow(() -> new EntityNotFoundException("회원(mnum) 없음: " + dto.getMnum()));
			if (member.getMemtype() == null || member.getMemtype().getMtcode() != 3) {
				throw new IllegalStateException("해당 회원은 배송기사 권한이 아닙니다(memtype!=3).");
			}
			e.setMember(member);
		}

		e.setDayoff(dto.getDayoff());
		e.setDelcost(dto.getDelcost());
		e.setLocation(dto.getLocation());

		return DeliverymanResponseDTO.fromEntity(e);
	}

	public void delete(Long delnum) {
		if (!deliverymanRepository.existsById(delnum)) {
			throw new EntityNotFoundException("배송기사 없음: " + delnum);
		}
		deliverymanRepository.deleteById(delnum);
	}
}

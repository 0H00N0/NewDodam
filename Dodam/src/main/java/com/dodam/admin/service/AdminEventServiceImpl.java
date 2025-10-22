package com.dodam.admin.service;

import com.dodam.admin.dto.EventRequestDTO;
import com.dodam.admin.dto.EventResponseDTO;
import com.dodam.admin.dto.WinnerDTO;
import com.dodam.event.entity.EventNumber;
import com.dodam.event.entity.First;
import com.dodam.event.repository.EventNumberRepository;
import com.dodam.event.repository.FirstRepository;
import com.dodam.event.repository.LotteryTicketTypeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminEventServiceImpl implements AdminEventService {

    private final EventNumberRepository eventNumberRepository;
    private final LotteryTicketTypeRepository lotteryTicketTypeRepository;
    private final FirstRepository firstRepository;

    @Override
    @Transactional
    public EventResponseDTO createEvent(EventRequestDTO dto) {
        log.info("이벤트 생성 시작 - evName: {}", dto.getEvName());
        
        EventNumber event = EventNumber.builder()
                .evName(dto.getEvName())
                .evContent(dto.getEvContent())
                .status(dto.getStatus() != null ? dto.getStatus() : 0)
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .eventType(dto.getEventType())
                .lotteryTicketType(dto.getLotTypeNum() != null ?
                        lotteryTicketTypeRepository.findById(dto.getLotTypeNum())
                                .orElseThrow(() -> new EntityNotFoundException("해당 추첨권 타입이 없습니다."))
                        : null)
                .capacity("FIRST".equalsIgnoreCase(dto.getEventType()) ? dto.getCapacity() : null)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        EventNumber saved = eventNumberRepository.save(event);
        log.info("이벤트 생성 완료 - evNum: {}", saved.getEvNum());
        
        return toDTO(saved);
    }

    @Override
    @Transactional
    public EventResponseDTO updateEvent(Long evNum, EventRequestDTO dto) {
        log.info("이벤트 수정 시작 - evNum: {}", evNum);
        
        EventNumber event = eventNumberRepository.findById(evNum)
                .orElseThrow(() -> new EntityNotFoundException("해당 이벤트를 찾을 수 없습니다. ID=" + evNum));

        event.setEvName(dto.getEvName());
        event.setEvContent(dto.getEvContent());
        event.setStatus(dto.getStatus());
        event.setStartTime(dto.getStartTime());
        event.setEndTime(dto.getEndTime());
        event.setEventType(dto.getEventType() != null ? dto.getEventType() : event.getEventType());
        event.setUpdatedAt(LocalDateTime.now());

        if ("FIRST".equalsIgnoreCase(event.getEventType())) {
            event.setCapacity(dto.getCapacity());
        } else {
            event.setCapacity(null);
        }

        EventNumber updated = eventNumberRepository.save(event);
        log.info("이벤트 수정 완료 - evNum: {}", evNum);
        
        return toDTO(updated);
    }

    /**
     * ✅ 이벤트 삭제 (FK 제약조건 위배 방지)
     * ORA-02292 에러 해결: 자식 레코드(First) 먼저 삭제 후 이벤트 삭제
     */
    @Override
    @Transactional
    public void deleteEvent(Long evNum) {
        log.info("이벤트 삭제 시작 - evNum: {}", evNum);
        
        EventNumber event = eventNumberRepository.findById(evNum)
                .orElseThrow(() -> new EntityNotFoundException("해당 이벤트를 찾을 수 없습니다. ID=" + evNum));

        try {
            // 1. 자식 레코드(선착순 참여 데이터) 먼저 삭제
            log.info("연관된 선착순 참여 데이터 삭제 중 - evNum: {}", evNum);
            firstRepository.deleteByEvent(event);
            
            // 2. 이벤트 삭제
            log.info("이벤트 삭제 중 - evNum: {}", evNum);
            eventNumberRepository.delete(event);
            
            log.info("이벤트 삭제 완료 - evNum: {}", evNum);
        } catch (Exception e) {
            log.error("이벤트 삭제 실패 - evNum: {}, error: {}", evNum, e.getMessage(), e);
            throw new RuntimeException("이벤트 삭제 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    @Override
    public EventResponseDTO getEventById(Long evNum) {
        log.info("이벤트 조회 - evNum: {}", evNum);
        
        EventNumber event = eventNumberRepository.findById(evNum)
                .orElseThrow(() -> new EntityNotFoundException("해당 이벤트를 찾을 수 없습니다. ID=" + evNum));
        return toDTO(event);
    }

    @Override
    public List<EventResponseDTO> getAllEvents() {
        log.info("모든 이벤트 조회 시작");
        
        return eventNumberRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * ✅ 선착순 이벤트 당첨자 조회
     */
    @Override
    public List<WinnerDTO> getFirstEventWinners(Long evNum) { 
        log.info("당첨자 조회 - evNum: {}", evNum);
        
        EventNumber event = eventNumberRepository.findById(evNum)
                .orElseThrow(() -> new EntityNotFoundException("해당 이벤트를 찾을 수 없습니다."));

        if (!"FIRST".equalsIgnoreCase(event.getEventType())) {
            throw new IllegalArgumentException("이 이벤트는 선착순 이벤트가 아닙니다.");
        }

        List<First> applicants = firstRepository.findByEvent_EvNumOrderByFdateAsc(evNum);
        int capacity = event.getCapacity() != null ? event.getCapacity() : applicants.size();

        log.info("당첨자 조회 완료 - evNum: {}, 당첨자 수: {}", evNum, Math.min(capacity, applicants.size()));
        
        return applicants.stream()
                .limit(capacity)
                .map(f -> {
                    var member = f.getMember();
                    return WinnerDTO.builder()
                            .mnum(member.getMnum())
                            .mname(member.getMname())
                            .memail(member.getMemail())
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Entity -> DTO 변환
     */
    private EventResponseDTO toDTO(EventNumber entity) {
        return EventResponseDTO.builder()
                .evNum(entity.getEvNum())
                .evName(entity.getEvName())
                .evContent(entity.getEvContent())
                .status(entity.getStatus())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .eventType(entity.getEventType())
                .capacity(entity.getCapacity())
                .build();
    }
}
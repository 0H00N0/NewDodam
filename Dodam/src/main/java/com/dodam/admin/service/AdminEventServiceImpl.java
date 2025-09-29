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
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminEventServiceImpl implements AdminEventService {

    private final EventNumberRepository eventNumberRepository;
    private final LotteryTicketTypeRepository lotteryTicketTypeRepository;
    private final FirstRepository firstRepository;

    @Override
    public EventResponseDTO createEvent(EventRequestDTO dto) {
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
        return toDTO(saved);
    }

    @Override
    public EventResponseDTO updateEvent(Long evNum, EventRequestDTO dto) {
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
        return toDTO(updated);
    }

    @Override
    public void deleteEvent(Long evNum) {
        if (!eventNumberRepository.existsById(evNum)) {
            throw new EntityNotFoundException("해당 이벤트를 찾을 수 없습니다. ID=" + evNum);
        }
        eventNumberRepository.deleteById(evNum);
    }

    @Override
    public EventResponseDTO getEventById(Long evNum) {
        EventNumber event = eventNumberRepository.findById(evNum)
                .orElseThrow(() -> new EntityNotFoundException("해당 이벤트를 찾을 수 없습니다. ID=" + evNum));
        return toDTO(event);
    }

    @Override
    public List<EventResponseDTO> getAllEvents() {
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
        EventNumber event = eventNumberRepository.findById(evNum)
                .orElseThrow(() -> new EntityNotFoundException("해당 이벤트를 찾을 수 없습니다."));

        if (!"FIRST".equalsIgnoreCase(event.getEventType())) {
            throw new IllegalArgumentException("이 이벤트는 선착순 이벤트가 아닙니다.");
        }

        List<First> applicants = firstRepository.findByEvent_EvNumOrderByFdateAsc(evNum);
        int capacity = event.getCapacity() != null ? event.getCapacity() : applicants.size();

        return applicants.stream()
                .limit(capacity)
                .map(f -> {
                    var member = f.getMember();
                    return WinnerDTO.builder()
                            .mnum(member.getMnum())
                            .mname(member.getMname())   // ✅ 수정: mname
                            .memail(member.getMemail()) // ✅ 수정: memail
                            .build();
                })
                .collect(Collectors.toList());
    }

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

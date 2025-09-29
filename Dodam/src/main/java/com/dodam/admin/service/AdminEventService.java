package com.dodam.admin.service;

import com.dodam.admin.dto.EventRequestDTO;
import com.dodam.admin.dto.EventResponseDTO;
import com.dodam.admin.dto.WinnerDTO;

import java.util.List;

public interface AdminEventService {
    EventResponseDTO createEvent(EventRequestDTO dto);
    EventResponseDTO updateEvent(Long evNum, EventRequestDTO dto);
    void deleteEvent(Long evNum);
    EventResponseDTO getEventById(Long evNum);
    List<EventResponseDTO> getAllEvents();

    // ✅ 수정된 선언
    List<WinnerDTO> getFirstEventWinners(Long evNum);
}
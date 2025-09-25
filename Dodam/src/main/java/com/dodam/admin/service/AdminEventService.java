package com.dodam.admin.service;

import com.dodam.admin.dto.EventRequestDTO;
import com.dodam.admin.dto.EventResponseDTO;

import java.util.List;

public interface AdminEventService {
    EventResponseDTO createEvent(EventRequestDTO dto);
    EventResponseDTO updateEvent(Long evNum, EventRequestDTO dto);
    void deleteEvent(Long evNum);
    EventResponseDTO getEventById(Long evNum);
    List<EventResponseDTO> getAllEvents();
}

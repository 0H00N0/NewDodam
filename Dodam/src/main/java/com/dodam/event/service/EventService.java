package com.dodam.event.service;

import com.dodam.event.dto.EventJoinRequestDTO;
import com.dodam.event.dto.EventResponseDTO;

import java.util.List;

public interface EventService {
    List<EventResponseDTO> getAllEvents();
    EventResponseDTO getEvent(Long evNum);
    String joinEvent(EventJoinRequestDTO request);
}


package com.dodam.event.controller;

import com.dodam.event.dto.EventJoinRequestDTO;
import com.dodam.event.dto.EventResponseDTO;
import com.dodam.event.service.EventService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("eventController2")
@RequestMapping("/events2")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class EventController {

    private final EventService eventService;

    /** 모든 이벤트 조회 */
    @GetMapping
    public ResponseEntity<List<EventResponseDTO>> getAllEvents() {
        return ResponseEntity.ok(eventService.getAllEvents());
    }

    /** 단건 이벤트 조회 */
    @GetMapping("/{evNum}")
    public ResponseEntity<EventResponseDTO> getEvent(@PathVariable("evNum") Long evNum) {
        return ResponseEntity.ok(eventService.getEvent(evNum));
    }

    /** 이벤트 응모 */
    @PostMapping("/join")
    public ResponseEntity<String> joinEvent(@RequestBody EventJoinRequestDTO request) {
        String message = eventService.joinEvent(request);
        return new ResponseEntity<>(message, HttpStatus.CREATED);
    }

    /** 예외 처리 */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<String> handleEntityNotFound(EntityNotFoundException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGlobalException(Exception ex) {
        return new ResponseEntity<>("서버 오류 발생: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

package com.dodam.admin.controller;

import com.dodam.admin.dto.ApiResponseDTO;
import com.dodam.admin.dto.EventRequestDTO;
import com.dodam.admin.dto.EventResponseDTO;
import com.dodam.admin.dto.WinnerDTO;
import com.dodam.admin.service.AdminEventService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/events")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class AdminEventController {

    private final AdminEventService adminEventService;

    /** 이벤트 전체 조회 */
    @GetMapping
    public ResponseEntity<List<EventResponseDTO>> getAllEvents() {
        return ResponseEntity.ok(adminEventService.getAllEvents());
    }

    /** 이벤트 단건 조회 */
    @GetMapping("/{evNum}")
    public ResponseEntity<EventResponseDTO> getEventById(@PathVariable("evNum") Long evNum) {
        return ResponseEntity.ok(adminEventService.getEventById(evNum));
    }

    /** 이벤트 생성 */
    @PostMapping
    public ResponseEntity<EventResponseDTO> createEvent(@RequestBody EventRequestDTO dto) {
        EventResponseDTO created = adminEventService.createEvent(dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /** 이벤트 수정 */
    @PutMapping("/{evNum}")
    public ResponseEntity<EventResponseDTO> updateEvent(@PathVariable("evNum") Long evNum,
                                                        @RequestBody EventRequestDTO dto) {
        EventResponseDTO updated = adminEventService.updateEvent(evNum, dto);
        return ResponseEntity.ok(updated);
    }

    /** 이벤트 삭제 */
    @DeleteMapping("/{evNum}")
    public ResponseEntity<ApiResponseDTO> deleteEvent(@PathVariable("evNum") Long evNum) {
        adminEventService.deleteEvent(evNum);
        return ResponseEntity.ok(new ApiResponseDTO(true, "이벤트가 성공적으로 삭제되었습니다."));
    }

    /** ✅ 선착순 이벤트 당첨자 조회 */
    @GetMapping("/{evNum}/winners")
    public ResponseEntity<List<WinnerDTO>> getFirstEventWinners(@PathVariable("evNum") Long evNum) {
        List<WinnerDTO> winners = adminEventService.getFirstEventWinners(evNum);
        return ResponseEntity.ok(winners);
    }

    /** 예외 처리 */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponseDTO> handleEntityNotFound(EntityNotFoundException ex) {
        return new ResponseEntity<>(new ApiResponseDTO(false, ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseDTO> handleGlobalException(Exception ex) {
        return new ResponseEntity<>(new ApiResponseDTO(false, "서버 오류 발생: " + ex.getMessage()),
                                    HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

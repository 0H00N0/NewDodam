package com.dodam.board.controller;
import com.dodam.board.*;
import com.dodam.board.dto.event.EventCreateRequest;
import com.dodam.board.dto.event.EventResponse;
import com.dodam.board.dto.event.EventUpdateRequest;
import com.dodam.board.service.EventService;
import jakarta.validation.Valid; 
import lombok.RequiredArgsConstructor; 
import org.springframework.data.domain.*; 
import org.springframework.web.bind.annotation.*; 
import java.time.LocalDate;
@RestController 
@RequestMapping("/events") 
@RequiredArgsConstructor public class EventController {
  private final EventService svc;
  @PostMapping 
  public EventResponse create(@Valid @RequestBody EventCreateRequest req){ return svc.create(req); }
  @GetMapping 
  public Page<EventResponse> list(@RequestParam String boardCode, @RequestParam(required=false) Boolean activeOnly, @RequestParam(required=false) LocalDate from, @RequestParam(required=false) LocalDate to, Pageable p){ return svc.list(boardCode, activeOnly, from, to, p); }
  @GetMapping("/{id}") 
  public EventResponse get(@PathVariable Long id, @RequestParam(defaultValue="true") boolean incViews){ return svc.get(id, incViews); }
  @PutMapping("/{id}") 
  public EventResponse upd(@PathVariable Long id, @Valid @RequestBody EventUpdateRequest req){ return svc.update(id, req); }
  @DeleteMapping("/{id}") 
  public void del(@PathVariable Long id){ svc.delete(id); }
}
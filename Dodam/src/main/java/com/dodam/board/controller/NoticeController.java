package com.dodam.board.controller;
import com.dodam.board.*;
import com.dodam.board.dto.notice.NoticeCreateRequest;
import com.dodam.board.dto.notice.NoticeResponse;
import com.dodam.board.dto.notice.NoticeUpdateRequest;
import com.dodam.board.service.NoticeService;

import jakarta.validation.Valid; 
import lombok.RequiredArgsConstructor; 
import org.springframework.data.domain.*; 
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/notices") @RequiredArgsConstructor public class NoticeController {
  private final NoticeService svc;
  @PostMapping public NoticeResponse create(@Valid @RequestBody NoticeCreateRequest req){ return svc.create(req); }
  @GetMapping public Page<NoticeResponse> list(@RequestParam String boardCode, Pageable p){ return svc.list(boardCode, p); }
  @GetMapping("/{id}") public NoticeResponse get(@PathVariable Long id, @RequestParam(defaultValue="true") boolean incViews){ return svc.get(id, incViews); }
  @PutMapping("/{id}") public NoticeResponse upd(@PathVariable Long id, @Valid @RequestBody NoticeUpdateRequest req){ return svc.update(id, req); }
  @DeleteMapping("/{id}") public void del(@PathVariable Long id){ svc.delete(id); }
}
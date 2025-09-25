package com.dodam.board.controller;
import com.dodam.board.*;
import com.dodam.board.dto.faq.FaqCreateRequest;
import com.dodam.board.dto.faq.FaqResponse;
import com.dodam.board.dto.faq.FaqUpdateRequest;
import com.dodam.board.service.FaqService;

import jakarta.validation.Valid; 
import lombok.RequiredArgsConstructor; 
import org.springframework.data.domain.*; 
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/faqs") @RequiredArgsConstructor public class FaqController {
  private final FaqService svc;
  @PostMapping public FaqResponse create(@Valid @RequestBody FaqCreateRequest req){ return svc.create(req); }
  @GetMapping public Page<FaqResponse> list(@RequestParam String boardCode, @RequestParam(required=false) String category, Pageable p){ return svc.list(boardCode, category, p); }
  @GetMapping("/{id}") public FaqResponse get(@PathVariable Long id){ return svc.get(id); }
  @PutMapping("/{id}") public FaqResponse upd(@PathVariable Long id, @Valid @RequestBody FaqUpdateRequest req){ return svc.update(id, req); }
  @DeleteMapping("/{id}") public void del(@PathVariable Long id){ svc.delete(id); }
}
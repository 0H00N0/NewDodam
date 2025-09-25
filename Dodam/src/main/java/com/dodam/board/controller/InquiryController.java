package com.dodam.board.controller;
import com.dodam.board.*;
import com.dodam.board.dto.inquiry.InquiryCreateRequest;
import com.dodam.board.dto.inquiry.InquiryResponse;
import com.dodam.board.dto.inquiry.InquiryUpdateRequest;
import com.dodam.board.service.InquiryService;

import jakarta.validation.Valid; 
import lombok.RequiredArgsConstructor; 
import org.springframework.data.domain.*; 
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/inquiries") @RequiredArgsConstructor public class InquiryController {
  private final InquiryService svc;
  @PostMapping public InquiryResponse create(@Valid @RequestBody InquiryCreateRequest req){ return svc.create(req); }
  @GetMapping public Page<InquiryResponse> list(@RequestParam String boardCode, Pageable p){ return svc.list(boardCode, p); }
  @GetMapping("/{id}") public InquiryResponse get(@PathVariable Long id, @RequestParam(required=false) String password){ return svc.get(id, password); }
  @PutMapping("/{id}") public InquiryResponse upd(@PathVariable Long id, @Valid @RequestBody InquiryUpdateRequest req){ return svc.update(id, req); }
  @DeleteMapping("/{id}") public void del(@PathVariable Long id){ svc.delete(id); }
}
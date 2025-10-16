package com.dodam.inquiry.controller;

import com.dodam.inquiry.dto.*;
import com.dodam.inquiry.service.ProductInquiryService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/product-inquiries")
public class ProductInquiryController {

  private final ProductInquiryService svc;

  @PostMapping
  public ResponseEntity<ProductInquiryResponse> create(@Valid @RequestBody ProductInquiryCreateRequest req,
                                                       HttpSession session) {
    String mid = (String) session.getAttribute("sid");
    if (mid == null || mid.isBlank()) return ResponseEntity.status(401).build();
    return ResponseEntity.ok(svc.create(mid, req));
  }

  @GetMapping("/my")
  public ResponseEntity<List<ProductInquiryResponse>> my(HttpSession session) {
    String mid = (String) session.getAttribute("sid");
    if (mid == null || mid.isBlank()) return ResponseEntity.status(401).build();
    return ResponseEntity.ok(svc.my(mid));
  }
}

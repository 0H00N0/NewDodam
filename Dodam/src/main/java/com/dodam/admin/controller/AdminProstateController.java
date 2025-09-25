package com.dodam.admin.controller;

import com.dodam.admin.dto.ProstateResponseDTO;
import com.dodam.admin.service.AdminProstateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/prostates")
@RequiredArgsConstructor
public class AdminProstateController {

    private final AdminProstateService adminProstateService;

    @GetMapping
    public ResponseEntity<List<ProstateResponseDTO>> getAllProstates() {
        List<ProstateResponseDTO> prostates = adminProstateService.findAllProstates();
        return ResponseEntity.ok(prostates);
    }
}
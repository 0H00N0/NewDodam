package com.dodam.admin.controller;

import com.dodam.admin.dto.DeliverymanRequestDTO;
import com.dodam.admin.dto.DeliverymanResponseDTO;
import com.dodam.admin.service.AdminDeliverymanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/deliverymen")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class AdminDeliverymanController {

    private final AdminDeliverymanService service;

    @PostMapping
    public ResponseEntity<DeliverymanResponseDTO> create(@Valid @RequestBody DeliverymanRequestDTO dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    @GetMapping
    public ResponseEntity<List<DeliverymanResponseDTO>> list() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{delnum}")
    public ResponseEntity<DeliverymanResponseDTO> get(@PathVariable Long delnum) {
        return ResponseEntity.ok(service.findById(delnum));
    }

    @PutMapping("/{delnum}")
    public ResponseEntity<DeliverymanResponseDTO> update(@PathVariable Long delnum,
                                                         @Valid @RequestBody DeliverymanRequestDTO dto) {
        return ResponseEntity.ok(service.update(delnum, dto));
    }

    @DeleteMapping("/{delnum}")
    public ResponseEntity<Void> delete(@PathVariable Long delnum) {
        service.delete(delnum);
        return ResponseEntity.noContent().build();
    }
}


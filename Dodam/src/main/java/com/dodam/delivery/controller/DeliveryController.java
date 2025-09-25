package com.dodam.delivery.controller;

import com.dodam.delivery.dto.DeliveryDTO.DeliveryMeDTO;
import com.dodam.delivery.dto.DeliveryDTO.AssignmentDTO;
import com.dodam.delivery.dto.DeliveryDTO.AreaDTO;
import com.dodam.delivery.dto.DeliveryDTO.MapPointDTO;
import com.dodam.delivery.dto.DeliveryDTO.CustomerDTO;
import com.dodam.delivery.dto.DeliveryDTO.ProductCheckDTO;
import com.dodam.delivery.dto.DeliveryDTO.ReturnCreateDTO;
import com.dodam.delivery.dto.DeliveryDTO.PerformanceDTO;
import com.dodam.delivery.dto.DeliveryDTO.ChargesDTO;
import com.dodam.delivery.dto.DeliveryDTO.OperationResult;
import com.dodam.delivery.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/delivery")
public class DeliveryController {

    private final DeliveryService svc;

    @GetMapping("/me")
    public DeliveryMeDTO me() {
        return svc.me();
    }

    @GetMapping("/assignments/today")
    public List<AssignmentDTO> today() {
        return svc.today();
    }

    @GetMapping("/areas")
    public List<AreaDTO> areas() {
        return svc.areas();
    }

    @GetMapping("/map/points")
    public List<MapPointDTO> mapPoints() {
        return svc.mapPoints();
    }

    @GetMapping("/customers")
    public List<CustomerDTO> customers(@RequestParam(required = false) String q) {
        return svc.customers(q);
    }

    @GetMapping("/products/check")
    public ProductCheckDTO check(@RequestParam String key) {
        return svc.productCheck(key);
    }

    @PostMapping("/returns")
    public OperationResult createReturn(@RequestBody ReturnCreateDTO dto) {
        return svc.createReturn(dto);
    }

    @GetMapping("/performance")
    public PerformanceDTO performance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return svc.performance(from, to);
    }

    @GetMapping("/charges")
    public ChargesDTO charges(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return svc.charges(from, to);
    }
}

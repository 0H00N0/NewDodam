// src/main/java/com/dodam/delivery/dto/DeliveryDtos.java
package com.dodam.delivery.dto;

import java.math.BigDecimal;
import java.util.List;

public final class DeliveryDTO {

    public record DeliveryMeDTO(Long delnum, Long mnum, String name, String location, Integer dayoffRemain) {}
    public record AssignmentDTO(Long id, String status, String address, String receiver, String phone, String expectTime) {}
    public record AreaDTO(Long id, String name) {}
    public record MapPointDTO(Double lat, Double lng, String label) {}
    public record CustomerDTO(Long orderId, String name, String address, String phone, String lastStatus) {}
    public record ProductCheckDTO(Long productId, String proname, String grade, boolean ok, String message) {}
    public record ReturnCreateDTO(Long orderId, Long productId, String reason, String memo) {}
    public record PerformanceDTO(int total, int delivered, int returned, int failed) {}
    public record ChargesDTO(int deliveredCount, BigDecimal deliveryFeeSum, BigDecimal bonus, BigDecimal total) {}
    public record OperationResult(boolean success, String message) {}
}

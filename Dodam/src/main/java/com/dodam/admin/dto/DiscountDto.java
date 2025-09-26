package com.dodam.admin.dto;

import lombok.*;

public class DiscountDto {

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Request {
        private Integer disLevel;
        private Integer disValue;
        private Long ptermId; // PlanTerms FK
    }

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Response {
        private Long disNum;
        private Integer disLevel;
        private Integer disValue;
        private Long ptermId;
    }
}

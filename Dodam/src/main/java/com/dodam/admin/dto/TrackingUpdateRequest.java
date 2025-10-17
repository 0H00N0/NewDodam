// src/main/java/com/dodam/admin/dto/TrackingUpdateRequest.java
package com.dodam.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class TrackingUpdateRequest {
    @JsonProperty("trackingNumber")
    private String trackingNumber; // null/"" 허용 (비우기 지원)
}

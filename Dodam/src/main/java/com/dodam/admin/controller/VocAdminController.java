package com.dodam.admin.controller;

import com.dodam.admin.dto.VocAdminDto;
import com.dodam.admin.service.VocAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Sort;

@RestController
@RequestMapping("/admin/voc")
@RequiredArgsConstructor
public class VocAdminController {

    private final VocAdminService vocAdminService;

    /**
     * 전체 VOC 목록 조회 API
     * @param pageable 페이징 정보 (예: /api/v1/admin/voc?page=0&size=10)
     */
    @GetMapping
    public ResponseEntity<Page<VocAdminDto.VocListResponse>> getAllVocs(
            @PageableDefault(
                    sort = "createdAt",
                    direction = Sort.Direction.DESC,
                    size = 20
            ) Pageable pageable) {
        Page<VocAdminDto.VocListResponse> vocs = vocAdminService.getAllVocs(pageable);
        return ResponseEntity.ok(vocs);
    }

    /**
     * 특정 VOC 상세 조회 API
     * @param vocId 조회할 VOC의 ID
     */
    @GetMapping("/{vocId}")
    public ResponseEntity<VocAdminDto.VocDetailResponse> getVocById(@PathVariable("vocId") Long vocId) {
        VocAdminDto.VocDetailResponse voc = vocAdminService.getVocById(vocId);
        return ResponseEntity.ok(voc);
    }

    /**
     * VOC 답변 및 상태 업데이트 API
     * @param vocId 업데이트할 VOC의 ID
     * @param request 업데이트 요청 정보 (답변 내용, 상태 등)
     */
    @PatchMapping("/{vocId}")
    public ResponseEntity<VocAdminDto.VocDetailResponse> updateVoc(
            @PathVariable("vocId") Long vocId,
            @RequestBody VocAdminDto.VocUpdateRequest request) {
        VocAdminDto.VocDetailResponse updatedVoc = vocAdminService.updateVoc(vocId, request);
        return ResponseEntity.ok(updatedVoc);
    }
}
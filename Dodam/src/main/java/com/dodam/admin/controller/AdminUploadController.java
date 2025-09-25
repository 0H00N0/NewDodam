package com.dodam.admin.controller;

import com.dodam.admin.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/admin/upload")
@RequiredArgsConstructor
public class AdminUploadController {

    private final FileUploadService fileUploadService;

    /**
     * 이미지 파일을 서버에 업로드하고 저장된 파일명을 반환하는 API
     * @param image 프론트엔드에서 'image'라는 key로 보낸 파일 데이터
     * @return JSON 형태의 저장된 파일명 (e.g., { "imageName": "uuid_image.jpg" })
     */
    @PostMapping("/image")
    public ResponseEntity<?> uploadImage(@RequestParam("image") MultipartFile image) {
        try {
            String storedFileName = fileUploadService.storeFile(image);
            // 프론트엔드에서 사용하기 쉽도록 JSON 객체로 감싸서 반환합니다.
            return ResponseEntity.ok(Map.of("imageName", storedFileName));
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("이미지 업로드에 실패했습니다: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
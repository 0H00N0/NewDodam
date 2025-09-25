package com.dodam.admin.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileUploadService {

    // application.properties에 설정한 파일 저장 경로를 주입받습니다.
    @Value("${file.upload-dir}")
    private String uploadDir;

    /**
     * MultipartFile을 서버에 저장하고, 고유한 파일명을 반환합니다.
     * @param file 업로드된 이미지 파일
     * @return 서버에 저장된 고유한 파일명
     * @throws IOException 파일 저장 중 오류 발생 시
     */
    public String storeFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일을 선택해주세요.");
        }

        // 파일명 중복을 피하기 위해 UUID를 사용합니다.
        String originalFilename = file.getOriginalFilename();
        String storedFileName = UUID.randomUUID().toString() + "_" + originalFilename;

        Path destinationPath = Paths.get(uploadDir + storedFileName);

        // 디렉토리가 존재하지 않으면 생성합니다.
        Files.createDirectories(destinationPath.getParent());

        // 파일을 지정된 경로에 저장합니다.
        Files.copy(file.getInputStream(), destinationPath);

        return storedFileName;
    }
}
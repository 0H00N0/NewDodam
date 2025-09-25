package com.dodam.admin.controller; // 실제 패키지 경로에 맞게 수정

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@RestController
public class TestFileController {
    
    @Value("${file.upload-dir}")
    private String uploadDir;
    
    @GetMapping("/test/file/{filename}")
    public Map<String, Object> testFile(@PathVariable String filename) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            File file = new File(uploadDir + filename);
            
            result.put("filename", filename);
            result.put("uploadDir", uploadDir);
            result.put("fullPath", file.getAbsolutePath());
            result.put("exists", file.exists());
            result.put("canRead", file.canRead());
            result.put("isFile", file.isFile());
            result.put("size", file.exists() ? file.length() : "N/A");
            
            // 디렉토리 내 파일들도 확인
            File directory = new File(uploadDir);
            if (directory.exists() && directory.isDirectory()) {
                File[] files = directory.listFiles();
                if (files != null) {
                    result.put("filesInDirectory", files.length);
                    // 처음 5개 파일 이름만 보여주기
                    String[] fileNames = new String[Math.min(5, files.length)];
                    for (int i = 0; i < fileNames.length; i++) {
                        fileNames[i] = files[i].getName();
                    }
                    result.put("sampleFiles", fileNames);
                }
            }
            
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }
        
        return result;
    }
}
package com.dodam.config; 

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebMvc 
public class WebConfig implements WebMvcConfigurer {

    
	@Value("${file.upload-dir:C:/dev/uploads/}")
    private String uploadDir; 
     
     @Override
     public void addCorsMappings(CorsRegistry registry) {
         registry.addMapping("/**")
                 .allowedOrigins("http://localhost:3000")
                 .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                 .allowedHeaders("*")
                 .allowCredentials(true);
     }
     
     @Override
     public void addResourceHandlers(ResourceHandlerRegistry registry) {
         System.out.println("업로드 디렉토리: " + uploadDir); // 디버깅용 로그
         
         // 이미지 파일을 /images/** 경로로 접근할 수 있도록 설정
         registry.addResourceHandler("/images/**")
                 .addResourceLocations("file:" + uploadDir)
                 .setCachePeriod(3600);
                 
         // 기본 정적 리소스도 설정
         registry.addResourceHandler("/**")
                 .addResourceLocations("classpath:/static/", "classpath:/public/");
     }
}
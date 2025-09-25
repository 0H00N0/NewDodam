package com.dodam.admin.controller;

import com.dodam.admin.dto.ApiResponseDTO;
import com.dodam.admin.dto.MemberResponseDTO;
import com.dodam.admin.service.AdminMemberService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/members")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true") // React 개발 서버 CORS 허용
public class AdminMemberController {

    private final AdminMemberService adminMemberService;

    /**
     * 모든 회원 목록을 조회하는 API
     * [GET /admin/members]
     */
    @GetMapping
    public ResponseEntity<List<MemberResponseDTO>> getAllMembers() {
        List<MemberResponseDTO> members = adminMemberService.findAllMembers();
        return ResponseEntity.ok(members);
    }

    /**
     * 특정 회원 정보를 조회하는 API
     * [GET /admin/members/{mnum}]
     */
    @GetMapping("/{mnum}")
    public ResponseEntity<MemberResponseDTO> getMemberById(@PathVariable("mnum") Long mnum) {
        MemberResponseDTO member = adminMemberService.findMemberById(mnum);
        return ResponseEntity.ok(member);
    }

    /**
     * 특정 회원을 삭제(강제 탈퇴)하는 API
     * [DELETE /admin/members/{mnum}]
     */
    @DeleteMapping("/{mnum}")
    public ResponseEntity<ApiResponseDTO> deleteMember(@PathVariable("mnum") Long mnum) {
        adminMemberService.deleteMember(mnum);
        ApiResponseDTO response = new ApiResponseDTO(true, "회원이 성공적으로 삭제되었습니다.");
        return ResponseEntity.ok(response);
    }
    
    /**
     * 예외 처리 핸들러
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponseDTO> handleEntityNotFoundException(EntityNotFoundException ex) {
        ApiResponseDTO response = new ApiResponseDTO(false, ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseDTO> handleGlobalException(Exception ex) {
        ApiResponseDTO response = new ApiResponseDTO(false, "서버 처리 중 오류가 발생했습니다: " + ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

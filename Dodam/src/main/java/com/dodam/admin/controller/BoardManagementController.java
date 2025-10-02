package com.dodam.admin.controller;

import com.dodam.admin.dto.BoardManagementDTO;
import com.dodam.admin.service.BoardManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/boards")
@RequiredArgsConstructor
public class BoardManagementController {

    private final BoardManagementService boardManagementService;

    /**
     * 새로운 게시판 카테고리를 생성하는 API
     * POST /api/v1/admin/boards
     */
    @PostMapping
    public ResponseEntity<BoardManagementDTO.BoardCategoryResponse> createBoardCategory(@RequestBody BoardManagementDTO.CreateBoardCategoryRequest requestDto) {
        BoardManagementDTO.BoardCategoryResponse createdCategory = boardManagementService.createBoardCategory(requestDto);
        return new ResponseEntity<>(createdCategory, HttpStatus.CREATED);
    }

    /**
     * 모든 게시판 카테고리 목록을 조회하는 API
     * GET /api/v1/admin/boards
     */
    @GetMapping
    public ResponseEntity<List<BoardManagementDTO.BoardCategoryResponse>> getAllBoardCategories() {
        List<BoardManagementDTO.BoardCategoryResponse> categories = boardManagementService.getAllBoardCategories();
        return ResponseEntity.ok(categories);
    }

    /**
     * 게시판 카테고리를 삭제하는 API
     * DELETE /api/v1/admin/boards/{categoryId}
     */
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> deleteBoardCategory(@PathVariable("categoryId") Long categoryId) {
        boardManagementService.deleteBoardCategory(categoryId);
        return ResponseEntity.noContent().build(); // 성공적으로 삭제되었음을 의미 (204 No Content)
    }
    /**
     * 특정 카테고리의 모든 게시글을 조회하는 API
     * GET /api/v1/admin/boards/{categoryId}/posts
     */
    @GetMapping("/{categoryId}/posts")
    public ResponseEntity<List<BoardManagementDTO.PostResponse>> getPostsByCategory(@PathVariable("categoryId") Long categoryId) {
        List<BoardManagementDTO.PostResponse> posts = boardManagementService.getPostsByCategory(categoryId);
        return ResponseEntity.ok(posts);
    }
    /**
     * 특정 게시글의 상세 정보를 조회하는 API
     * GET /api/v1/admin/boards/posts/{postId}
     */
    @GetMapping("/posts/{postId}")
    public ResponseEntity<BoardManagementDTO.PostDetailResponse> getPostById(@PathVariable("postId") Long postId) {
        BoardManagementDTO.PostDetailResponse post = boardManagementService.getPostById(postId);
        return ResponseEntity.ok(post);
    }

    /**
     * 특정 게시글을 삭제하는 API
     * DELETE /api/v1/admin/boards/posts/{postId}
     */
    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable Long postId) {
        boardManagementService.deletePost(postId);
        return ResponseEntity.noContent().build();
    }
    /**
     * 새 게시글을 생성하는 API
     * POST /api/v1/admin/boards/posts
     */
    @PostMapping("/posts")
    public ResponseEntity<BoardManagementDTO.PostDetailResponse> createPost(@RequestBody BoardManagementDTO.CreatePostRequest requestDto) {
        BoardManagementDTO.PostDetailResponse createdPost = boardManagementService.createPost(requestDto);
        return new ResponseEntity<>(createdPost, HttpStatus.CREATED);
    }
 // --- 카테고리 수정 ---
    @PutMapping("/{categoryId}")
    public ResponseEntity<BoardManagementDTO.BoardCategoryResponse> updateBoardCategory(
            @PathVariable("categoryId") Long categoryId,              // ← 이름 명시!
            @RequestBody BoardManagementDTO.UpdateBoardCategoryRequest requestDto) {
        return ResponseEntity.ok(boardManagementService.updateBoardCategory(categoryId, requestDto));
    }

 // --- 게시글 수정 API ---
    @PutMapping("/posts/{postId}")
    public ResponseEntity<BoardManagementDTO.PostDetailResponse> updatePost(
            @PathVariable("postId") Long postId,   // <-- 명시적으로 이름 지정
            @RequestBody BoardManagementDTO.UpdatePostRequest requestDto) {
        return ResponseEntity.ok(boardManagementService.updatePost(postId, requestDto));
    }


}
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

    // ------------------------------------------------------
    // 📌 게시판 카테고리 관련 API
    // ------------------------------------------------------

    /**
     * 새로운 게시판 카테고리를 생성하는 API
     * POST /admin/boards
     */
    @PostMapping
    public ResponseEntity<BoardManagementDTO.BoardCategoryResponse> createBoardCategory(
            @RequestBody BoardManagementDTO.CreateBoardCategoryRequest requestDto) {

        BoardManagementDTO.BoardCategoryResponse createdCategory =
                boardManagementService.createBoardCategory(requestDto);

        return new ResponseEntity<>(createdCategory, HttpStatus.CREATED);
    }

    /**
     * 모든 게시판 카테고리 목록을 조회하는 API
     * GET /admin/boards
     */
    @GetMapping
    public ResponseEntity<List<BoardManagementDTO.BoardCategoryResponse>> getAllBoardCategories() {
        List<BoardManagementDTO.BoardCategoryResponse> categories =
                boardManagementService.getAllBoardCategories();

        return ResponseEntity.ok(categories);
    }

    /**
     * 게시판 카테고리를 삭제하는 API
     * DELETE /admin/boards/{categoryId}
     */
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> deleteBoardCategory(@PathVariable("categoryId") Long categoryId) {
        boardManagementService.deleteBoardCategory(categoryId);
        return ResponseEntity.noContent().build(); // 204 No Content
    }

    /**
     * 게시판 카테고리 수정 API
     * PUT /admin/boards/{categoryId}
     */
    @PutMapping("/{categoryId}")
    public ResponseEntity<BoardManagementDTO.BoardCategoryResponse> updateBoardCategory(
            @PathVariable("categoryId") Long categoryId,
            @RequestBody BoardManagementDTO.UpdateBoardCategoryRequest requestDto) {

        BoardManagementDTO.BoardCategoryResponse updatedCategory =
                boardManagementService.updateBoardCategory(categoryId, requestDto);

        return ResponseEntity.ok(updatedCategory);
    }

    // ------------------------------------------------------
    // 📌 게시글 관련 API
    // ------------------------------------------------------

    /**
     * 특정 카테고리의 모든 게시글을 조회하는 API
     * GET /admin/boards/{categoryId}/posts
     */
    @GetMapping("/{categoryId}/posts")
    public ResponseEntity<List<BoardManagementDTO.PostResponse>> getPostsByCategory(
            @PathVariable("categoryId") Long categoryId) {

        List<BoardManagementDTO.PostResponse> posts =
                boardManagementService.getPostsByCategory(categoryId);

        return ResponseEntity.ok(posts);
    }

    /**
     * 특정 게시글의 상세 정보를 조회하는 API
     * GET /admin/boards/posts/{postId}
     */
    @GetMapping("/posts/{postId}")
    public ResponseEntity<BoardManagementDTO.PostDetailResponse> getPostById(
            @PathVariable("postId") Long postId) {

        BoardManagementDTO.PostDetailResponse post =
                boardManagementService.getPostById(postId);

        return ResponseEntity.ok(post);
    }

    /**
     * 게시글 생성 API
     * POST /admin/boards/posts
     */
    @PostMapping("/posts")
    public ResponseEntity<BoardManagementDTO.PostDetailResponse> createPost(
            @RequestBody BoardManagementDTO.CreatePostRequest requestDto) {

        BoardManagementDTO.PostDetailResponse createdPost =
                boardManagementService.createPost(requestDto);

        return new ResponseEntity<>(createdPost, HttpStatus.CREATED);
    }

    /**
     * 게시글 수정 API
     * PUT /admin/boards/posts/{postId}
     */
    @PutMapping("/posts/{postId}")
    public ResponseEntity<BoardManagementDTO.PostDetailResponse> updatePost(
            @PathVariable("postId") Long postId,
            @RequestBody BoardManagementDTO.UpdatePostRequest requestDto) {

        BoardManagementDTO.PostDetailResponse updatedPost =
                boardManagementService.updatePost(postId, requestDto);

        return ResponseEntity.ok(updatedPost);
    }

    /**
     * 게시글 삭제 API
     * DELETE /admin/boards/posts/{postId}
     */
    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable("postId") Long postId) {
        boardManagementService.deletePost(postId);
        return ResponseEntity.noContent().build();
    }
}

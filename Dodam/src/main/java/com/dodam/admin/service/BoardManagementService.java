package com.dodam.admin.service;

import com.dodam.admin.dto.BoardManagementDTO;
import com.dodam.board.repository.BoardCategoryRepository;
import com.dodam.board.repository.BoardRepository;
import com.dodam.board.repository.BoardStateRepository;

import jakarta.persistence.EntityNotFoundException;

import com.dodam.board.entity.BoardCategoryEntity;
import com.dodam.board.entity.BoardEntity;
import com.dodam.board.entity.BoardStateEntity;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor // final 필드에 대한 생성자를 자동으로 생성합니다.
@Transactional(readOnly = true) // 기본적으로 모든 메서드를 읽기 전용 트랜잭션으로 설정
public class BoardManagementService {

    private final BoardCategoryRepository boardCategoryRepository;
    private final BoardRepository boardRepository; // BoardRepository 주입
    private final BoardStateRepository boardStateRepository; // 주입

    /**
     * 새로운 게시판 카테고리를 생성합니다.
     *
     * @param requestDto 생성할 카테고리 정보 DTO
     * @return 생성된 카테고리 정보 DTO
     */
    @Transactional // 쓰기 작업이므로 개별적으로 트랜잭션 설정
    public BoardManagementDTO.BoardCategoryResponse createBoardCategory(BoardManagementDTO.CreateBoardCategoryRequest requestDto) {
        BoardCategoryEntity newCategory = requestDto.toEntity();
        BoardCategoryEntity savedCategory = boardCategoryRepository.save(newCategory);
        return BoardManagementDTO.BoardCategoryResponse.fromEntity(savedCategory);
    }

    /**
     * 모든 게시판 카테고리 목록을 조회합니다.
     *
     * @return 카테고리 정보 DTO 리스트
     */
    public List<BoardManagementDTO.BoardCategoryResponse> getAllBoardCategories() {
        return boardCategoryRepository.findAll().stream()
                .map(BoardManagementDTO.BoardCategoryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 특정 ID의 게시판 카테고리를 삭제합니다.
     *
     * @param categoryId 삭제할 카테고리 ID
     */
    @Transactional // 쓰기 작업이므로 개별적으로 트랜잭션 설정
    public void deleteBoardCategory(Long categoryId) {
        // 카테고리 존재 여부 확인 후 삭제
        if (!boardCategoryRepository.existsById(categoryId)) {
            throw new IllegalArgumentException("해당 ID의 게시판 카테고리가 존재하지 않습니다: " + categoryId);
        }
        boardCategoryRepository.deleteById(categoryId);
    }

    /**
     * 특정 카테고리에 속한 모든 게시글을 조회합니다.
     * @param categoryId 게시판 카테고리 ID
     * @return 해당 카테고리의 게시글 DTO 리스트
     */
    public List<BoardManagementDTO.PostResponse> getPostsByCategory(Long categoryId) {
        if (!boardCategoryRepository.existsById(categoryId)) {
            throw new IllegalArgumentException("존재하지 않는 게시판 카테고리입니다: " + categoryId);
        }
        return boardRepository.findByBoardCategory_BcanumOrderByBnumDesc(categoryId).stream()
                .map(BoardManagementDTO.PostResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 특정 ID의 게시글을 삭제합니다.
     * @param postId 삭제할 게시글 ID
     */
    @Transactional
    public void deletePost(Long postId) {
        if (!boardRepository.existsById(postId)) {
            throw new IllegalArgumentException("존재하지 않는 게시글입니다: " + postId);
        }
        boardRepository.deleteById(postId);
    }
    /**
     * 특정 ID의 게시글 상세 정보를 조회합니다.
     * @param postId 조회할 게시글 ID
     * @return 게시글 상세 정보 DTO
     */
    public BoardManagementDTO.PostDetailResponse getPostById(Long postId) {
        BoardEntity post = boardRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("게시글을 찾을 수 없습니다: " + postId));
        return BoardManagementDTO.PostDetailResponse.fromEntity(post);
    }

    /**
     * 관리자 계정으로 새 게시글을 생성합니다.
     * @param requestDto 생성할 게시글 정보 DTO
     * @return 생성된 게시글 상세 정보 DTO
     */
    @Transactional
    public BoardManagementDTO.PostDetailResponse createPost(BoardManagementDTO.CreatePostRequest requestDto) {
        // 카테고리와 상태 엔티티 조회
        BoardCategoryEntity category = boardCategoryRepository.findById(requestDto.getCategoryId())
                .orElseThrow(() -> new EntityNotFoundException("카테고리를 찾을 수 없습니다: " + requestDto.getCategoryId()));
        BoardStateEntity defaultState = boardStateRepository.findById(1L) // bsnum=1 (일반 상태)
                .orElseThrow(() -> new EntityNotFoundException("기본 게시글 상태를 찾을 수 없습니다."));

        // 새 게시글 엔티티 생성 및 정보 설정
        BoardEntity newPost = new BoardEntity();
        newPost.setBtitle(requestDto.getTitle());
        newPost.setBcontent(requestDto.getContent());
        newPost.setBoardCategory(category);
        newPost.setBoardState(defaultState);

        // 관리자 정보 고정
        newPost.setMid("admin");
        newPost.setMnic("관리자");
        newPost.setMnum(7L);

        BoardEntity savedPost = boardRepository.save(newPost);
        return BoardManagementDTO.PostDetailResponse.fromEntity(savedPost);
    }
}
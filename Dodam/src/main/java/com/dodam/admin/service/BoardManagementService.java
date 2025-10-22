package com.dodam.admin.service;

import com.dodam.admin.dto.BoardManagementDTO;
import com.dodam.board.repository.BoardCategoryRepository;
import com.dodam.board.repository.BoardRepository;
import com.dodam.board.repository.BoardStateRepository;
import com.dodam.board.entity.BoardCategoryEntity;
import com.dodam.board.entity.BoardEntity;
import com.dodam.board.entity.BoardStateEntity;
import com.dodam.member.entity.MemberEntity;
import com.dodam.member.repository.MemberRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardManagementService {

    private final BoardCategoryRepository boardCategoryRepository;
    private final BoardRepository boardRepository;
    private final BoardStateRepository boardStateRepository;
    private final MemberRepository memberRepository;

    // --- 카테고리 생성 ---
    @Transactional
    public BoardManagementDTO.BoardCategoryResponse createBoardCategory(BoardManagementDTO.CreateBoardCategoryRequest requestDto) {
        BoardCategoryEntity newCategory = requestDto.toEntity();
        BoardCategoryEntity savedCategory = boardCategoryRepository.save(newCategory);
        return BoardManagementDTO.BoardCategoryResponse.fromEntity(savedCategory);
    }

    // --- 카테고리 전체 조회 ---
    public List<BoardManagementDTO.BoardCategoryResponse> getAllBoardCategories() {
        return boardCategoryRepository.findAll().stream()
                .map(BoardManagementDTO.BoardCategoryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // --- 카테고리 삭제 (자식 게시글 먼저 삭제) ---
    @Transactional
    public void deleteBoardCategory(Long categoryId) {
        BoardCategoryEntity category = boardCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("해당 ID의 게시판 카테고리가 존재하지 않습니다: " + categoryId));

        // 1️⃣ 해당 카테고리에 속한 게시글 모두 조회
        List<BoardEntity> posts = boardRepository.findByBoardCategory_BcanumOrderByBnumDesc(categoryId);

        // 2️⃣ 게시글이 존재한다면 모두 삭제
        if (!posts.isEmpty()) {
            boardRepository.deleteAll(posts);
        }

        // 3️⃣ 카테고리 삭제
        boardCategoryRepository.delete(category);
    }

    // --- 특정 카테고리의 게시글 조회 ---
    public List<BoardManagementDTO.PostResponse> getPostsByCategory(Long categoryId) {
        if (!boardCategoryRepository.existsById(categoryId)) {
            throw new IllegalArgumentException("존재하지 않는 게시판 카테고리입니다: " + categoryId);
        }
        return boardRepository.findByBoardCategory_BcanumOrderByBnumDesc(categoryId).stream()
                .map(BoardManagementDTO.PostResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // --- 게시글 삭제 ---
    @Transactional
    public void deletePost(Long postId) {
        if (!boardRepository.existsById(postId)) {
            throw new IllegalArgumentException("존재하지 않는 게시글입니다: " + postId);
        }
        boardRepository.deleteById(postId);
    }

    // --- 게시글 상세 조회 ---
    public BoardManagementDTO.PostDetailResponse getPostById(Long postId) {
        BoardEntity post = boardRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("게시글을 찾을 수 없습니다: " + postId));
        return BoardManagementDTO.PostDetailResponse.fromEntity(post);
    }

    // --- 게시글 생성 (SecurityContext에서 로그인 사용자 조회) ---
    @Transactional
    public BoardManagementDTO.PostDetailResponse createPost(BoardManagementDTO.CreatePostRequest requestDto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String loginId = authentication.getName();

        BoardCategoryEntity category = boardCategoryRepository.findById(requestDto.getCategoryId())
                .orElseThrow(() -> new EntityNotFoundException("카테고리를 찾을 수 없습니다: " + requestDto.getCategoryId()));

        BoardStateEntity defaultState = boardStateRepository.findById(1L)
                .orElseThrow(() -> new EntityNotFoundException("기본 게시글 상태를 찾을 수 없습니다."));

        MemberEntity member = memberRepository.findByMid(loginId)
                .orElseThrow(() -> new EntityNotFoundException("회원 정보를 찾을 수 없습니다: " + loginId));

        BoardEntity newPost = new BoardEntity();
        newPost.setBsub(requestDto.getTitle());
        newPost.setBcontent(requestDto.getContent());
        newPost.setBoardCategory(category);
        newPost.setBoardState(defaultState);

        // ✅ 작성자 정보 세팅
        newPost.setMnum(member.getMnum());
        newPost.setMtnum(member.getMemtype().getMtnum());
        newPost.setMid(member.getMid());
        newPost.setMnic(member.getMnic());

        BoardEntity savedPost = boardRepository.save(newPost);
        return BoardManagementDTO.PostDetailResponse.fromEntity(savedPost);
    }

    // --- 카테고리 수정 ---
    @Transactional
    public BoardManagementDTO.BoardCategoryResponse updateBoardCategory(Long categoryId, BoardManagementDTO.UpdateBoardCategoryRequest requestDto) {
        BoardCategoryEntity category = boardCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("카테고리를 찾을 수 없습니다: " + categoryId));

        category.setBcaname(requestDto.getName());
        BoardCategoryEntity updated = boardCategoryRepository.save(category);

        return BoardManagementDTO.BoardCategoryResponse.fromEntity(updated);
    }

    // --- 게시글 수정 ---
    @Transactional
    public BoardManagementDTO.PostDetailResponse updatePost(Long postId, BoardManagementDTO.UpdatePostRequest requestDto) {
        BoardEntity post = boardRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("게시글을 찾을 수 없습니다: " + postId));

        post.setBsub(requestDto.getTitle());
        post.setBcontent(requestDto.getContent());

        // 카테고리 변경 가능
        if (requestDto.getCategoryId() != null) {
            BoardCategoryEntity category = boardCategoryRepository.findById(requestDto.getCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException("카테고리를 찾을 수 없습니다: " + requestDto.getCategoryId()));
            post.setBoardCategory(category);
        }

        // 상태 변경 가능
        if (requestDto.getStateId() != null) {
            BoardStateEntity state = boardStateRepository.findById(requestDto.getStateId())
                    .orElseThrow(() -> new EntityNotFoundException("게시글 상태를 찾을 수 없습니다: " + requestDto.getStateId()));
            post.setBoardState(state);
        }

        BoardEntity updatedPost = boardRepository.save(post);
        return BoardManagementDTO.PostDetailResponse.fromEntity(updatedPost);
    }
}

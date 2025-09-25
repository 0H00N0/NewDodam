package com.dodam.board.service;

import com.dodam.board.entity.BoardEntity;
import com.dodam.board.dto.BoardDto;
import com.dodam.board.repository.BoardRepository;
import com.dodam.board.error.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService {

    private final BoardRepository repo;

    public List<BoardDto> list() {
        return repo.findAll()
                .stream()
                .map(BoardDto::from)   // 엔티티→DTO 매핑은 DTO의 정적 메서드로 위임
                .toList();
    }

    /** PK(bnum)로 조회 */
    public BoardEntity getOrThrow(Long bnum) {
        return repo.findById(bnum)
                .orElseThrow(() -> new NotFoundException("보드를 찾을 수 없습니다: " + bnum));
    }

    /**
     * (기존 컨트롤러가 아직 String 'code'를 넘긴다면 임시 호환)
     * 숫자 문자열만 허용: "123" -> bnum=123
     */
    public BoardEntity getByCodeOrThrow(String code) {
        try {
            Long bnum = Long.valueOf(code);
            return getOrThrow(bnum);
        } catch (NumberFormatException e) {
            throw new NotFoundException("지원되지 않는 보드 식별자(숫자만 허용): " + code);
        }
    }
}
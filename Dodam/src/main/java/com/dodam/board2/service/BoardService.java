package com.dodam.board2.service;

import com.dodam.board.entity.BoardEntity;
import com.dodam.board2.repository.BoardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("boardService2")
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;

    // 전체 게시글 조회
    public List<BoardEntity> getAllBoards() {
        return boardRepository.findAll();
    }

    // 게시글 저장
    public BoardEntity saveBoard(BoardEntity board) {
        return boardRepository.save(board);
    }

    // 게시글 단건 조회
    public BoardEntity getBoardById(Long id) {
        return boardRepository.findById(id).orElse(null);
    }

    // 게시글 삭제
    public void deleteBoard(Long id) {
        boardRepository.deleteById(id);
    }
}

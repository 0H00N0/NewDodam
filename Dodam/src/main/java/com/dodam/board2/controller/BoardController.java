package com.dodam.board2.controller;

import com.dodam.board.entity.BoardEntity;
import com.dodam.board2.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("boardController2")
@RequestMapping("/api/boards")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    // 전체 조회
    @GetMapping
    public List<BoardEntity> getBoards() {
        return boardService.getAllBoards();
    }

    // 단건 조회
    @GetMapping("/{id}")
    public BoardEntity getBoard(@PathVariable Long id) {
        return boardService.getBoardById(id);
    }

    // 저장
    @PostMapping
    public BoardEntity createBoard(@RequestBody BoardEntity board) {
        return boardService.saveBoard(board);
    }

    // 삭제
    @DeleteMapping("/{id}")
    public void deleteBoard(@PathVariable Long id) {
        boardService.deleteBoard(id);
    }
}

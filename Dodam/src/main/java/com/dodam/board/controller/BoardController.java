package com.dodam.board.controller;
import com.dodam.board.dto.BoardDto; 
import com.dodam.board.service.BoardService; 
import lombok.RequiredArgsConstructor; 
import org.springframework.web.bind.annotation.*; 
import java.util.List;
@RestController 
@RequestMapping("/boards") 
@RequiredArgsConstructor public class BoardController {
  private final BoardService svc; 
  @GetMapping 
  public List<BoardDto> list(){ return svc.list(); }
}
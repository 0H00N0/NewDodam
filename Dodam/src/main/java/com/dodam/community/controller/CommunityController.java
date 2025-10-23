package com.dodam.community.controller;

import com.dodam.community.dto.CommunityDTO;
import com.dodam.community.service.CommunityService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/board/community")
@RequiredArgsConstructor
public class CommunityController {

	private final CommunityService boardService;

	@GetMapping
	public CommunityDTO.PageResp<CommunityDTO.Resp> list(@RequestParam(name = "bcanum", required = false) Long bcanum,
			@RequestParam(name = "bsnum", required = false) Long bsnum,
			@RequestParam(name = "q", required = false) String q,
			@RequestParam(name = "page", defaultValue = "0") int page,
			@RequestParam(name = "size", defaultValue = "15") int size) {
		Page<CommunityDTO.Resp> p = boardService.search(bcanum, bsnum, q, page, size);
		return CommunityDTO.PageResp.<CommunityDTO.Resp>builder().content(p.getContent())
				.totalElements(p.getTotalElements()).totalPages(p.getTotalPages()).number(p.getNumber())
				.size(p.getSize()).build();
	}

	@GetMapping("/{bnum}")
	public CommunityDTO.Resp get(@PathVariable("bnum") Long bnum) {
		return boardService.get(bnum);
	}

	/** 글쓰기: Security 인증정보로 작성자 보강, bcanum 기본=3, 제목/내용 방어 */
	@PostMapping
	public ResponseEntity<Long> create(@RequestBody CommunityDTO.CreateReq req, Authentication authentication) {
		String mid = authentication.getName();
		req.setMid(mid);

		// ✅ 클라이언트가 mnum/mtnum/mnic을 보내도 무시 (서비스에서 서버값으로 채움)
		req.setMnum(null);
		req.setMtnum(null);
		req.setMnic(null);

		if (req.getBcanum() == null)
			req.setBcanum(3L);

		if (req.getBsub() == null || req.getBsub().isBlank()) {
			req.setBsub("(제목 없음)");
		}
		if (req.getBcontent() == null || req.getBcontent().isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "내용을 입력하세요.");
		}

		Long id = boardService.create(req);
		return ResponseEntity.status(HttpStatus.CREATED).body(id);
	}

	@PutMapping("/{bnum}")
	public void update(@PathVariable("bnum") Long bnum, @RequestBody CommunityDTO.UpdateReq req,
			Authentication authentication) {
		if (authentication == null)
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
		String actorMid = authentication.getName();
		boardService.update(bnum, req, actorMid);
	}

	@DeleteMapping("/{bnum}")
	public void delete(@PathVariable("bnum") Long bnum, Authentication authentication) {
		if (authentication == null)
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
		String actorMid = authentication.getName();
		boardService.delete(bnum, actorMid);
	}

	// ===== Comments =====

	@GetMapping("/{bnum}/comments")
	public java.util.List<CommunityDTO.CommentResp> comments(@PathVariable("bnum") Long bnum) {
		return boardService.listComments(bnum);
	}

	/** 댓글 등록: Security 인증정보로 작성자 보강 */
	@PostMapping("/{bnum}/comments")
	public ResponseEntity<Long> addComment(@PathVariable("bnum") Long bnum,
			@RequestBody CommunityDTO.CommentCreateReq req, Authentication authentication) {
		String mid = authentication.getName();
		req.setMid(mid);

		// ✅ 클라이언트가 보낸 mnic/mnum은 무시
		req.setMnum(null);
		req.setMnic(null);

		Long cid = boardService.addComment(bnum, req);
		return ResponseEntity.status(HttpStatus.CREATED).body(cid);
	}

//===== Comments =====
	@PutMapping("/{bnum}/comments/{conum}")
	public void updateComment(@PathVariable("bnum") Long bnum, @PathVariable("conum") Long conum,
			@RequestBody CommunityDTO.CommentUpdateReq req, Authentication authentication) {
		if (authentication == null)
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
		String actorMid = authentication.getName();
		boardService.updateComment(bnum, conum, req, actorMid);
	}

	@DeleteMapping("/{bnum}/comments/{conum}")
	public void deleteComment(@PathVariable("bnum") Long bnum, @PathVariable("conum") Long conum,
			Authentication authentication) {
		if (authentication == null)
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
		String actorMid = authentication.getName();
		boardService.deleteComment(bnum, conum, actorMid);
	}
}

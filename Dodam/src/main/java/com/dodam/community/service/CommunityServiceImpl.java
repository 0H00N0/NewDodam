// com.dodam.community.service.CommunityServiceImpl
package com.dodam.community.service;

import com.dodam.community.dto.CommunityDTO;
import com.dodam.community.repository.*;
import com.dodam.board.entity.*;
import com.dodam.member.entity.MemberEntity;
import com.dodam.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class CommunityServiceImpl implements CommunityService {
  private final CommunityRepository boardRepo;
  private final CommunityCategoryRepository catRepo;
  private final CommunityStateRepository stateRepo;
  private final CommunityCommentRepository commentRepo;
  private final MemberRepository memberRepo;

  private String currentMid() {
    Authentication a = SecurityContextHolder.getContext().getAuthentication();
    return a != null ? a.getName() : null;
  }

  private CommunityDTO.Resp toResp(BoardEntity e) {
    String viewer = currentMid();

    // 최신 닉네임 우선
    String nickname = e.getMnic();
    if (e.getMnum() != null) {
      var m = memberRepo.findById(e.getMnum()).orElse(null);
      if (m != null && m.getMnic() != null && !m.getMnic().isBlank()) {
        nickname = m.getMnic();
      }
    }

    return CommunityDTO.Resp.builder()
        .bnum(e.getBnum())
        .mnum(e.getMnum())
        .mtnum(e.getMtnum())
        .bcanum(e.getBoardCategory() != null ? e.getBoardCategory().getBcanum() : null)
        .bcaname(e.getBoardCategory() != null ? e.getBoardCategory().getBcaname() : null)
        .bsnum(e.getBoardState() != null ? e.getBoardState().getBsnum() : null)
        .bsname(e.getBoardState() != null ? e.getBoardState().getBsname() : null)
        .bsub(e.getBsub())
        .bcontent(e.getBcontent())
        .mid(e.getMid())
        .mnic(nickname)
        .bdate(e.getBdate())
        .bedate(e.getBedate())
        .mine(viewer != null && viewer.equals(e.getMid())) // ✅ 내가 쓴 글
        .build();
  }

  private CommunityDTO.CommentResp toResp(CommentEntity c){
    String viewer = currentMid();
    return CommunityDTO.CommentResp.builder()
        .conum(c.getConum())
        .bnum(c.getBoard().getBnum())
        .mnum(c.getMnum())
        .mid(c.getMid())
        .mnic(c.getMnic())
        .ccontent(c.getCcontent())
        .cdate(c.getCdate())
        .cedate(c.getCedate())
        .parentConum(c.getParent() != null ? c.getParent().getConum() : null)
        .mine(viewer != null && viewer.equals(c.getMid())) // ✅ 내가 쓴 댓글
        .build();
  }

  @Override
  @Transactional(readOnly = true)
  public Page<CommunityDTO.Resp> search(Long bcanum, Long bsnum, String q, int page, int size) {
    var pageable = PageRequest.of(page, size);
    var normQ = (q == null || q.isBlank()) ? null : q;
    var p = boardRepo.search(bcanum, bsnum, normQ, pageable);
    return p.map(this::toResp);
  }

  @Override @Transactional(readOnly = true)
  public CommunityDTO.Resp get(Long bnum) {
    var e = boardRepo.findById(bnum)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "board not found"));
    return toResp(e);
  }

  /** 글 작성용: mid 기준으로 항상 서버 값으로 덮어씀 */
  private void fillWriterFromMid(CommunityDTO.CreateReq req) {
    String mid = req.getMid();
    if (mid == null || mid.isBlank())
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "login required");

    MemberEntity m = memberRepo.findByMid(mid)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "member not found"));

    req.setMnum(m.getMnum());
    if (m.getMemtype() != null) req.setMtnum(m.getMemtype().getMtnum());
    String nickname = (m.getMnic() != null && !m.getMnic().isBlank()) ? m.getMnic() : m.getMid();
    req.setMnic(nickname);
  }

  /** 댓글 작성용: mid 기준으로 항상 서버 값으로 덮어씀 */
  private void fillWriterFromMid(CommunityDTO.CommentCreateReq req) {
    String mid = req.getMid();
    if (mid == null || mid.isBlank())
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "login required");

    MemberEntity m = memberRepo.findByMid(mid)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "member not found"));

    req.setMnum(m.getMnum());
    String nickname = (m.getMnic() != null && !m.getMnic().isBlank()) ? m.getMnic() : m.getMid();
    req.setMnic(nickname);
  }

  @Override
  public Long create(CommunityDTO.CreateReq req) {
    fillWriterFromMid(req);

    Long bcanum = (req.getBcanum() != null) ? req.getBcanum() : 3L;
    if (req.getBsub() == null || req.getBsub().isBlank()) req.setBsub("(제목 없음)");
    if (req.getBcontent() == null || req.getBcontent().isBlank())
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "내용이 비었습니다.");

    var cat = catRepo.findById(bcanum)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid category"));

    var st = stateRepo.findById(1L) // 1=활성화
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "state missing"));

    var e = BoardEntity.builder()
        .mnum(req.getMnum())
        .mtnum(req.getMtnum())
        .boardCategory(cat)
        .boardState(st)
        .bsub(req.getBsub())
        .bcontent(req.getBcontent())
        .mid(req.getMid())
        .mnic(req.getMnic())
        .build();

    boardRepo.save(e);
    return e.getBnum();
  }

  private void guardAuthor(BoardEntity e, String actorMid) {
    if (actorMid == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    if (!e.getMid().equals(actorMid))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "not author");
  }

  @Override
  public void update(Long bnum, CommunityDTO.UpdateReq req, String actorMid) {
    var e = boardRepo.findById(bnum).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    guardAuthor(e, actorMid);

    if (req.getBcanum() != null) {
      var cat = catRepo.findById(req.getBcanum())
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid category"));
      e.setBoardCategory(cat);
    }
    if (req.getBsnum() != null) {
      var st = stateRepo.findById(req.getBsnum())
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid state"));
      e.setBoardState(st);
    }
    if (req.getBsub() != null) e.setBsub(req.getBsub());
    if (req.getBcontent() != null) e.setBcontent(req.getBcontent());
  }

  @Override
  public void delete(Long bnum, String actorMid) {
    var e = boardRepo.findById(bnum)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    guardAuthor(e, actorMid);

    // 1) 먼저 댓글 전체 삭제
    commentRepo.deleteByBoard(e);

    // 2) 그 다음 게시글 삭제
    boardRepo.delete(e);
  }

  @Override @Transactional(readOnly = true)
  public List<CommunityDTO.CommentResp> listComments(Long bnum) {
    var board = boardRepo.findById(bnum).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    var flat = commentRepo.findByBoardOrderByConumAsc(board)
                .stream().map(this::toResp).toList();

    // 트리 구성
    Map<Long, CommunityDTO.CommentResp> byId = new HashMap<>();
    flat.forEach(c -> byId.put(c.getConum(), c));
    List<CommunityDTO.CommentResp> roots = new ArrayList<>();
    for (var c : flat) {
      if (c.getParentConum() == null) {
        roots.add(c);
      } else {
        var p = byId.get(c.getParentConum());
        if (p != null) {
          // ✅ 항상 가변 컬렉션
          if (p.getChildren() == null) p.setChildren(new ArrayList<>());
          p.getChildren().add(c);
        } else {
          roots.add(c);
        }
      }
    }
    return roots;
  }

  @Override
  public Long addComment(Long bnum, CommunityDTO.CommentCreateReq req) {
    fillWriterFromMid(req);

    var board = boardRepo.findById(bnum).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    CommentEntity parent = null;
    if (req.getParentConum() != null) {
      parent = commentRepo.findById(req.getParentConum())
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "parent comment not found"));
      if (!parent.getBoard().getBnum().equals(bnum))
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "parent belongs to different board");
    }

    var c = CommentEntity.builder()
        .board(board)
        .parent(parent)
        .mnum(req.getMnum())
        .mid(req.getMid())
        .mnic(req.getMnic())
        .ccontent(req.getCcontent())
        .build();
    commentRepo.save(c);
    return c.getConum();
  }

  @Override
  public void updateComment(Long bnum, Long conum, CommunityDTO.CommentUpdateReq req, String actorMid) {
    var c = commentRepo.findById(conum).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    if (!c.getBoard().getBnum().equals(bnum)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    if (actorMid == null || !actorMid.equals(c.getMid())) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    c.setCcontent(req.getCcontent());
  }

  @Override
  public void deleteComment(Long bnum, Long conum, String actorMid) {
    var c = commentRepo.findById(conum).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    if (!c.getBoard().getBnum().equals(bnum)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    if (actorMid == null || !actorMid.equals(c.getMid())) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    commentRepo.delete(c);
  }
}

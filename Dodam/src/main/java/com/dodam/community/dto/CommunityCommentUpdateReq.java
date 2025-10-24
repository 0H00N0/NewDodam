package com.dodam.community.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommunityCommentUpdateReq {

	/** 프런트가 ccontent, content, text 어떤 키로 보내도 수신되게 */
    @NotBlank(message = "내용을 입력하세요.")
    @JsonAlias({ "content", "text" })
    private String ccontent;
}

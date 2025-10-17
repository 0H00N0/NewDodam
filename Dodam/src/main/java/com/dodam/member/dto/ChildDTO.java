package com.dodam.member.dto;

import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.PastOrPresent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public class ChildDTO {
	    private String chname;
	    
	    @PastOrPresent(message = "자녀 생년월일은 미래일 수 없습니다.")
	    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	    private LocalDate chbirth;
	    
	    @AssertTrue(message = "자녀 생년월일은 2000-01-01 이후여야 합니다.")
	    public boolean isChbirthNotTooEarly() {
	        return chbirth == null || !chbirth.isBefore(LocalDate.of(2000, 1, 1));
	    }
	}


package com.dodam.buy.controller;

import com.dodam.buy.dto.BuyDTO;
import com.dodam.buy.service.BuyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/buy")
@RequiredArgsConstructor
public class BuyController {
    private final BuyService buyService;

    @GetMapping("/{buynum}")
    public BuyDTO get(@PathVariable("buynum") Long buynum) {
        return buyService.get(buynum);
    }

    @PostMapping
    public BuyDTO save(@RequestBody BuyDTO dto) {
        return buyService.save(dto);
    }
}
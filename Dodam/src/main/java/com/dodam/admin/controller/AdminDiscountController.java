package com.dodam.admin.controller;

import com.dodam.admin.dto.DiscountDto;
import com.dodam.discount.entity.Discount;
import com.dodam.discount.service.DiscountService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/discounts")
@RequiredArgsConstructor
public class AdminDiscountController {

    private final DiscountService discountService;

    @GetMapping
    public List<DiscountDto.Response> listAll() {
        return discountService.findAll().stream()
                .map(d -> new DiscountDto.Response(
                        d.getDisNum(),
                        d.getDisLevel(),
                        d.getDisValue(),
                        d.getPtermId().getPtermId()
                ))
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public DiscountDto.Response getOne(@PathVariable Long id) {
        Discount d = discountService.findById(id);
        return new DiscountDto.Response(
                d.getDisNum(),
                d.getDisLevel(),
                d.getDisValue(),
                d.getPtermId().getPtermId()
        );
    }

    @PostMapping
    public DiscountDto.Response create(@RequestBody DiscountDto.Request dto) {
        Discount d = discountService.create(dto.getDisLevel(), dto.getDisValue(), dto.getPtermId());
        return new DiscountDto.Response(
                d.getDisNum(),
                d.getDisLevel(),
                d.getDisValue(),
                d.getPtermId().getPtermId()
        );
    }

    @PutMapping("/{id}")
    public DiscountDto.Response update(@PathVariable("id") Long id, @RequestBody DiscountDto.Request dto) {
        Discount d = discountService.update(id, dto.getDisLevel(), dto.getDisValue(), dto.getPtermId());
        return new DiscountDto.Response(
                d.getDisNum(),
                d.getDisLevel(),
                d.getDisValue(),
                d.getPtermId().getPtermId()
        );
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable("id") Long id) {
        discountService.delete(id);
    }
}

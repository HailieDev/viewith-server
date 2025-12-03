package com.puremadeleine.viewith.controller;

import com.puremadeleine.viewith.dto.performance.PerformanceSearchListResDto;
import com.puremadeleine.viewith.service.PerformanceService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/performances")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PerformanceController {

    PerformanceService performanceService;

    @GetMapping("/search")
    public PerformanceSearchListResDto searchPerformance(@RequestParam(name = "venue_id") Long venueId,
                                                         @RequestParam String keyword) {
        return performanceService.searchPerformance(venueId, keyword);
    }
}

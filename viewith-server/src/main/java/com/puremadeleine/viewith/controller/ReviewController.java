package com.puremadeleine.viewith.controller;

import com.puremadeleine.viewith.common.ApiResponse;
import com.puremadeleine.viewith.dto.review.CreateReviewReqDto;
import com.puremadeleine.viewith.dto.review.CreateReviewResDto;
import com.puremadeleine.viewith.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("")
    public ApiResponse<CreateReviewResDto> createReview(@RequestBody CreateReviewReqDto createReviewReqDto) {
        CreateReviewResDto review = reviewService.createReview(createReviewReqDto);
        return ApiResponse.of(review);
    }

}

package com.puremadeleine.viewith.controller;

import com.puremadeleine.viewith.dto.common.SortType;
import com.puremadeleine.viewith.dto.member.MemberInfo;
import com.puremadeleine.viewith.dto.review.request.CreateReviewReqDto;
import com.puremadeleine.viewith.dto.review.request.ReportReviewReqDto;
import com.puremadeleine.viewith.dto.review.request.ReviewListReqDto;
import com.puremadeleine.viewith.dto.review.request.UpdateReviewReqDto;
import com.puremadeleine.viewith.dto.review.response.CreateReviewResDto;
import com.puremadeleine.viewith.dto.review.response.ReviewInfoResDto;
import com.puremadeleine.viewith.dto.review.response.ReviewListResDto;
import com.puremadeleine.viewith.service.ReviewService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/v1/reviews")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReviewController {

    ReviewService reviewService;

    @PostMapping("")
    public CreateReviewResDto createReview(@Valid @RequestPart(value = "CreateReviewReqDto") CreateReviewReqDto createReviewReqDto,
                                           @RequestPart(value = "images", required = false) List<MultipartFile> images,
                                           MemberInfo memberInfo) {
        return reviewService.createReview(createReviewReqDto, images, memberInfo.getMemberId());
    }

    @GetMapping("/{review_id}")
    public ReviewInfoResDto getReview(@PathVariable("review_id") Long reviewId, MemberInfo memberInfo) {
        return reviewService.getReviewInfo(reviewId, memberInfo.getMemberId());
    }

    @PutMapping("/{review_id}")
    public void updateReview(@PathVariable("review_id") Long reviewId,
                             @Valid @RequestBody UpdateReviewReqDto reqDto, MemberInfo memberInfo) {
        reviewService.updateReview(reviewId, reqDto, memberInfo.getMemberId());
    }

    @DeleteMapping("/{review_id}")
    public void deleteReview(@PathVariable("review_id") Long reviewId, MemberInfo memberInfo) {
        reviewService.deleteReview(reviewId, memberInfo.getMemberId());
    }

    @GetMapping("/list")
    public ReviewListResDto getReviewList(
            @RequestParam(value = "page", required = false, defaultValue = "1") @Min(1) Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "10") @Min(1) @Max(20) Integer size,
            @RequestParam(value = "sort_type", required = false, defaultValue = "LATEST") SortType sortType,
            @RequestParam(value = "venue_id", required = false) Long venueId,
            @RequestParam(value = "floor", required = false) String floor,
            @RequestParam(value = "section", required = false) String section,
            @RequestParam(value = "seat_row", required = false) String seatRow,
            @RequestParam(value = "is_summary", required = false, defaultValue = "false") Boolean isSummary) {
        ReviewListReqDto req = ReviewListReqDto.builder()
                .page(page)
                .size(size)
                .sortType(sortType)
                .venueId(venueId)
                .floor(floor)
                .section(section)
                .seatRow(seatRow)
                .build();
        return reviewService.getReviewList(req, isSummary);
    }

    @PostMapping("/{review_id}/report")
    public void reportReview(@PathVariable("review_id") Long reviewId,
                             @Valid @RequestBody ReportReviewReqDto req,
                             MemberInfo memberInfo) {
        reviewService.reportReview(reviewId, req, memberInfo.getMemberId());
    }
}

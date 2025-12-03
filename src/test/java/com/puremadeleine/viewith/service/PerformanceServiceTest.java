package com.puremadeleine.viewith.service;

import com.puremadeleine.viewith.domain.venue.PerformanceEntity;
import com.puremadeleine.viewith.domain.venue.VenueEntity;
import com.puremadeleine.viewith.dto.performance.PerformanceSearchListResDto;
import com.puremadeleine.viewith.provider.PerformanceProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PerformanceServiceTest {

    PerformanceProvider performanceProvider = mock(PerformanceProvider.class);
    PerformanceService.PerformanceServiceMapper mapper =
            Mappers.getMapper(PerformanceService.PerformanceServiceMapper.class);
    PerformanceService performanceService = new PerformanceService(performanceProvider, mapper);

    @DisplayName("search performances successfully")
    @Test
    void search_performances_successfully_test() {
        // given
        Long venueId = 1L;
        String keyword = "더보이즈";
        PerformanceEntity performance = getPerformance();
        when(performanceProvider.search(anyLong(), anyString())).thenReturn(List.of(performance));

        // when
        PerformanceSearchListResDto result = performanceService.searchPerformance(venueId, keyword);

        // then
        assertThat(result.getPerformances()).hasSize(1);
        assertThat(result.getPerformances().getFirst().getPerformanceTitle()).isEqualTo("더보이즈 공연");
    }

    VenueEntity getVenue() {
        return VenueEntity.builder()
                .id(1L)
                .name("venue1")
                .build();
    }

    PerformanceEntity getPerformance() {
        return PerformanceEntity.builder()
                .id(1L)
                .title("더보이즈 공연")
                .artist("더보이즈")
                .venue(getVenue())
                .build();
    }
}

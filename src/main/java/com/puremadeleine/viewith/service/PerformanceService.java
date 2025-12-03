package com.puremadeleine.viewith.service;

import com.puremadeleine.viewith.domain.venue.PerformanceEntity;
import com.puremadeleine.viewith.dto.performance.PerformanceSearchListResDto;
import com.puremadeleine.viewith.provider.PerformanceProvider;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static java.util.Objects.isNull;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PerformanceService {

    PerformanceProvider performanceProvider;
    PerformanceServiceMapper mapper;

    public PerformanceSearchListResDto searchPerformance(Long venueId, String keyword) {
        List<PerformanceEntity> searchList = performanceProvider.search(venueId, keyword);
        var performances = mapper.toPerformanceSearchResDtos(searchList);
        return PerformanceSearchListResDto.builder().performances(performances).build();
    }


    @Mapper(componentModel = "spring")
    public interface PerformanceServiceMapper {

        List<PerformanceSearchListResDto.PerformanceSearchResDto> toPerformanceSearchResDtos(List<PerformanceEntity> performances);

        @Mapping(source = "title", target = "performanceTitle")
        @Mapping(source = "venue.name", target = "venueName")
        @Mapping(source = "startDate", target = "performanceStartDate", qualifiedByName = "localDateToEpochMillis")
        @Mapping(source = "endDate", target = "performanceEndDate", qualifiedByName = "localDateToEpochMillis")
        PerformanceSearchListResDto.PerformanceSearchResDto toPerformanceSearchResDto(PerformanceEntity performance);

        @Named("localDateToEpochMillis")
        static Long localDateToEpochMillis(LocalDate date) {
            return isNull(date) ? null : date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        }
    }
}

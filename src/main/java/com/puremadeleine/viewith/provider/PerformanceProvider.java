package com.puremadeleine.viewith.provider;

import com.puremadeleine.viewith.domain.venue.PerformanceEntity;
import com.puremadeleine.viewith.exception.ViewithException;
import com.puremadeleine.viewith.repository.PerformanceRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.puremadeleine.viewith.exception.ViewithErrorCode.NO_PERFORMANCE;
import static com.puremadeleine.viewith.exception.ViewithErrorCode.NO_VENUE;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PerformanceProvider {

    PerformanceRepository performanceRepository;

    public List<PerformanceEntity> search(Long venueId, String keyword) {
        return performanceRepository.findByVenue_IdAndTitleContainingIgnoreCase(venueId, keyword);
    }

    public List<PerformanceEntity> findTopPerformancesPerVenue(int limit) {
        return performanceRepository.findTopPerformancesPerVenue(limit);
    }

    public List<PerformanceEntity> saveAll(List<PerformanceEntity> performances) {
        return performanceRepository.saveAll(performances);
    }

    public List<String> getExistKopisIds(List<String> kopisIds) {
        return performanceRepository.findByKopisIds(kopisIds);
    }

    public PerformanceEntity getPerformance(Long performanceId) {
        return performanceRepository.findById(performanceId)
            .orElseThrow(() -> new ViewithException(NO_PERFORMANCE, "The performance with ID " + performanceId + " was not found."));
    }
}

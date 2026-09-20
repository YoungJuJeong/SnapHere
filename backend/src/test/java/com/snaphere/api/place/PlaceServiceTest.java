package com.snaphere.api.place;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlaceServiceTest {

    @Mock private PlaceRepository places;
    @Mock private GoogleGeocodingClient geocoder;
    @Mock private TourPlaceDetailClient details;
    @Mock private ViewCounterService views;
    @Mock private RecentPlaceService recentPlaces;
    @Mock private PlaceReadCache cache;

    private PlaceService service;

    @BeforeEach
    void setUp() {
        service = new PlaceService(places, geocoder, details, views, recentPlaces, cache);
    }

    @Test
    @DisplayName("관광 API가 실패해도 내부 장소 기본 정보로 상세를 제공한다")
    void detailFallsBackToStoredPlaceWhenTourApiFails() {
        var place = new PlaceRepository.PlaceRecord(
                7L, "TOURIST", "tour-7", "전주시청년축제", 37, 12, 500, 10);
        var summary = new PlaceDtos.PlaceSummary(
                "plc_7", "TOURIST", "전주시청년축제", "전주시 덕진구", null,
                null, null, 3, 2, null, null, null);

        when(places.placeRecord(7L)).thenReturn(place);
        when(places.hasDetail(7L, "ko")).thenReturn(false);
        when(details.load("tour-7", "ko")).thenThrow(new IllegalStateException("tour api down"));
        when(places.detail(7L, "ko"))
                .thenReturn(new PlaceRepository.DetailRecord(null, null, null, 500, 10));
        when(cache.detail(7L, "ko")).thenReturn(Optional.empty());
        when(places.summary(7L, null)).thenReturn(summary);
        when(places.posts(7L, null, 12, null)).thenReturn(List.of());
        when(views.pending(7L)).thenReturn(0L);

        PlaceDtos.PlaceDetail result = service.detail("plc_7", "ko", null);

        assertThat(result.place()).isEqualTo(summary);
        assertThat(result.overview()).isNull();
        assertThat(result.viewCount()).isEqualTo(11);
        verify(places, never()).upsertDetail(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any());
        verify(views).increment(7L);
    }
}

package com.saigonplantravel.backend.assistant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.saigonplantravel.backend.ai.client.AiAssistantClient;
import com.saigonplantravel.backend.ai.client.dto.AiAssistantRequest;
import com.saigonplantravel.backend.ai.client.dto.AiAssistantResponse;
import com.saigonplantravel.backend.assistant.dto.AssistantMessageRequest;
import com.saigonplantravel.backend.assistant.dto.AssistantMessageResponse;
import com.saigonplantravel.backend.itinerary.dto.ItineraryDetailItemResponse;
import com.saigonplantravel.backend.itinerary.dto.ItineraryDetailResponse;
import com.saigonplantravel.backend.itinerary.dto.ItineraryPlaceResponse;
import com.saigonplantravel.backend.itinerary.dto.ItineraryScheduleResponse;
import com.saigonplantravel.backend.itinerary.dto.ItinerarySummaryResponse;
import com.saigonplantravel.backend.itinerary.service.ItineraryService;
import com.saigonplantravel.backend.place.entity.Place;
import com.saigonplantravel.backend.place.service.PlaceQueryService;
import com.saigonplantravel.backend.trip.entity.Trip;
import com.saigonplantravel.backend.trip.exception.TripNotFoundException;
import com.saigonplantravel.backend.trip.service.TripQueryService;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TripAssistantServiceTest {

    @Mock
    private TripQueryService tripQueryService;

    @Mock
    private ItineraryService itineraryService;

    @Mock
    private AiAssistantClient aiAssistantClient;

    @Mock
    private PlaceQueryService placeQueryService;

    @Mock
    private Trip ownedTrip;

    private TripAssistantService service;

    @BeforeEach
    void setUp() {
        service = new TripAssistantService(tripQueryService, itineraryService, aiAssistantClient, placeQueryService);
    }

    @Test
    void mapsOwnedTripCurrentItineraryExclusionsAndHistoryToAiRequest() {
        Long userId = 7L;
        UUID tripPublicId = UUID.randomUUID();
        when(tripQueryService.findOwnedTrip(userId, tripPublicId)).thenReturn(ownedTrip);
        when(itineraryService.getItinerary(userId, tripPublicId)).thenReturn(itinerary(tripPublicId));
        when(aiAssistantClient.sendMessage(any()))
                .thenReturn(new AiAssistantResponse("Câu trả lời", List.of()));
        AssistantMessageRequest request = new AssistantMessageRequest(
                "  Tôi thích lịch sử  ",
                List.of(new AssistantMessageRequest.ConversationMessage(
                        AssistantMessageRequest.Role.USER, "  Ưu tiên bảo tàng  ")));

        service.sendMessage(userId, tripPublicId, request);

        ArgumentCaptor<AiAssistantRequest> captor = ArgumentCaptor.forClass(AiAssistantRequest.class);
        verify(aiAssistantClient).sendMessage(captor.capture());
        AiAssistantRequest mapped = captor.getValue();
        assertThat(mapped.message()).isEqualTo("Tôi thích lịch sử");
        assertThat(mapped.history())
                .containsExactly(new AiAssistantRequest.ConversationMessage("USER", "Ưu tiên bảo tàng"));
        assertThat(mapped.excludedPlaceSlugs()).containsExactly("dinh-doc-lap");
    }

    @Test
    void sendsEmptyExclusionSetWhenNoItineraryExists() {
        Long userId = 7L;
        UUID tripPublicId = UUID.randomUUID();
        when(tripQueryService.findOwnedTrip(userId, tripPublicId)).thenReturn(ownedTrip);
        when(itineraryService.getItinerary(userId, tripPublicId)).thenReturn(emptyItinerary(tripPublicId));
        when(aiAssistantClient.sendMessage(any()))
                .thenReturn(new AiAssistantResponse("Câu trả lời", List.of()));

        service.sendMessage(userId, tripPublicId, request());

        ArgumentCaptor<AiAssistantRequest> captor = ArgumentCaptor.forClass(AiAssistantRequest.class);
        verify(aiAssistantClient).sendMessage(captor.capture());
        assertThat(captor.getValue().excludedPlaceSlugs()).isEmpty();
    }

    @Test
    void stopsWhenTripIsNotOwned() {
        Long userId = 7L;
        UUID tripPublicId = UUID.randomUUID();
        when(tripQueryService.findOwnedTrip(userId, tripPublicId)).thenThrow(new TripNotFoundException());

        assertThatThrownBy(() -> service.sendMessage(userId, tripPublicId, request()))
                .isInstanceOf(TripNotFoundException.class);

        verifyNoInteractions(itineraryService, aiAssistantClient, placeQueryService);
    }

    @Test
    void resolvesOnlyExistingActiveRecommendations() {
        Long userId = 7L;
        UUID tripPublicId = UUID.randomUUID();
        when(tripQueryService.findOwnedTrip(userId, tripPublicId)).thenReturn(ownedTrip);
        when(itineraryService.getItinerary(userId, tripPublicId)).thenReturn(itinerary(tripPublicId));

        AiAssistantResponse aiResponse = new AiAssistantResponse(
                "Bạn có thể cân nhắc bảo tàng.",
                List.of(
                        new AiAssistantResponse.SuggestedPlace("valid-place", "  Phù hợp lịch sử.  "),
                        new AiAssistantResponse.SuggestedPlace("dinh-doc-lap", "Đã có trong hành trình"),
                        new AiAssistantResponse.SuggestedPlace("unknown-place", "Không tồn tại"),
                        new AiAssistantResponse.SuggestedPlace("inactive-place", "Đã tắt"),
                        new AiAssistantResponse.SuggestedPlace("valid-place", "Lý do trùng")));
        when(aiAssistantClient.sendMessage(any())).thenReturn(aiResponse);

        Place valid = place("Địa điểm hợp lệ", "valid-place");
        ReflectionTestUtils.setField(valid, "id", 42L);
        Place inactive = place("Địa điểm ngừng hoạt động", "inactive-place");
        inactive.deactivate();
        when(placeQueryService.findAllActiveBySlugsForScheduling(any())).thenReturn(List.of(valid, inactive));

        AssistantMessageResponse response = service.sendMessage(userId, tripPublicId, request());

        assertThat(response.suggestedPlaces())
                .containsExactly(new AssistantMessageResponse.SuggestedPlace(
                        42L, "valid-place", "Địa điểm hợp lệ", null, "Phù hợp lịch sử."));
        verify(placeQueryService)
                .findAllActiveBySlugsForScheduling(org.mockito.ArgumentMatchers.argThat(
                        slugs -> !slugs.contains("dinh-doc-lap")));
    }

    private AssistantMessageRequest request() {
        return new AssistantMessageRequest("Tôi thích lịch sử", List.of());
    }

    private Place place(String name, String slug) {
        return new Place(
                name,
                slug,
                "Quận 1",
                new BigDecimal("10.7800000"),
                new BigDecimal("106.6900000"),
                90,
                BigDecimal.ZERO,
                new BigDecimal("100000"),
                true);
    }

    private ItineraryDetailResponse itinerary(UUID tripPublicId) {
        ItineraryPlaceResponse place = new ItineraryPlaceResponse(
                "dinh-doc-lap", "Dinh Độc Lập", new BigDecimal("10.7770000"), new BigDecimal("106.6950000"));
        ItineraryScheduleResponse schedule = new ItineraryScheduleResponse(
                LocalTime.of(8, 15), LocalTime.of(8, 15), LocalTime.of(9, 45), 15, 1.0, BigDecimal.ZERO);
        return new ItineraryDetailResponse(
                UUID.randomUUID(),
                tripPublicId,
                List.of(new ItineraryDetailItemResponse(UUID.randomUUID(), 1, place, schedule)),
                new ItinerarySummaryResponse(BigDecimal.ZERO, 15, 90, 1.0),
                List.of());
    }

    private ItineraryDetailResponse emptyItinerary(UUID tripPublicId) {
        return new ItineraryDetailResponse(
                null, tripPublicId, List.of(), new ItinerarySummaryResponse(BigDecimal.ZERO, 0, 0, 0.0), List.of());
    }
}

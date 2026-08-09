package com.saigonplantravel.backend.scheduling.service;

import com.saigonplantravel.backend.place.scheduling.PlaceSchedulingCandidate;
import com.saigonplantravel.backend.place.scheduling.PlaceSchedulingQuery;
import com.saigonplantravel.backend.scheduling.domain.SchedulingPolicy;
import com.saigonplantravel.backend.scheduling.domain.algorithm.CandidateRejectionReason;
import com.saigonplantravel.backend.scheduling.domain.algorithm.GreedyItineraryScheduler;
import com.saigonplantravel.backend.scheduling.domain.algorithm.GreedySchedulingOutcome;
import com.saigonplantravel.backend.scheduling.domain.algorithm.SchedulingInput;
import com.saigonplantravel.backend.scheduling.dto.ItineraryResponse;
import com.saigonplantravel.backend.scheduling.dto.RejectionSummary;
import com.saigonplantravel.backend.scheduling.entity.Itinerary;
import com.saigonplantravel.backend.scheduling.exception.ItineraryNotFoundException;
import com.saigonplantravel.backend.scheduling.exception.NoFeasibleItineraryException;
import com.saigonplantravel.backend.scheduling.exception.SchedulingDataConflictException;
import com.saigonplantravel.backend.scheduling.mapper.ItineraryMapper;
import com.saigonplantravel.backend.scheduling.repository.ItineraryRepository;
import com.saigonplantravel.backend.trip.service.TripSchedulingQuery;
import com.saigonplantravel.backend.trip.service.TripSchedulingSnapshot;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class SchedulingService {

    private final TripSchedulingQuery tripSchedulingQuery;
    private final PlaceSchedulingQuery placeSchedulingQuery;
    private final GreedyItineraryScheduler scheduler;
    private final SchedulingPolicy schedulingPolicy;
    private final ItineraryRepository itineraryRepository;
    private final ItineraryMapper itineraryMapper;
    private final Clock clock;

    public SchedulingService(
            TripSchedulingQuery tripSchedulingQuery,
            PlaceSchedulingQuery placeSchedulingQuery,
            GreedyItineraryScheduler scheduler,
            SchedulingPolicy schedulingPolicy,
            ItineraryRepository itineraryRepository,
            ItineraryMapper itineraryMapper,
            Clock clock
    ) {
        this.tripSchedulingQuery = tripSchedulingQuery;
        this.placeSchedulingQuery = placeSchedulingQuery;
        this.scheduler = scheduler;
        this.schedulingPolicy = schedulingPolicy;
        this.itineraryRepository = itineraryRepository;
        this.itineraryMapper = itineraryMapper;
        this.clock = clock;
    }

    @Transactional
    public ItineraryResponse generate(
            Long userId,
            UUID tripPublicId
    ) {
        TripSchedulingSnapshot trip = loadTrip(
                tripPublicId,
                userId
        );
        List<PlaceSchedulingCandidate> candidates =
                loadCandidates(trip);

        if (candidates.size() > schedulingPolicy.maxCandidates()) {
            throw new SchedulingDataConflictException(
                    "Candidate pool exceeds the configured maximum of "
                            + schedulingPolicy.maxCandidates()
            );
        }

        SchedulingInput input;
        GreedySchedulingOutcome outcome;
        try {
            input = new SchedulingInput(
                    trip,
                    candidates,
                    schedulingPolicy,
                    OffsetDateTime.now(clock)
            );
            outcome = scheduler.schedule(input);
        } catch (IllegalArgumentException
                 | IllegalStateException
                 | NullPointerException exception) {
            throw new SchedulingDataConflictException(
                    "Trip or place scheduling snapshot is inconsistent",
                    exception
            );
        }

        if (outcome.scheduledCandidates().isEmpty()) {
            throw new NoFeasibleItineraryException(
                    summarizeRejections(outcome)
            );
        }

        Itinerary itinerary = Itinerary.create(
                userId,
                input,
                outcome
        );
        Itinerary saved = itineraryRepository.save(itinerary);
        return itineraryMapper.toResponse(saved, false);
    }

    @Transactional(readOnly = true)
    public ItineraryResponse get(
            Long userId,
            UUID itineraryPublicId
    ) {
        Itinerary itinerary = itineraryRepository
                .findByPublicIdAndUserId(
                        itineraryPublicId,
                        userId
                )
                .orElseThrow(ItineraryNotFoundException::new);
        TripSchedulingSnapshot currentTrip = loadTrip(
                itinerary.getTripPublicIdSnapshot(),
                userId
        );
        boolean stale = currentTrip.updatedAt().isAfter(
                itinerary.getTripUpdatedAtSnapshot()
        );
        return itineraryMapper.toResponse(itinerary, stale);
    }

    private List<PlaceSchedulingCandidate> loadCandidates(
            TripSchedulingSnapshot trip
    ) {
        try {
            return placeSchedulingQuery.findCandidates(
                    trip.preferredCategoryIds(),
                    trip.tripDate()
            );
        } catch (IllegalArgumentException
                 | IllegalStateException
                 | NullPointerException exception) {
            throw new SchedulingDataConflictException(
                    "Place scheduling snapshot is inconsistent",
                    exception
            );
        }
    }

    private TripSchedulingSnapshot loadTrip(
            UUID tripPublicId,
            Long userId
    ) {
        try {
            return tripSchedulingQuery.getByPublicId(
                    tripPublicId,
                    userId
            );
        } catch (IllegalArgumentException
                 | IllegalStateException
                 | NullPointerException exception) {
            throw new SchedulingDataConflictException(
                    "Trip scheduling snapshot is inconsistent",
                    exception
            );
        }
    }

    private RejectionSummary summarizeRejections(
            GreedySchedulingOutcome outcome
    ) {
        int environment = 0;
        int budget = 0;
        int closedHours = 0;
        int tripWindow = 0;
        int closingTime = 0;

        for (var rejection : outcome.terminalRejections()) {
            CandidateRejectionReason reason = rejection.reason();
            switch (reason) {
                case ENVIRONMENT -> environment++;
                case BUDGET -> budget++;
                case CLOSED_HOURS -> closedHours++;
                case TRIP_WINDOW -> tripWindow++;
                case CLOSING_TIME -> closingTime++;
            }
        }

        return new RejectionSummary(
                outcome.candidatePoolSize(),
                environment,
                budget,
                closedHours,
                tripWindow,
                closingTime
        );
    }
}

package com.saigonplantravel.backend.scheduling.domain.visit;

import com.saigonplantravel.backend.place.scheduling.OpeningHoursSnapshot;

import java.time.LocalDateTime;
import java.util.Objects;

public final class VisitFeasibilityEvaluator {

    public VisitFeasibilityResult evaluate(
            VisitFeasibilityInput input
    ) {
        Objects.requireNonNull(
                input,
                "input must not be null"
        );

        LocalDateTime arrivalAt =
                input.departureAt()
                        .plusMinutes(
                                input.travelMinutes()
                        );

        return switch (
                input.openingHours().status()
                ) {
            case CLOSED ->
                    VisitFeasibilityResult.infeasible(
                            VisitRejectionReason.PLACE_CLOSED
                    );

            case UNKNOWN ->
                    evaluateUnknownOpeningHours(
                            input,
                            arrivalAt
                    );

            case KNOWN_OPEN ->
                    evaluateKnownOpeningHours(
                            input,
                            arrivalAt
                    );
        };
    }

    private static VisitFeasibilityResult
    evaluateUnknownOpeningHours(
            VisitFeasibilityInput input,
            LocalDateTime arrivalAt
    ) {
        LocalDateTime visitStartAt =
                arrivalAt;

        LocalDateTime visitEndAt =
                visitStartAt.plusMinutes(
                        input.visitMinutes()
                );

        if (visitEndAt.isAfter(
                input.tripEndAt()
        )) {
            return VisitFeasibilityResult.infeasible(
                    VisitRejectionReason
                            .TRIP_END_TIME_EXCEEDED
            );
        }

        return createFeasibleResult(
                input.departureAt(),
                arrivalAt,
                visitStartAt,
                visitEndAt
        );
    }

    private static VisitFeasibilityResult
    evaluateKnownOpeningHours(
            VisitFeasibilityInput input,
            LocalDateTime arrivalAt
    ) {
        OpeningHoursSnapshot openingHours =
                input.openingHours();

        LocalDateTime openingAt =
                LocalDateTime.of(
                        input.departureAt()
                                .toLocalDate(),
                        openingHours.openTime()
                );

        LocalDateTime closingAt =
                LocalDateTime.of(
                        input.departureAt()
                                .toLocalDate(),
                        openingHours.closeTime()
                );

        LocalDateTime visitStartAt =
                arrivalAt.isBefore(openingAt)
                        ? openingAt
                        : arrivalAt;

        LocalDateTime visitEndAt =
                visitStartAt.plusMinutes(
                        input.visitMinutes()
                );

        VisitRejectionReason rejectionReason =
                findDeadlineViolation(
                        visitEndAt,
                        closingAt,
                        input.tripEndAt()
                );

        if (rejectionReason != null) {
            return VisitFeasibilityResult.infeasible(
                    rejectionReason
            );
        }

        return createFeasibleResult(
                input.departureAt(),
                arrivalAt,
                visitStartAt,
                visitEndAt
        );
    }

    private static VisitRejectionReason
    findDeadlineViolation(
            LocalDateTime visitEndAt,
            LocalDateTime closingAt,
            LocalDateTime tripEndAt
    ) {
        /*
         * Xác định giới hạn kết thúc sớm hơn:
         *
         * - địa điểm đóng cửa;
         * - hoặc chuyến đi kết thúc.
         */
        if (closingAt.isBefore(tripEndAt)) {
            if (visitEndAt.isAfter(closingAt)) {
                return VisitRejectionReason
                        .PLACE_CLOSING_TIME_EXCEEDED;
            }

            return null;
        }

        if (visitEndAt.isAfter(tripEndAt)) {
            return VisitRejectionReason
                    .TRIP_END_TIME_EXCEEDED;
        }

        return null;
    }

    private static VisitFeasibilityResult
    createFeasibleResult(
            LocalDateTime departureAt,
            LocalDateTime arrivalAt,
            LocalDateTime visitStartAt,
            LocalDateTime visitEndAt
    ) {
        VisitSchedule schedule =
                new VisitSchedule(
                        departureAt,
                        arrivalAt,
                        visitStartAt,
                        visitEndAt
                );

        return VisitFeasibilityResult.feasible(
                schedule
        );
    }
}
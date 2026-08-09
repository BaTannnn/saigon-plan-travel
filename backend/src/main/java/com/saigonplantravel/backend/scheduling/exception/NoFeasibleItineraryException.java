package com.saigonplantravel.backend.scheduling.exception;

import com.saigonplantravel.backend.scheduling.dto.RejectionSummary;

public class NoFeasibleItineraryException extends RuntimeException {

    private final RejectionSummary rejectionSummary;

    public NoFeasibleItineraryException(
            RejectionSummary rejectionSummary
    ) {
        super("No place can be scheduled within the current trip constraints");
        this.rejectionSummary = rejectionSummary;
    }

    public RejectionSummary getRejectionSummary() {
        return rejectionSummary;
    }
}

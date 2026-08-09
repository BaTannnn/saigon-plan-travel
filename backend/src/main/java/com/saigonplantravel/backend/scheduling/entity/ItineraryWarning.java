package com.saigonplantravel.backend.scheduling.entity;

import com.saigonplantravel.backend.scheduling.domain.ItineraryWarningCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "itinerary_warnings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItineraryWarning {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "itinerary_id", nullable = false, updatable = false)
    @Getter(AccessLevel.NONE)
    private Itinerary itinerary;

    @Column(name = "item_sequence", updatable = false)
    private Integer itemSequence;

    @Enumerated(EnumType.STRING)
    @Column(name = "code", nullable = false, length = 60, updatable = false)
    private ItineraryWarningCode code;

    @Column(name = "message", nullable = false, length = 500, updatable = false)
    private String message;

    @Column(name = "sort_order", nullable = false, updatable = false)
    private int sortOrder;

    ItineraryWarning(
            Itinerary itinerary,
            Integer itemSequence,
            ItineraryWarningCode code,
            String message,
            int sortOrder
    ) {
        this.itinerary = itinerary;
        this.itemSequence = itemSequence;
        this.code = code;
        this.message = message;
        this.sortOrder = sortOrder;
    }
}

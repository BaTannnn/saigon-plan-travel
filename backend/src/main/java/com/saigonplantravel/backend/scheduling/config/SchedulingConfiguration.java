package com.saigonplantravel.backend.scheduling.config;

import com.saigonplantravel.backend.scheduling.domain.*;
import com.saigonplantravel.backend.scheduling.domain.algorithm.GreedyItineraryScheduler;
import com.saigonplantravel.backend.scheduling.domain.scoring.CandidateRanker;
import com.saigonplantravel.backend.scheduling.domain.scoring.CandidateScorer;
import com.saigonplantravel.backend.scheduling.domain.travel.DistanceCalculator;
import com.saigonplantravel.backend.scheduling.domain.travel.HaversineDistanceCalculator;
import com.saigonplantravel.backend.scheduling.domain.travel.TravelTimeEstimator;
import com.saigonplantravel.backend.scheduling.domain.visit.PaceDurationPolicy;
import com.saigonplantravel.backend.scheduling.domain.visit.VisitFeasibilityEvaluator;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(
        proxyBeanMethods = false
)
@EnableConfigurationProperties(
        SchedulingProperties.class
)
public class SchedulingConfiguration {

    @Bean
    public SchedulingPolicy schedulingPolicy(
            SchedulingProperties properties
    ) {
        SchedulingAlgorithmVersion algorithmVersion =
                SchedulingAlgorithmVersion.valueOf(
                        properties.algorithmVersion()
                );

        return new SchedulingPolicy(
                algorithmVersion,
                properties.averageSpeedKmh(),
                properties.fixedTransferMinutes(),
                properties.maxCandidates()
        );
    }
    @Bean
    public DistanceCalculator distanceCalculator() {
        return new HaversineDistanceCalculator();
    }

    @Bean
    public TravelTimeEstimator travelTimeEstimator(
            DistanceCalculator distanceCalculator,
            SchedulingPolicy schedulingPolicy
    ) {
        return new TravelTimeEstimator(
                distanceCalculator,
                schedulingPolicy
        );
    }
    @Bean
    public PaceDurationPolicy paceDurationPolicy() {
        return new PaceDurationPolicy();
    }
    @Bean
    public VisitFeasibilityEvaluator
    visitFeasibilityEvaluator() {
        return new VisitFeasibilityEvaluator();
    }
    @Bean
    public CandidateScorer candidateScorer() {
        return new CandidateScorer();
    }
    @Bean
    public CandidateRanker candidateRanker() {
        return new CandidateRanker();
    }
    @Bean
    public GreedyItineraryScheduler
    greedyItineraryScheduler(
            TravelTimeEstimator travelTimeEstimator,
            PaceDurationPolicy paceDurationPolicy,
            VisitFeasibilityEvaluator visitFeasibilityEvaluator,
            CandidateScorer candidateScorer,
            CandidateRanker candidateRanker
    ) {
        return new GreedyItineraryScheduler(
                travelTimeEstimator,
                paceDurationPolicy,
                visitFeasibilityEvaluator,
                candidateScorer,
                candidateRanker
        );
    }
}
package com.saigonplantravel.backend.scheduling.config;

import com.saigonplantravel.backend.scheduling.domain.SchedulingAlgorithmVersion;
import com.saigonplantravel.backend.scheduling.domain.SchedulingPolicy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.saigonplantravel.backend.scheduling.domain.DistanceCalculator;
import com.saigonplantravel.backend.scheduling.domain.HaversineDistanceCalculator;
import com.saigonplantravel.backend.scheduling.domain.TravelTimeEstimator;
import com.saigonplantravel.backend.scheduling.domain.PaceDurationPolicy;
import com.saigonplantravel.backend.scheduling.domain.VisitFeasibilityEvaluator;

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
}
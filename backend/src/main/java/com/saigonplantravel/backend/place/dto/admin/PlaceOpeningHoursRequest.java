package com.saigonplantravel.backend.place.dto.admin;

import com.saigonplantravel.backend.place.dto.OpeningHourState;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.format.annotation.DateTimeFormat;

public class PlaceOpeningHoursRequest {

    @NotNull
    @Size(min = 7, max = 7)
    private List<@Valid Day> days = new ArrayList<>();

    public PlaceOpeningHoursRequest() {}

    public PlaceOpeningHoursRequest(List<Day> days) {
        this.days = new ArrayList<>(days);
    }

    public List<Day> getDays() {
        return days;
    }

    public void setDays(List<Day> days) {
        this.days = days;
    }

    @AssertTrue(message = "must contain each ISO day from 1 through 7 exactly once")
    public boolean isCompleteWeek() {
        if (days == null || days.size() != 7 || days.stream().anyMatch(day -> day == null)) {
            return false;
        }
        Set<Short> submittedDays = days.stream().map(Day::getDayOfWeek).collect(Collectors.toSet());
        return submittedDays.equals(
                Set.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6, (short) 7));
    }

    public static class Day {

        @NotNull
        @Min(1)
        @Max(7)
        private Short dayOfWeek;

        @NotNull
        private OpeningHourState state;

        @DateTimeFormat(pattern = "HH:mm")
        private LocalTime openTime;

        @DateTimeFormat(pattern = "HH:mm")
        private LocalTime closeTime;

        public Day() {}

        public Day(Short dayOfWeek, OpeningHourState state, LocalTime openTime, LocalTime closeTime) {
            this.dayOfWeek = dayOfWeek;
            this.state = state;
            this.openTime = openTime;
            this.closeTime = closeTime;
        }

        public Short getDayOfWeek() {
            return dayOfWeek;
        }

        public void setDayOfWeek(Short dayOfWeek) {
            this.dayOfWeek = dayOfWeek;
        }

        public OpeningHourState getState() {
            return state;
        }

        public void setState(OpeningHourState state) {
            this.state = state;
        }

        public LocalTime getOpenTime() {
            return openTime;
        }

        public void setOpenTime(LocalTime openTime) {
            this.openTime = openTime;
        }

        public LocalTime getCloseTime() {
            return closeTime;
        }

        public void setCloseTime(LocalTime closeTime) {
            this.closeTime = closeTime;
        }

        @AssertTrue(message = "open days require both times and opening time must be before closing time")
        public boolean isValidTimeRange() {
            if (state != OpeningHourState.OPEN) {
                return true;
            }
            return openTime != null && closeTime != null && openTime.isBefore(closeTime);
        }

        public String getDayLabel() {
            if (dayOfWeek == null) {
                return "Unknown day";
            }
            return switch (dayOfWeek) {
                case 1 -> "Monday";
                case 2 -> "Tuesday";
                case 3 -> "Wednesday";
                case 4 -> "Thursday";
                case 5 -> "Friday";
                case 6 -> "Saturday";
                case 7 -> "Sunday";
                default -> "Unknown day";
            };
        }
    }
}

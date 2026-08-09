package com.bikepooling.service;

import com.bikepooling.entity.ScheduledRideApplicationDay;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
public class ScheduledDaysUnavailableEvent {
    private Long bookerId;
    private String bookerName;
    private List<LocalDate> dates;
    private boolean unavailable;
    private List<ScheduledRideApplicationDay> unavailableDays;

    public ScheduledDaysUnavailableEvent(List<ScheduledRideApplicationDay> unavailableDays) {
        this.unavailableDays = unavailableDays;
    }

    public ScheduledDaysUnavailableEvent(Long bookerId, String bookerName, List<LocalDate> dates, boolean unavailable) {
        this.bookerId = bookerId;
        this.bookerName = bookerName;
        this.dates = dates;
        this.unavailable = unavailable;
    }
}

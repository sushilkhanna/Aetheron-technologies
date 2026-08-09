package com.bikepooling.service;

import com.bikepooling.entity.ScheduledRideApplicationDay;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
public class ScheduledDaysConfirmedEvent {
    private Long bookerId;
    private String bookerName;
    private List<LocalDate> dates;
    private List<ScheduledRideApplicationDay> confirmedDays;

    public ScheduledDaysConfirmedEvent(List<ScheduledRideApplicationDay> confirmedDays) {
        this.confirmedDays = confirmedDays;
    }

    public ScheduledDaysConfirmedEvent(Long bookerId, String bookerName, List<LocalDate> dates) {
        this.bookerId = bookerId;
        this.bookerName = bookerName;
        this.dates = dates;
    }
}

package com.bikepooling.service;

import com.bikepooling.entity.ScheduledRideApplicationDay;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
public class ScheduledDaysRejectedEvent {
    private Long bookerId;
    private String bookerName;
    private List<LocalDate> dates;
    private List<ScheduledRideApplicationDay> rejectedDays;

    public ScheduledDaysRejectedEvent(List<ScheduledRideApplicationDay> rejectedDays) {
        this.rejectedDays = rejectedDays;
    }

    public ScheduledDaysRejectedEvent(Long bookerId, String bookerName, List<LocalDate> dates) {
        this.bookerId = bookerId;
        this.bookerName = bookerName;
        this.dates = dates;
    }
}

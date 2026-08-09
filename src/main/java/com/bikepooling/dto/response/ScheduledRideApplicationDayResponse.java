package com.bikepooling.dto.response;

import com.bikepooling.entity.ScheduledRideApplicationDay;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ScheduledRideApplicationDayResponse {

    private Long applicationDayId;
    private Long applicationId;
    private Long instanceId;
    private LocalDate rideDate;
    private Long bookerId;
    private String bookerName;
    private String note;
    private String status;

    public static ScheduledRideApplicationDayResponse from(ScheduledRideApplicationDay d) {
        return ScheduledRideApplicationDayResponse.builder()
                .applicationDayId(d.getId())
                .applicationId(d.getApplication().getId())
                .instanceId(d.getInstance().getId())
                .rideDate(d.getInstance().getRideDate())
                .bookerId(d.getApplication().getBooker().getId())
                .bookerName(d.getApplication().getBooker().getFullName())
                .note(d.getApplication().getNote())
                .status(d.getStatus().name())
                .build();
    }
}
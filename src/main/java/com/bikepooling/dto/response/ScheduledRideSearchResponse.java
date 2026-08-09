package com.bikepooling.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Wrapper for scheduled-ride search results, grouped by driver/template.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ScheduledRideSearchResponse {

    /** Rides grouped by Driver/Template that match the booker's requested dates + route. */
    private List<ScheduledRideSearchResultItem> exactMatches;

    /** Rides grouped by Driver/Template from other templates that cover uncovered dates. */
    private List<ScheduledRideSearchResultItem> suggestions;

    /** Dates from the booker's request that have no matching rides at all. */
    private Set<LocalDate> uncoveredDates;
}

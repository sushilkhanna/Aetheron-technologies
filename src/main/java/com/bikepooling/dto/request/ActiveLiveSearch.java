package com.bikepooling.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Stored in Redis under key: live:search:{bookerId}
 *
 * Created when booker taps "Start Search".
 * TTL = 3 minutes (180 seconds) — Redis auto-expires it.
 *
 * We track which rides were already notified so we don't
 * spam the same driver twice in one search session.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActiveLiveSearch {

    private Long   bookerId;

    private double pickupLat;
    private double pickupLng;
    private double dropLat;
    private double dropLng;

    private String pickupName;
    private String dropName;

    // comma-separated rideIds already notified in this session
    // e.g. "101,204,389"  — avoids Set<Long> serialisation issues
    private String notifiedRideIds;

    private long   createdAt;   // epoch millis
    private long   expiresAt;   // epoch millis (createdAt + 180_000)
}
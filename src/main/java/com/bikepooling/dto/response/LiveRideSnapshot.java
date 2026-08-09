package com.bikepooling.dto.response;

import com.bikepooling.dto.request.LiveRideLocation;
import com.bikepooling.dto.request.LiveRideMeta;
import lombok.Getter;

/**
 * Read-only merge of LiveRideMeta + LiveRideLocation for consumers that need
 * both (admin map, live-search matching). Never persisted directly — meta and
 * location are always stored and updated as two separate Redis keys.
 */
@Getter
public class LiveRideSnapshot {

    private final LiveRideMeta meta;
    private final LiveRideLocation location;

    private LiveRideSnapshot(LiveRideMeta meta, LiveRideLocation location) {
        this.meta = meta;
        this.location = location;
    }

    public static LiveRideSnapshot of(LiveRideMeta meta, LiveRideLocation location) {
        return new LiveRideSnapshot(meta, location);
    }

    public Long getRideId()   { return meta.getRideId(); }
    public Long getDriverId() { return meta.getDriverId(); }

    /** Falls back to the go-live seed point if no location ping has arrived yet. */
    public double getCurrentLat() {
        return location != null ? location.getCurrentLat() : meta.getGoLiveLat();
    }
    public double getCurrentLng() {
        return location != null ? location.getCurrentLng() : meta.getGoLiveLng();
    }
    public long getLastUpdatedAt() {
        return location != null ? location.getLastUpdatedAt() : meta.getGoLiveAt();
    }
}
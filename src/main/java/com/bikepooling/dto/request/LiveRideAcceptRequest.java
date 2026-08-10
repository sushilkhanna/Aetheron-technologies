package com.bikepooling.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LiveRideAcceptRequest {

    @NotNull
    private Long searchRequestId;
}

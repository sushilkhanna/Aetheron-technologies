package com.bikepooling.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ApplicationDaysRequest {

    @NotEmpty(message = "Select at least one application day")
    private List<Long> applicationDayIds;
}

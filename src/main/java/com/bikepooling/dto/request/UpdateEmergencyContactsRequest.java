package com.bikepooling.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateEmergencyContactsRequest {
    private String contact1Name;
    private String contact1Phone;
    private String contact2Name;
    private String contact2Phone;
    private String contact3Name;
    private String contact3Phone;
}
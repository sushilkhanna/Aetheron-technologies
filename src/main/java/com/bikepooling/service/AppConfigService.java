package com.bikepooling.service;

import com.bikepooling.entity.AppConfig;
import com.bikepooling.exception.AppException;
import com.bikepooling.repository.AppConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AppConfigService {

    private static final Long CONFIG_ID = 1L;

    private final AppConfigRepository configRepo;

    public AppConfig get() {
        return configRepo.findById(CONFIG_ID)
                .orElseThrow(() -> AppException.notFound(
                        "App config not found. Run the DB seed script to insert the default row."));
    }

    public BigDecimal getFarePerKm()             { return get().getFarePerKm(); }
    public BigDecimal getMinFare()               { return get().getMinFare(); }
    public int        getMatchRadiusMetres()     { return get().getMatchRadiusMetres(); }
    public int        getMatchTimeWindowMinutes(){ return get().getMatchTimeWindowMinutes(); }
}
package com.technical.aemet.config;

import com.technical.aemet.domain.service.MunicipalityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class MunicipalityWarmup {
    private static final Logger log = LoggerFactory.getLogger(MunicipalityWarmup.class);
    private final MunicipalityService service;

    public MunicipalityWarmup(MunicipalityService service) {
        this.service = service;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void load() {
        service.refreshAsync();
        log.info("Municipality catalogue refresh scheduled");
    }
}

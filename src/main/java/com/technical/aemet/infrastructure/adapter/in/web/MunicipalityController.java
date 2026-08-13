package com.technical.aemet.infrastructure.adapter.in.web;

import com.technical.aemet.domain.model.Municipality;
import com.technical.aemet.domain.port.in.SearchMunicipalityUseCase;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/municipalities")
public class MunicipalityController {
    private final SearchMunicipalityUseCase useCase;

    public MunicipalityController(SearchMunicipalityUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    List<Municipality> search(@RequestParam(defaultValue = "") String name) {
        return useCase.search(name);
    }
}

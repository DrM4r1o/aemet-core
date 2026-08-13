package com.technical.aemet.domain.port.in;

import com.technical.aemet.domain.model.Municipality;
import java.util.List;

public interface SearchMunicipalityUseCase {
    List<Municipality> search(String name);
}

package com.technical.aemet.domain.port.out;

import com.technical.aemet.domain.model.Municipality;
import java.util.List;

public interface MunicipalityStore {
    List<Municipality> replaceAll(List<Municipality> municipalities);
}

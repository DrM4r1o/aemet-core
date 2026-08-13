package com.technical.aemet.infrastructure.adapter.out.persistence;

import com.technical.aemet.domain.model.Municipality;
import com.technical.aemet.domain.port.out.MunicipalityStore;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class MunicipalityPersistenceAdapter implements MunicipalityStore {
    private final MunicipalityRepository repository;

    public MunicipalityPersistenceAdapter(MunicipalityRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public List<Municipality> replaceAll(List<Municipality> municipalities) {
        var entities = municipalities.stream()
                .map(m -> new MunicipalityEntity(m.code(), m.name(), m.province()))
                .toList();

        repository.deleteAllInBatch();
        repository.saveAllAndFlush(entities);
        return repository.findAll().stream()
                .map(m -> new Municipality(m.getCode(), m.getName(), m.getProvince()))
                .toList();
    }
}

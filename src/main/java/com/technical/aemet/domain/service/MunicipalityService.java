package com.technical.aemet.domain.service;

import com.technical.aemet.domain.model.Municipality;
import com.technical.aemet.domain.port.in.SearchMunicipalityUseCase;
import com.technical.aemet.domain.port.out.MunicipalityProvider;
import com.technical.aemet.domain.port.out.MunicipalityStore;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class MunicipalityService implements SearchMunicipalityUseCase {
    private final MunicipalityProvider provider;
    private final MunicipalityStore store;
    private final Executor executor;
    private final AtomicReference<List<Municipality>> municipalities = new AtomicReference<>(List.of());
    private final AtomicReference<Boolean> refreshInProgress = new AtomicReference<>(false);

    public MunicipalityService(MunicipalityProvider provider, MunicipalityStore store,
            @Qualifier("applicationTaskExecutor") Executor executor) {
        this.provider = provider;
        this.store = store;
        this.executor = executor;
    }

    public synchronized void refresh() {
        var loaded = provider.loadMunicipalities();
        municipalities.set(store.replaceAll(loaded));
    }

    public void refreshAsync() {
        if (!refreshInProgress.compareAndSet(false, true))
            return;
        executor.execute(() -> {
            try {
                refresh();
            } finally {
                refreshInProgress.set(false);
            }
        });
    }

    @Override
    public List<Municipality> search(String name) {
        if (municipalities.get().isEmpty()) {
            refreshAsync();
        }
        var query = normalize(name == null ? "" : name);
        return municipalities.get().stream()
                .filter(m -> query.isBlank() || normalize(m.name()).contains(query))
                .limit(20)
                .toList();
    }

    private String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).trim();
    }
}

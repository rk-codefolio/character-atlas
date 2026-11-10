package dev.characteratlas.archipelago.catalog.infrastructure;

import dev.characteratlas.archipelago.catalog.application.SearchContentOverrides;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisSearchContentOverrides implements SearchContentOverrides {
    private final SearchContentMapper mapper;

    MyBatisSearchContentOverrides(SearchContentMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Map<String, Boolean> load() {
        return mapper.load().stream().collect(Collectors.toUnmodifiableMap(
                SearchContentMapper.VisibilityRow::id, SearchContentMapper.VisibilityRow::visible));
    }

    @Override
    public void lock() {
        mapper.lock();
    }

    @Override
    public void save(String id, boolean visible) {
        mapper.save(id, visible);
    }
}

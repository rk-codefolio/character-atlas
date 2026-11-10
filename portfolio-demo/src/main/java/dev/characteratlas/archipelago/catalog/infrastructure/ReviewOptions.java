package dev.characteratlas.archipelago.catalog.infrastructure;

import dev.characteratlas.archipelago.catalog.application.CatalogOptions;
import dev.characteratlas.domain.CharacterProfile;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class ReviewOptions implements CatalogOptions {
    private final OptionsMapper mapper;
    private final Book book;

    ReviewOptions(OptionsMapper mapper, ObjectMapper json) throws IOException {
        this.mapper = mapper;
        try (var input = new ClassPathResource("archipelago-options.json").getInputStream()) {
            book = json.readValue(input, Book.class);
        }
    }

    @Override
    public Options get() {
        var roles = mapper.roles().stream().collect(Collectors.groupingBy(OptionsMapper.RoleRow::role,
                Collectors.mapping(OptionsMapper.RoleRow::characterId, Collectors.toList())));
        var kinds = mapper.kinds().stream().collect(Collectors.toMap(
                OptionsMapper.KindRow::label, OptionsMapper.KindRow::kind, (first, _) -> first));
        return new Options(book.contents(), book.shells(), book.matrices(), book.mainStats(), book.template(),
                roles, kinds, book.attributes(), mapper.advanced());
    }

    @Override
    public boolean isSearchable(String id) {
        return book.searchable().contains(id);
    }

    @Override
    public List<Content> contents() {
        return book.contents();
    }

    @Override
    public List<EffectTip> effects(String termId) {
        return mapper.effects(termId);
    }

    @Override
    public Progress progress(CharacterProfile character) {
        var levels = mapper.levels(character.id()).stream().collect(Collectors.groupingBy(OptionsMapper.LevelRow::skillId,
                Collectors.mapping(row -> new SkillLevel(row.level(), row.description()), Collectors.toList())));
        var metadata = mapper.metadata(character.id()).stream().collect(Collectors.toMap(
                OptionsMapper.MetaRow::skillId, row -> new SkillInfo(row.passive(), row.cooldown())));
        return new Progress(mapper.awakened(character.id()), book.attributes().get(character.attribute()), levels, metadata);
    }

    record Book(List<Content> contents, Set<String> searchable, List<String> shells, List<Matrix> matrices,
                List<String> mainStats, List<String> template, Map<String, String> attributes) {}
}

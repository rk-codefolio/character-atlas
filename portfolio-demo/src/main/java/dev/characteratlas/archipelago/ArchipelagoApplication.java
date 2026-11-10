package dev.characteratlas.archipelago;

import dev.characteratlas.application.CharacterCatalog;
import dev.characteratlas.application.CatalogRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ArchipelagoApplication {
    public static void main(String[] args) {
        SpringApplication.run(ArchipelagoApplication.class, args);
    }

    @Bean
    CharacterCatalog characterCatalog(CatalogRepository repository) {
        return new CharacterCatalog(repository);
    }
}

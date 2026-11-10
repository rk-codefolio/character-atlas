package dev.characteratlas.archipelago;

import dev.characteratlas.application.CatalogRepository;
import dev.characteratlas.application.CharacterCatalog;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 群島のアプリケーションを表す。 */
@SpringBootApplication
@EnableScheduling
public class ArchipelagoApplication {
  /** 起動処理を扱う。 */
  public static void main(String[] args) {
    SpringApplication.run(ArchipelagoApplication.class, args);
  }

  /** キャラクターの図鑑を扱う。 */
  @Bean
  CharacterCatalog characterCatalog(CatalogRepository repository) {
    return new CharacterCatalog(repository);
  }
}

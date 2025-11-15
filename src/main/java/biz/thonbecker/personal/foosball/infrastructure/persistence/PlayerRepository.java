package biz.thonbecker.personal.foosball.infrastructure.persistence;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource(
        path = "players",
        collectionResourceRel = "players",
        itemResourceRel = "player")
public interface PlayerRepository extends CrudRepository<Player, Long> {

    @RestResource(path = "by-name", rel = "by-name")
    Optional<Player> findByName(String name);

    @RestResource(path = "by-email", rel = "by-email")
    Optional<Player> findByEmail(String email);

    @RestResource(path = "search", rel = "search")
    List<Player> findByNameContainingIgnoreCase(String name);

    List<Player> findAllByOrderByNameAsc();
}

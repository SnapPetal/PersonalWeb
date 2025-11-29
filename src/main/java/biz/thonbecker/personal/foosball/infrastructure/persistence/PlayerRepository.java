package biz.thonbecker.personal.foosball.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

@RepositoryRestResource(path = "players", collectionResourceRel = "players", itemResourceRel = "player")
public interface PlayerRepository extends CrudRepository<Player, Long> {

    @RestResource(path = "by-name", rel = "by-name")
    Optional<Player> findByName(String name);

    @RestResource(path = "search", rel = "search")
    List<Player> findByNameContainingIgnoreCase(String name);

    List<Player> findAllByOrderByNameAsc();

    // Rating/Ranking queries
    @RestResource(path = "by-rating", rel = "by-rating")
    List<Player> findAllByOrderByRatingDesc();

    @RestResource(path = "leaderboard", rel = "leaderboard")
    List<Player> findTop10ByOrderByRatingDesc();

    List<Player> findByGamesPlayedGreaterThanEqualOrderByRatingDesc(int minGames);
}

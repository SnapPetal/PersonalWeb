package solutions.thonbecker.personal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import solutions.thonbecker.personal.entity.PlayerEntity;

import java.util.Optional;

@Repository
public interface PlayerRepository extends JpaRepository<PlayerEntity, String> {
    Optional<PlayerEntity> findByName(String name);
}

package biz.thonbecker.personal.landscape.platform.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PlantPlacementRepository extends JpaRepository<PlantPlacementEntity, Long> {
    // Inherits basic CRUD operations from JpaRepository
    // Placements are typically accessed through LandscapePlanEntity.getPlacements()
}

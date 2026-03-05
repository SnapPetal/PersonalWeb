package biz.thonbecker.personal.landscape.platform.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface RecommendedPlantRepository extends JpaRepository<RecommendedPlantEntity, Long> {
    // Inherits basic CRUD operations from JpaRepository
    // Recommendations are typically accessed through LandscapePlanEntity.getRecommendations()
}

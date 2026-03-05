package biz.thonbecker.personal.landscape.platform.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "recommended_plants", schema = "landscape")
@Data
@NoArgsConstructor
public class RecommendedPlantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private LandscapePlanEntity plan;

    @Column(name = "usda_symbol", nullable = false)
    private String usdaSymbol;

    @Column(name = "plant_name", nullable = false)
    private String plantName;

    @Column(name = "common_name")
    private String commonName;

    @Column(name = "recommendation_reason", nullable = false)
    private String recommendationReason;

    @Column(name = "confidence_score", nullable = false)
    private Integer confidenceScore;

    @Column(name = "light_requirement")
    private String lightRequirement;

    @Column(name = "water_requirement")
    private String waterRequirement;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}

package biz.thonbecker.personal.landscape.platform.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "plant_placements", schema = "landscape")
@Data
@NoArgsConstructor
public class PlantPlacementEntity {

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

    @Column(name = "x_coord", nullable = false, precision = 6, scale = 2)
    private BigDecimal xCoord;

    @Column(name = "y_coord", nullable = false, precision = 6, scale = 2)
    private BigDecimal yCoord;

    @Column(name = "notes")
    private String notes;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (quantity == null) {
            quantity = 1;
        }
    }
}

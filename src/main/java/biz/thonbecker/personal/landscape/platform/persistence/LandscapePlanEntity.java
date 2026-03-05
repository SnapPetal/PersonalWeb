package biz.thonbecker.personal.landscape.platform.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "landscape_plans", schema = "landscape")
@Data
@NoArgsConstructor
public class LandscapePlanEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "image_s3_key", nullable = false)
    private String imageS3Key;

    @Column(name = "image_cdn_url", nullable = false)
    private String imageCdnUrl;

    @Column(name = "hardiness_zone", nullable = false)
    private String hardinessZone;

    @Column(name = "zip_code")
    private String zipCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PlantPlacementEntity> placements = new ArrayList<>();

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<RecommendedPlantEntity> recommendations = new ArrayList<>();

    @PrePersist
    void prePersist() {
        final var now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}

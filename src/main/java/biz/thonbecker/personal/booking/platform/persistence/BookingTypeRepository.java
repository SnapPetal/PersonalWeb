package biz.thonbecker.personal.booking.platform.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingTypeRepository extends JpaRepository<BookingTypeEntity, Long> {

    /**
     * Finds all active booking types.
     *
     * @return List of active booking types
     */
    List<BookingTypeEntity> findByActiveTrue();
}

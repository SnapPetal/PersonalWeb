package biz.thonbecker.personal.user.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface UserProfileRepository extends JpaRepository<UserProfileEntity, String> {}

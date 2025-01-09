package com.rentalplatform.repository;

import com.rentalplatform.entity.NotificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {
    @Query("SELECT n FROM NotificationEntity n WHERE n.user.id = :userId AND n.isRead = false")
    Page<NotificationEntity> findAllByUserIdAndIsReadFalse(@Param("userId") Long userId, Pageable pageable);
    Page<NotificationEntity> findAllByUserId(Long userId, Pageable pageable);
}

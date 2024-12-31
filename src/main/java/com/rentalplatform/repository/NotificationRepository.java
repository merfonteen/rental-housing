package com.rentalplatform.repository;

import com.rentalplatform.entity.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {
    List<NotificationEntity> findAllByUserIdAndIsReadFalse(Long userId);
    List<NotificationEntity> findAllByUserId(Long userId);
}

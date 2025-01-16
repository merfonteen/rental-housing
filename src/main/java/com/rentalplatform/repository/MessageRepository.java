package com.rentalplatform.repository;

import com.rentalplatform.entity.MessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<MessageEntity, Long> {
    List<MessageEntity> findAllBySenderIdAndReceiverId(Long senderId, Long receiverId);

    @Query("SELECT m FROM MessageEntity m WHERE m.receiver.username = :username")
    List<MessageEntity> findAllByUsername(@Param("username") String username);

    @Query("SELECT m FROM MessageEntity m WHERE m.receiver.id = :receiverId AND m.isRead = false")
    List<MessageEntity> findAllByReceiverIdAndIsReadFalse(@Param("receiverId") Long receiverId);
}

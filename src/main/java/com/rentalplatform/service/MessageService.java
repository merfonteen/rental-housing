package com.rentalplatform.service;

import com.rentalplatform.controller.MessageWebSocketController;
import com.rentalplatform.dto.MessageDto;
import com.rentalplatform.entity.MessageEntity;
import com.rentalplatform.entity.UserEntity;
import com.rentalplatform.exception.BadRequestException;
import com.rentalplatform.exception.NotFoundException;
import com.rentalplatform.mapper.MessageDtoMapper;
import com.rentalplatform.repository.MessageRepository;
import com.rentalplatform.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class MessageService {

    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final MessageDtoMapper messageDtoMapper;
    private final MessageWebSocketController messageWebSocketController;

    public List<MessageDto> getConversation(String receiverUsername, String senderUsername) {
        UserEntity receiver = findUserByUsernameOrThrowException(receiverUsername);
        UserEntity sender = findUserByUsernameOrThrowException(senderUsername);

        return messageDtoMapper.makeMessageDto(
                messageRepository.findAllBySenderIdAndReceiverId(sender.getId(), receiver.getId()));
    }

    @Cacheable(cacheNames = "messages", key = "#username", unless = "#result.isEmpty()")
    public List<MessageDto> getAllMessages(String username) {
        findUserByUsernameOrThrowException(username);
        return messageDtoMapper.makeMessageDto(messageRepository.findAllByReceiverUsername(username));
    }

    @Cacheable(cacheNames = "unreadMessages", key = "#username", unless = "#result.isEmpty()")
    public List<MessageDto> getUnreadMessages(String username) {
        UserEntity user = findUserByUsernameOrThrowException(username);
        return messageDtoMapper.makeMessageDto(messageRepository.findAllByReceiverIdAndIsReadFalse(user.getId()));
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "messages", key = "#senderUsername"),
            @CacheEvict(cacheNames = "unreadMessages", key = "#receiverUsername")
    })
    @Transactional
    public void sendMessage(String receiverUsername, String content, String senderUsername) {
        UserEntity receiver = findUserByUsernameOrThrowException(receiverUsername);
        UserEntity sender = findUserByUsernameOrThrowException(senderUsername);

        MessageEntity message = MessageEntity.builder()
                .content(content)
                .sender(sender)
                .receiver(receiver)
                .build();

        messageRepository.save(message);
        messageWebSocketController.sendNotification(receiverUsername, content);
    }

    @CacheEvict(cacheNames = "unreadMessages", key = "#username")
    @Transactional
    public MessageDto markAsRead(Long messageId, String username) {
        MessageEntity message = findMessageOrThrowException(messageId);

        if(!message.getReceiver().getUsername().equals(username)) {
            throw new BadRequestException("You are not not authorized to mark this message as read");
        }

        message.setRead(true);

        MessageEntity savedMessage = messageRepository.save(message);
        return messageDtoMapper.makeMessageDto(savedMessage);
    }

    private MessageEntity findMessageOrThrowException(Long messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(() -> new NotFoundException("Message with id '%d' not found".formatted(messageId)));
    }

    private UserEntity findUserByUsernameOrThrowException(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User '%s' not found".formatted(username)));
    }
}

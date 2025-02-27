package com.rentalplatform.services;

import com.rentalplatform.controller.MessageWebSocketController;
import com.rentalplatform.dto.MessageDto;
import com.rentalplatform.entity.MessageEntity;
import com.rentalplatform.entity.UserEntity;
import com.rentalplatform.exception.BadRequestException;
import com.rentalplatform.exception.NotFoundException;
import com.rentalplatform.mapper.MessageDtoMapper;
import com.rentalplatform.repository.MessageRepository;
import com.rentalplatform.repository.UserRepository;
import com.rentalplatform.service.MessageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MessageDtoMapper messageDtoMapper;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private MessageWebSocketController messageWebSocketController;

    @InjectMocks
    private MessageService messageService;

    @Test
    void testGetConversation_Success() {
        String receiverUsername = "Receiver Username";
        String senderUsername = "Sender Username";

        UserEntity receiver = UserEntity.builder()
                .id(1L)
                .username(receiverUsername)
                .build();

        UserEntity sender = UserEntity.builder()
                .id(2L)
                .username(senderUsername)
                .build();

        MessageEntity message = MessageEntity.builder()
                .id(1L)
                .content("Some content...")
                .receiver(receiver)
                .sender(sender)
                .build();

        MessageDto messageDto = MessageDto.builder()
                .id(1L)
                .content("Some content...")
                .receiverUsername(receiver.getUsername())
                .senderUsername(sender.getUsername())
                .build();

        List<MessageEntity> messages = new ArrayList<>(List.of(message));
        List<MessageDto> messageDtos = new ArrayList<>(List.of(messageDto));

        when(userRepository.findByUsername(receiverUsername)).thenReturn(Optional.ofNullable(receiver));
        when(userRepository.findByUsername(senderUsername)).thenReturn(Optional.ofNullable(sender));
        when(messageRepository.findAllBySenderIdAndReceiverId(sender.getId(), receiver.getId())).thenReturn(messages);
        when(messageDtoMapper.makeMessageDto(messages)).thenReturn(messageDtos);

        List<MessageDto> result = messageService.getConversation(receiverUsername, senderUsername);

        assertNotNull(result);
        assertEquals(messageDto.getContent(), result.get(0).getContent());
        verify(messageDtoMapper, times(1)).makeMessageDto(messages);
        verify(messageRepository, times(1)).findAllBySenderIdAndReceiverId(sender.getId(), receiver.getId());
    }

    @Test
    void testGetConversation_WhenReceiverNotFound_ShouldThrowException() {
        String receiverUsername = "Receiver Username";
        String senderUsername = "Sender Username";

        when(userRepository.findByUsername(receiverUsername)).thenReturn(Optional.empty());

        Exception exception = assertThrows(NotFoundException.class, () ->
                messageService.getConversation(receiverUsername, senderUsername));

        assertEquals("User 'Receiver Username' not found", exception.getMessage());
    }

    @Test
    void testGetConversation_WhenSenderNotFound_ShouldThrowException() {
        String receiverUsername = "Receiver Username";
        String senderUsername = "Sender Username";

        UserEntity receiver = UserEntity.builder()
                .id(1L)
                .username(senderUsername)
                .build();

        when(userRepository.findByUsername(receiverUsername)).thenReturn(Optional.ofNullable(receiver));
        when(userRepository.findByUsername(senderUsername)).thenReturn(Optional.empty());

        Exception exception = assertThrows(NotFoundException.class, () ->
                messageService.getConversation(receiverUsername, senderUsername));

        assertEquals("User 'Sender Username' not found", exception.getMessage());
    }

    @Test
    void testGetAllMessages_Success() {
        String username = "Test Username";
        UserEntity user = UserEntity.builder()
                .id(1L)
                .username(username)
                .build();

        MessageEntity message = MessageEntity.builder()
                .id(1L)
                .content("Some content...")
                .receiver(user)
                .sender(UserEntity.builder().id(2L).username("Sender Username").build())
                .isRead(true)
                .build();

        MessageDto messageDto = MessageDto.builder()
                .id(1L)
                .content("Some content...")
                .receiverUsername(user.getUsername())
                .senderUsername(message.getSender().getUsername())
                .isRead(true)
                .build();

        List<MessageEntity> messages = new ArrayList<>(List.of(message));
        List<MessageDto> messageDtos = new ArrayList<>(List.of(messageDto));

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(messageRepository.findAllByUsername(username)).thenReturn(messages);
        when(messageDtoMapper.makeMessageDto(messages)).thenReturn(messageDtos);

        List<MessageDto> result = messageService.getAllMessages(username);

        assertNotNull(result);
        assertEquals(messageDto.getContent(), result.get(0).getContent());
        verify(messageDtoMapper, times(1)).makeMessageDto(messages);
        verify(messageRepository, times(1)).findAllByUsername(username);
        verify(userRepository, times(1)).findByUsername(username);
    }

    @Test
    void testGetAllMessages_WhenUserNotFound_ShouldThrowException() {
        String username = "Test Username";

        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> messageService.getAllMessages(username));
    }

    @Test
    void testGetUnreadMessages_Success() {
        String username = "Test Username";

        UserEntity user = UserEntity.builder()
                .id(1L)
                .username(username)
                .build();

        MessageEntity message = MessageEntity.builder()
                .id(1L)
                .content("Some content...")
                .receiver(user)
                .sender(UserEntity.builder().id(2L).username("Sender Username").build())
                .isRead(false)
                .build();

        MessageDto messageDto = MessageDto.builder()
                .id(1L)
                .content("Some content...")
                .receiverUsername(user.getUsername())
                .senderUsername(message.getSender().getUsername())
                .isRead(false)
                .build();

        List<MessageEntity> messages = new ArrayList<>(List.of(message));
        List<MessageDto> messageDtos = new ArrayList<>(List.of(messageDto));

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(messageRepository.findAllByReceiverIdAndIsReadFalse(user.getId())).thenReturn(messages);
        when(messageDtoMapper.makeMessageDto(messages)).thenReturn(messageDtos);

        List<MessageDto> result = messageService.getUnreadMessages(username);

        assertNotNull(result);
        assertEquals(messageDto.getContent(), result.get(0).getContent());
        verify(messageDtoMapper, times(1)).makeMessageDto(messages);
        verify(messageRepository, times(1)).findAllByReceiverIdAndIsReadFalse(user.getId());
    }

    @Test
    void testGetUnreadMessages_WhenUserNotFound_ShouldThrowException() {
        String username = "Test Username";

        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> messageService.getUnreadMessages(username));
    }

    @Test
    void testSendMessage_Success() {
        String receiverUsername = "Receiver Username";
        String content = "Content";
        String senderUsername = "Sender Username";

        UserEntity receiver = UserEntity.builder()
                .id(1L)
                .username(receiverUsername)
                .build();

        UserEntity sender = UserEntity.builder()
                .id(2L)
                .username(senderUsername)
                .build();

        MessageEntity message = MessageEntity.builder()
                .content(content)
                .sender(sender)
                .receiver(receiver)
                .build();

        when(userRepository.findByUsername(receiverUsername)).thenReturn(Optional.ofNullable(receiver));
        when(userRepository.findByUsername(senderUsername)).thenReturn(Optional.ofNullable(sender));
        when(messageRepository.save(any(MessageEntity.class))).thenReturn(message);

        messageService.sendMessage(receiverUsername, content, senderUsername);

        verify(messageRepository, times(1)).save(any(MessageEntity.class));
        verify(messageWebSocketController, times(1)).sendNotification(receiverUsername, content);
    }

    @Test
    void testSendMessage_WhenReceiverNotFound_ShouldThrowException() {
        String receiverUsername = "Receiver Username";
        String content = "Content";
        String senderUsername = "Sender Username";

        when(userRepository.findByUsername(receiverUsername)).thenReturn(Optional.empty());

        Exception exception = assertThrows(NotFoundException.class, () ->
                messageService.sendMessage(receiverUsername, content, senderUsername));

        assertEquals("User 'Receiver Username' not found", exception.getMessage());
    }

    @Test
    void testSendMessage_WhenSenderNotFound_ShouldThrowException() {
        String receiverUsername = "Receiver Username";
        String content = "Content";
        String senderUsername = "Sender Username";

        UserEntity receiver = UserEntity.builder()
                .id(1L)
                .username(senderUsername)
                .build();

        when(userRepository.findByUsername(receiverUsername)).thenReturn(Optional.ofNullable(receiver));
        when(userRepository.findByUsername(senderUsername)).thenReturn(Optional.empty());

        Exception exception = assertThrows(NotFoundException.class, () ->
                messageService.sendMessage(receiverUsername, content, senderUsername));

        assertEquals("User 'Sender Username' not found", exception.getMessage());
    }

    @Test
    void testMarkAsRead_Success() {
        Long messageId = 1L;
        String username = "Test Username";

        UserEntity user = UserEntity.builder()
                .id(1L)
                .username(username)
                .build();

        MessageEntity message = MessageEntity.builder()
                .id(1L)
                .receiver(user)
                .content("Content")
                .isRead(false)
                .build();

        MessageDto messageDto = MessageDto.builder()
                .id(1L)
                .receiverUsername(user.getUsername())
                .content(message.getContent())
                .isRead(true)
                .build();

        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));
        when(messageRepository.save(any(MessageEntity.class))).thenReturn(message);
        when(messageDtoMapper.makeMessageDto(message)).thenReturn(messageDto);

        MessageDto result = messageService.markAsRead(messageId, username);

        assertNotNull(result);
        assertEquals(messageDto.getId(), result.getId());
        assertEquals(messageDto.isRead(), result.isRead());
        verify(messageRepository, times(1)).save(any(MessageEntity.class));
        verify(messageDtoMapper, times(1)).makeMessageDto(message);
    }

    @Test
    void testMarkAsRead_WhenMessageNotFound_ShouldThrowException() {
        Long messageId = 1L;
        String username = "Test Username";

        when(messageRepository.findById(messageId)).thenReturn(Optional.empty());

        Exception exception = assertThrows(NotFoundException.class, () ->
                messageService.markAsRead(messageId, username));

        assertEquals("Message with id '1' not found", exception.getMessage());
    }

    @Test
    void testMarkAsRead_WhenUserNotOwnerTheMessage_ShouldThrowException() {
        Long messageId = 1L;
        String username = "Test Username";

        MessageEntity message = MessageEntity.builder()
                .id(1L)
                .receiver(UserEntity.builder().id(50L).username("Another User").build())
                .content("Content")
                .isRead(false)
                .build();

        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));

        Exception exception = assertThrows(BadRequestException.class, () ->
                messageService.markAsRead(messageId, username));

        assertEquals("You are not not authorized to mark this message as read", exception.getMessage());
    }
}
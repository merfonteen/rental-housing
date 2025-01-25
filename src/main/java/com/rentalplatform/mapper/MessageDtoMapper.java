package com.rentalplatform.mapper;

import com.rentalplatform.dto.MessageDto;
import com.rentalplatform.entity.MessageEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class MessageDtoMapper {
    public MessageDto makeMessageDto(MessageEntity message) {
        return MessageDto.builder()
                .id(message.getId())
                .content(message.getContent())
                .senderUsername(message.getSender().getUsername())
                .receiverUsername(message.getReceiver().getUsername())
                .isRead(message.isRead())
                .createdAt(message.getCreatedAt())
                .build();
    }

    public List<MessageDto> makeMessageDto(List<MessageEntity> messages) {
        return messages.stream()
                .map(this::makeMessageDto)
                .collect(Collectors.toList());
    }
}

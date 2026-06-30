package com.back.domain.chat.chatRoom.repository;
import com.back.domain.chat.chatRoom.entity.ChatRoom;
import com.back.domain.chat.chatRoom.entity.ChatRoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, UUID> {
    long countByStatus(ChatRoomStatus status);
}
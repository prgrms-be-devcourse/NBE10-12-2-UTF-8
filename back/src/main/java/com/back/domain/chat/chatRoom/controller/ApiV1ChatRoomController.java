package com.back.domain.chat.chatRoom.controller;

import com.back.domain.chat.chatRoom.dto.ChatRoomDto;
import com.back.domain.chat.chatRoom.entity.ChatRoom;
import com.back.domain.chat.chatRoom.service.ChatRoomService;
import com.back.domain.chat.chatRoomParticipant.service.ChatRoomParticipantService;
import com.back.domain.member.member.entity.Member;
import com.back.global.exception.ServiceException;
import com.back.global.rq.Rq;
import com.back.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
@Tag(name = "ApiV1ChatRoomController", description = "API 채팅방 컨트롤러")
@SecurityRequirement(name = "bearerAuth")
public class ApiV1ChatRoomController {

    private final ChatRoomService chatRoomService;
    private final ChatRoomParticipantService chatRoomParticipantService;
    private final Rq rq;

    @GetMapping("/{roomId}")
    @Operation(summary = "채팅방 정보 조회")
    public RsData<ChatRoomDto> getRoom(@PathVariable UUID roomId) {
        ChatRoom chatRoom = chatRoomService.getChatRoom(roomId);

        Member actor = rq.getActor();
        if (actor == null) {
            throw new ServiceException("401-1", "인증이 필요합니다.");
        }

        chatRoomParticipantService.validateAccess(roomId, actor);

        return new RsData<>(
                "200-1",
                "채팅방 정보 조회 성공",
                new ChatRoomDto(chatRoom)
        );
    }

    @DeleteMapping("/{roomId}")
    @Operation(summary = "채팅방 종료")
    public RsData<ChatRoomDto> closeRoom(@PathVariable UUID roomId) {
        Member actor = rq.getActor();
        if (actor == null) {
            throw new ServiceException("401-1", "인증이 필요합니다.");
        }

        ChatRoom chatRoom = chatRoomService.closeChatRoom(roomId, actor);
        return new RsData<>(
                "200-1",
                "채팅방 삭제 성공",
                new ChatRoomDto(chatRoom)
        );
    }
}
package com.roommate_connection_platform.controller;

import com.roommate_connection_platform.DTO.RoomConnection.SendRequestDTO;
import com.roommate_connection_platform.DTO.RoomConnection.UpdateRoomConnectionRequest;
import com.roommate_connection_platform.services.IRoomConnectionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RoomConnectController {
    private final IRoomConnectionService roomConnectionService;

    @PostMapping(value = "/api/request_join")
    public ResponseEntity<?> sendRequest(@Valid @RequestBody SendRequestDTO sendRequestDTO){
        roomConnectionService.createRoomConnection(sendRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body("Gui yeu cau thanh cong");
    }

    @DeleteMapping(value = "/api/request_join/{id}")
    public ResponseEntity<?> deleteRequest(@PathVariable Long id){
        roomConnectionService.deleteRoomConnection(id);
        return ResponseEntity.ok("Huy yeu cau thanh cong");
    }

    @PatchMapping(value = "/api/request_join")
    public ResponseEntity<?> updateRequest(@Valid @RequestBody UpdateRoomConnectionRequest updateRoomConnectionRequest){
        roomConnectionService.updateRequest(updateRoomConnectionRequest);
        return ResponseEntity.ok("thanh cong");
    }

    @GetMapping(value = "/api/user_joined")
    public ResponseEntity<?> getUser_Joined(){
        return ResponseEntity.ok(roomConnectionService.getUser_Joined());
    }

    @GetMapping(value = "/api/posts/{postId}/members")
    public ResponseEntity<?> getJoinedMembers(@PathVariable Long postId){
        return ResponseEntity.ok(roomConnectionService.getJoinedMembersByPostId(postId));
    }
}

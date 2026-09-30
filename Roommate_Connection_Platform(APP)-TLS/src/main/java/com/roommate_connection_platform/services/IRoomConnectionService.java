package com.roommate_connection_platform.services;

import com.roommate_connection_platform.DTO.Post.PostResponseDTO;
import com.roommate_connection_platform.DTO.RoomConnection.SendRequestDTO;
import com.roommate_connection_platform.DTO.RoomConnection.JoinedRoomMemberResponseDTO;
import com.roommate_connection_platform.DTO.RoomConnection.UpdateRoomConnectionRequest;

import java.util.List;

public interface IRoomConnectionService {
    void createRoomConnection(SendRequestDTO sendRequestDTO);

    void deleteRoomConnection(Long id);

    void updateRequest(UpdateRoomConnectionRequest updateRoomConnectionRequest);

    List<PostResponseDTO> getUser_Joined();

    List<JoinedRoomMemberResponseDTO> getJoinedMembersByPostId(Long postId);
}

package com.roommate_connection_platform.services;

import com.roommate_connection_platform.DTO.Post.CreatePostDTO;
import com.roommate_connection_platform.DTO.Post.GetPostRequestDTO;
import com.roommate_connection_platform.DTO.Post.UpdatePostDTO;
import com.roommate_connection_platform.DTO.Post.PostResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IPostService {
    void createPost(CreatePostDTO dto, List<MultipartFile> images);

    void updatePost(Long postId, UpdatePostDTO dto, List<MultipartFile> images);

    void deletePost(Long postId);

    List<Long> deletePosts(List<Long> postIds);

    PostResponseDTO getPost(Long postId);

    List<PostResponseDTO> getPosts(GetPostRequestDTO getPostRequestDTO);

    List<PostResponseDTO> getMyPostsByStatus(String status);
}

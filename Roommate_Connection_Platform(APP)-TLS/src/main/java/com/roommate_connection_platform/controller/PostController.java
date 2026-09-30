package com.roommate_connection_platform.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roommate_connection_platform.DTO.Post.CreatePostDTO;
import com.roommate_connection_platform.DTO.Post.GetPostRequestDTO;
import com.roommate_connection_platform.DTO.Post.UpdatePostDTO;
import com.roommate_connection_platform.customexceptions.BusinessException;
import com.roommate_connection_platform.services.IPostService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class PostController {
    private final IPostService postService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    //CREATE
    @PostMapping(value = "/api/posts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createPost(
            @RequestPart("post") String postJson,
            @RequestPart("images") List<MultipartFile> images
    ) {
        CreatePostDTO dto = parsePostJson(postJson, CreatePostDTO.class);
        validatePost(dto, images);
        postService.createPost(dto,images);
        return ResponseEntity.ok("Dang bai thanh cong");
    }

    //UPDATE
    @PatchMapping(value = "/api/posts/{postId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updatePost(
            @PathVariable Long postId,
            @RequestPart("post") String postJson,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) {
        UpdatePostDTO dto = parsePostJson(postJson, UpdatePostDTO.class);
        if(images == null && dto.getCurrentMembers()<=dto.getMaxMembers()){
            postService.updatePost(postId, dto, images);
        }
        else{
            validatePost(dto, images);
            postService.updatePost(postId, dto, images);}
        return ResponseEntity.ok("Cap nhat bai dang thanh cong");
    }

    //DELETE
    @DeleteMapping(value = "/api/post/{postId}")
    public ResponseEntity<?> deletePost(@PathVariable Long postId){
        postService.deletePost(postId);
        return ResponseEntity.ok("Xoa thanh cong");
    }

    @DeleteMapping(value = "/api/posts")
    public ResponseEntity<?> deletePosts(@RequestParam List<Long> ids){
        List<Long> errorIds = postService.deletePosts(ids);
        if (errorIds.isEmpty()) return ResponseEntity.ok("Xoa thanh cong");
        return ResponseEntity.ok("Xoa thanh cong mot so bai va mot so bai khong tim thay");
    }

    //READ
    @GetMapping(value = "/api/posts/{postId}")
    public ResponseEntity<?> getPost(@PathVariable Long postId){
        return ResponseEntity.ok(postService.getPost(postId));
    }

    @GetMapping(value = "/api/posts")
    public ResponseEntity<?> getPosts(@Valid @ModelAttribute GetPostRequestDTO getPostRequestDTO){
        return ResponseEntity.ok(postService.getPosts(getPostRequestDTO));
    }

    // Lay danh sach bai dang cua user dang dang nhap theo status OPEN hoac SCHEDULED.
    @GetMapping(value = "/api/my-posts")
    public ResponseEntity<?> getMyPosts(@RequestParam String status) {
        return ResponseEntity.ok(postService.getMyPostsByStatus(status));
    }

    // chuyen string -> json
    private <T> T parsePostJson(String postJson, Class<T> dtoClass) {
        try {
            return objectMapper.readValue(postJson, dtoClass);
        } catch (JsonProcessingException e) {
            throw new BusinessException("INVALID_POST_JSON", "Du lieu bai dang khong dung dinh dang JSON", HttpStatus.BAD_REQUEST);
        }
    }

    // kiem tra valid post(chuyen tu string -> json)
    private <T> void validatePost(T dto, List<MultipartFile> images) {
        Set<ConstraintViolation<T>> violations = validator.validate(dto);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining(", "));
            throw new BusinessException("VALIDATION_ERROR", message, HttpStatus.BAD_REQUEST);
        }

        validateImages(images);

        //valid members
        if (dto instanceof CreatePostDTO createPostDTO
                && createPostDTO.getCurrentMembers() >= createPostDTO.getMaxMembers()) {
            throw new BusinessException("FULL_PEOPLE","phong da day thanh vien roi!",HttpStatus.BAD_REQUEST);
        }

        if (dto instanceof UpdatePostDTO updatePostDTO
                && updatePostDTO.getCurrentMembers() > updatePostDTO.getMaxMembers()) {
            throw new BusinessException("INVALID_MEMBERS","So thanh vien hien tai khong duoc lon hon so thanh vien toi da",HttpStatus.BAD_REQUEST);
        }
    }

    private void validateImages(List<MultipartFile> images) {
        //valid images
        if (images == null || images.isEmpty()) {
            throw new BusinessException("IMAGE_REQUIRED", "Can it nhat 1 anh cho bai dang", HttpStatus.BAD_REQUEST);
        }

        for (MultipartFile image : images) {
            if (image == null || image.isEmpty()) {
                throw new BusinessException("INVALID_IMAGE", "Anh khong hop le", HttpStatus.BAD_REQUEST);
            }

            String contentType = image.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new BusinessException("INVALID_IMAGE_TYPE", "File upload phai la anh", HttpStatus.BAD_REQUEST);
            }
        }
    }



}

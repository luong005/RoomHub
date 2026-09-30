package com.roommate_connection_platform.services.Impl;

import com.roommate_connection_platform.DTO.Post.*;
import com.roommate_connection_platform.DTO.User.MyUserDetail;
import com.roommate_connection_platform.converter.MapStruct;
import com.roommate_connection_platform.customexceptions.BusinessException;
import com.roommate_connection_platform.models.PostEntity;
import com.roommate_connection_platform.models.PostImageEntity;
import com.roommate_connection_platform.models.UserEntity;
import com.roommate_connection_platform.repositories.PostRepository;
import com.roommate_connection_platform.repositories.UserRepository;
import com.roommate_connection_platform.security.SecurityUtils;
import com.roommate_connection_platform.services.IPostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService implements IPostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;
    private final MapStruct mapStruct;

    @Override
    @Transactional
    public void createPost(CreatePostDTO dto, List<MultipartFile> images) {
        MyUserDetail principal = SecurityUtils.getPrincipal();
        if (principal == null) {
            throw new BusinessException("LOGIN_REQUIRED", "Yeu cau dang nhap", HttpStatus.UNAUTHORIZED);
        }

        UserEntity user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "Khong tim thay nguoi dung", HttpStatus.NOT_FOUND));

        PostEntity post = mapStruct.toPostEntity(dto);
        post.setUser(user);
        setPostSchedule(post, dto); // set status va thoi gian dang bai dua theo scheduledAt trong DTO

        post.setImages(buildPostImages(post, images));
        postRepository.save(post);
    }

    @Override
    @Transactional
    public void updatePost(Long postId, UpdatePostDTO dto, List<MultipartFile> images) {
        MyUserDetail principal = SecurityUtils.getPrincipal();
        if (principal == null) {
            throw new BusinessException("LOGIN_REQUIRED", "Yeu cau dang nhap", HttpStatus.UNAUTHORIZED);
        }

        PostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException("POST_NOT_FOUND", "Khong tim thay bai dang", HttpStatus.NOT_FOUND));

        if (!post.getUser().getId().equals(principal.getId())) {
            throw new BusinessException("PERMISSION_DENIED", "Bai dang nay khong ton tai", HttpStatus.FORBIDDEN);
        }

        mapStruct.updatePostEntity(dto, post);

        if (images != null && !images.isEmpty()) {
            replacePostImages(post, images);
        }

        postRepository.save(post);
    }

    @Override
    public void deletePost(Long postId) {
        MyUserDetail principal = SecurityUtils.getPrincipal();
        if (principal == null) {
            throw new BusinessException("LOGIN_REQUIRED", "Yeu cau dang nhap", HttpStatus.UNAUTHORIZED);
        }
        PostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException("POST_NOT_FOUND", "Khong tim thay bai dang", HttpStatus.NOT_FOUND));

        if (!post.getUser().getId().equals(principal.getId())) {
            throw new BusinessException("PERMISSION_DENIED", "Bai dang nay khong ton tai", HttpStatus.FORBIDDEN);
        }

        postRepository.deleteById(postId);
    }

    @Override
    public List<Long> deletePosts(List<Long> postIds) {
        MyUserDetail principal = SecurityUtils.getPrincipal();
        if (principal == null) {
            throw new BusinessException("LOGIN_REQUIRED", "Yeu cau dang nhap", HttpStatus.UNAUTHORIZED);
        }
        // ids: danh sach post cua user dang login
        List<Long> ids = postRepository.findPostIdsByUserId(principal.getId());
        List<Long>errorId = new ArrayList<>();
        for (Long id: postIds){
            if (!ids.contains(id)){
                errorId.add(id);
                continue;}
            postRepository.deleteById(id);
        }
        return errorId;
    }

    @Override
    public PostResponseDTO getPost(Long postId) {
        MyUserDetail principal = SecurityUtils.getPrincipal();
        if (principal == null) {
            throw new BusinessException("LOGIN_REQUIRED", "Yeu cau dang nhap", HttpStatus.UNAUTHORIZED);
        }
        PostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException("POST_NOT_FOUND", "Khong tim thay bai dang", HttpStatus.NOT_FOUND));

        if(post.getStatus().equals("SCHEDULED") && !post.getUser().getId().equals(principal.getId())){
            throw new BusinessException("POST_NOT_FOUND", "Khong tim thay bai dang", HttpStatus.NOT_FOUND);
        }
        PostResponseDTO postResponseDTO = mapStruct.toPostResponseDto(post);
        List<PostImageResponseDTO> postImageResponseDTOS = post.getImages().stream().map(mapStruct::toPostImageResponseDto).toList();
        postResponseDTO.setImages(postImageResponseDTOS);
        return postResponseDTO;
    }

    @Override
    public List<PostResponseDTO> getPosts( GetPostRequestDTO getPostRequestDTO) {
        //GetPostRequestDTO request = getPostRequestDTO == null ? new GetPostRequestDTO() : getPostRequestDTO;
//        MyUserDetail principal = SecurityUtils.getPrincipal();
//        if(getPostRequestDTO.getAuthorId()!=principal.getId() && getPostRequestDTO.getStatus().equals("SCHEDULED")){
//            throw new BusinessException("POST_NOT_FOUND", "Khong tim thay bai dang", HttpStatus.NOT_FOUND);
//        }
        return postRepository.findPostsByRequest(
                        "OPEN",
                        normalize(getPostRequestDTO.getAddress()),
                        getPostRequestDTO.getAuthorId(),
                        getPostRequestDTO.getPriceFrom(),
                        getPostRequestDTO.getPriceTo(),
                        getPostRequestDTO.getCurrentMember(),
                        getPostRequestDTO.getArea(),
                        normalize(getPostRequestDTO.getGender())
                )
                .stream()
                .map(this::toPostResponseDTO)
                .toList();
    }

    @Override
    public List<PostResponseDTO> getMyPostsByStatus(String status) {
        MyUserDetail principal = SecurityUtils.getPrincipal();
        if (principal == null) {
            throw new BusinessException("LOGIN_REQUIRED", "Yeu cau dang nhap", HttpStatus.UNAUTHORIZED);
        }

        return postRepository.findByUserIdAndStatusWithImages(principal.getId(), status)
                .stream()
                .map(this::toPostResponseDTO)
                .toList();
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private PostResponseDTO toPostResponseDTO(PostEntity post) {
        PostResponseDTO postResponseDTO = mapStruct.toPostResponseDto(post);
        List<PostImageResponseDTO> postImageResponseDTOS = post.getImages() == null
                ? List.of()
                : post.getImages().stream().map(mapStruct::toPostImageResponseDto).toList();
        postResponseDTO.setImages(postImageResponseDTOS);
        return postResponseDTO;
    }

    @Scheduled(fixedRate = 60000) // tu dong chay method nay moi 60 giay
    @Transactional // dam bao lenh update status duoc chay trong 1 transaction
    public void publishScheduledPosts() {
        int publishedCount = postRepository.publishScheduledPosts(LocalDateTime.now()); // update cac post SCHEDULED co scheduledAt <= hien tai thanh OPEN
        if (publishedCount > 0) { // chi ghi log khi co it nhat 1 bai duoc publish
            log.info("Published {} scheduled posts", publishedCount); // log so luong bai hen gio vua duoc mo public
        }
    }

    private void setPostSchedule(PostEntity post, CreatePostDTO dto) {
        LocalDateTime now = LocalDateTime.now();

        if (dto.getScheduledAt() == null || dto.getScheduledAt().isBlank()) {
            post.setStatus("OPEN");
            post.setScheduledAt(now);
            return;
        }

        LocalDateTime scheduledAt = parseScheduledAt(dto.getScheduledAt());
        if (!scheduledAt.isAfter(now)) {
            throw new BusinessException(
                    "INVALID_SCHEDULED_AT",
                    "Thoi gian hen dang phai o tuong lai",
                    HttpStatus.BAD_REQUEST
            );
        }

        post.setStatus("SCHEDULED");
        post.setScheduledAt(scheduledAt);
    }

    private LocalDateTime parseScheduledAt(String scheduledAt) {
        try {
            return LocalDateTime.parse(scheduledAt);
        } catch (DateTimeParseException ignored) {
            try {
                return OffsetDateTime.parse(scheduledAt).toLocalDateTime();
            } catch (DateTimeParseException e) {
                throw new BusinessException(
                        "INVALID_SCHEDULED_AT",
                        "Thoi gian hen dang khong dung dinh dang",
                        HttpStatus.BAD_REQUEST
                );
            }
        }
    }

    // up anh len cloud
    private Map uploadImage(MultipartFile file) {
        try {
            return cloudinaryService.upload(file);
        } catch (IOException e) {
            throw new BusinessException("UPLOAD_IMAGE_FAILED", "Upload anh that bai", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    //tao list image va up len cloud -> tra ra list PostImageEntity
    private List<PostImageEntity> buildPostImages(PostEntity post, List<MultipartFile> images) {
        List<CompletableFuture<PostImageEntity>> futures = new ArrayList<>();

        for (int i = 0; i < images.size(); i++) {
            MultipartFile file = images.get(i);
            int displayOrder = i;
            long uploadStartTime = System.currentTimeMillis();
            CompletableFuture<PostImageEntity> future = CompletableFuture.supplyAsync(() -> {
                Map uploadResult = uploadImage(file);
                log.info(
                        "Uploaded post image index={}, originalFilename={}, size={} bytes, duration={} ms",
                        displayOrder,
                        file.getOriginalFilename(),
                        file.getSize(),
                        System.currentTimeMillis() - uploadStartTime
                );
                return PostImageEntity.builder()
                        .imageUrl(uploadResult.get("secure_url").toString())
                        .publicId(uploadResult.get("public_id").toString())
                        .displayOrder(displayOrder)
                        .post(post)
                        .build();
            });

            futures.add(future);
        }

        return futures.stream()
                .map(CompletableFuture::join)
                .toList();
    }

    // xoa list image cu thay bang image moi ( thuoc UPDATE )
    private void replacePostImages(PostEntity post, List<MultipartFile> images) {
        deleteOldImagesFromCloudinary(post.getImages());

        List<PostImageEntity> newImages = buildPostImages(post, images);
        if (post.getImages() == null) {
            post.setImages(new ArrayList<>());
        } else {
            post.getImages().clear();
        }
        post.getImages().addAll(newImages);
    }


    // xoa anh cu tren cloud ( thuoc UPDATE )
    private void deleteOldImagesFromCloudinary(List<PostImageEntity> images) {
        if (images == null || images.isEmpty()) {
            return;
        }

        for (PostImageEntity image : images) {
            if (image.getPublicId() == null || image.getPublicId().isBlank()) {
                continue;
            }

            try {
                cloudinaryService.delete(image.getPublicId());
            } catch (IOException e) {
                throw new BusinessException("DELETE_IMAGE_FAILED", "Xoa anh cu that bai", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
    }

}

package com.roommate_connection_platform.services;

import com.roommate_connection_platform.DTO.Admin.AdminUserResponseDTO;
import com.roommate_connection_platform.DTO.User.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;

import java.util.Map;

public interface IUserService {

    Map<String,String> login(String phoneNumber, String password);

    String refreshAccessToken(String refreshToken) ;

    void logout(HttpServletRequest request);

    void register(UserLoginDTO userLoginDTO);

    void updateInfo(UpdateInfoDTO info);

    void updatePassword(UpdatePasswordDTO updatePasswordDTO);

    UserInfoResponseDTO getProfile();

    Page<AdminUserResponseDTO> getUsers(int page, int size);

    //List<UserInfoResponseDTO> getUsers(UserRequestDTO userRequestDTO);
}

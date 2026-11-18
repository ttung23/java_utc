package com.library.auth.controller;

import com.library.auth.dto.ChangePasswordRequest;
import com.library.auth.dto.ForgotPasswordRequest;
import com.library.auth.dto.LoginRequest;
import com.library.auth.dto.LogoutRequest;
import com.library.auth.dto.RefreshRequest;
import com.library.auth.dto.RegisterRequest;
import com.library.auth.dto.ResetPasswordRequest;
import com.library.auth.dto.TokenResponse;
import com.library.auth.dto.UserResponse;
import com.library.auth.dto.VerifyEmailRequest;
import com.library.auth.service.AuthService;
import com.library.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Đăng ký tài khoản độc giả")
    public ApiResponse<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(authService.register(request), "Đăng ký thành công. Tài khoản đang chờ duyệt.");
    }

    @PostMapping("/login")
    @Operation(summary = "Đăng nhập")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return ApiResponse.ok(authService.login(request, httpRequest));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Làm mới access token")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request, HttpServletRequest httpRequest) {
        return ApiResponse.ok(authService.refresh(request, httpRequest));
    }

    @PostMapping("/logout")
    @Operation(summary = "Đăng xuất")
    public ApiResponse<Void> logout(@RequestBody(required = false) LogoutRequest request) {
        authService.logout(request != null ? request : new LogoutRequest());
        return ApiResponse.ok(null, "Đăng xuất thành công");
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Quên mật khẩu")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ApiResponse.ok(null, "Nếu email tồn tại, hướng dẫn đặt lại mật khẩu đã được gửi.");
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Đặt lại mật khẩu")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.ok(null, "Đặt lại mật khẩu thành công");
    }

    @PostMapping("/change-password")
    @Operation(summary = "Đổi mật khẩu")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return ApiResponse.ok(null, "Đổi mật khẩu thành công");
    }

    @GetMapping("/me")
    @Operation(summary = "Thông tin người dùng hiện tại")
    public ApiResponse<UserResponse> me() {
        return ApiResponse.ok(authService.me());
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Xác thực email")
    public ApiResponse<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        authService.verifyEmail(request);
        return ApiResponse.ok(null, "Xác thực email thành công");
    }
}

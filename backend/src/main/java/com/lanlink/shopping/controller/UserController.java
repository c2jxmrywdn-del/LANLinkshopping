package com.lanlink.shopping.controller;

import com.lanlink.shopping.common.R;
import com.lanlink.shopping.config.CsrfInterceptor;
import com.lanlink.shopping.config.UserContext;
import com.lanlink.shopping.dto.*;
import com.lanlink.shopping.entity.Address;
import com.lanlink.shopping.entity.ThirdAuth;
import com.lanlink.shopping.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 账号中心控制器：个人信息 / 联系方式换绑 / 账户安全(TOTP) / 系统设置 / 第三方授权 / 审计 / CSRF
 * 说明：非 GET 请求由 CsrfInterceptor 校验 X-CSRF-TOKEN；登录态由 AuthInterceptor 保障。
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Value("${app.upload-dir:${user.dir}/uploads}")
    private String uploadDirectory;

    private final UserService userService;
    private final UserProfileService profileService;
    private final VerifyCodeService verifyCodeService;
    private final TotpService totpService;
    private final UserSettingsService settingsService;
    private final ThirdAuthService thirdAuthService;
    private final Audit���q�^
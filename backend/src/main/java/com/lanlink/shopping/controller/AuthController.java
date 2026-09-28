package com.lanlink.shopping.controller;

import com.lanlink.shopping.common.BusinessException;
import com.lanlink.shopping.common.R;
import com.lanlink.shopping.config.UserContext;
import com.lanlink.shopping.dto.LoginDTO;
import com.lanlink.shopping.dto.RegisterDTO;
import com.lanlink.shopping.entity.Role;
import com.lanlink.shopping.entity.User;
import com.lanlink.shopping.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器: 注册 / 登录 / 登出 / 当前用户
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public R<Map<String, Object>> register(@Valid @RequestBody RegisterDTO dto) {
        User u = userService.register(dto);
        return R.ok("注册成功", toView(u));
    }

    @PostMapping("/login")
    public R<Map<String, Object>> login(@Valid @RequestBody LoginDTO dto, HttpServletRequest request) {
        User u = userService.login(dto);
        request.getSession().setAttribute(UserContext.SESSION_KEY, u);
        return R.ok("登录成功", toView(u));
    }

    @PostMapping("/logout")
    public R<Void> logout(HttpServletRequest request) {
        request.getSession().invalidate();
        return R.ok();
    }

    @GetMapping("/me")
    public R<Map<String, Object>> me(HttpServletRequest request) {
        User u = UserContext.current(request);
        if (u == null) throw new BusinessException(401, "未登录");
        return R.ok(toView(u));
    }

    private Map<String, Object> toView(User u) {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", u.getUserId());
        map.put("phone", u.getPhone());
        map.put("nickname", u.getNickname());
        map.put("roleId", u.getRoleId());
        map.put("entId", u.getEntId());
        Role r = userService.findRole(u.getRoleId());
        map.put("roleCode", r == null ? null : r.getRoleCode());
        map.put("roleName", r == null ? null : r.getRoleName());
        return map;
    }
}

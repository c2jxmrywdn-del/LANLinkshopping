package com.lanlink.shopping.service;

import com.lanlink.shopping.common.BusinessException;
import com.lanlink.shopping.entity.User;
import com.lanlink.shopping.dto.RegisterDTO;
import com.lanlink.shopping.mapper.RoleMapper;
import com.lanlink.shopping.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 用户服务：修改密码强规则（8-20 位含大小写字母/数字/特殊字符）、原密码校验、换绑手机唯一性
 */
class UserServiceTest {

    private UserMapper userMapper;
    private RoleMapper roleMapper;
    private UserService service;
    private User user;

    @BeforeEach
    void setUp() {
        userMapper = mock(UserMapper.class);
        roleMapper = mock(RoleMapper.class);
        service = new UserService(userMapper, roleMapper, new BCryptPasswordEncoder());
        user = new User();
        user.setUserId(1L);
        user.setPhone("13900000001");
        user.setPassword(new BCryptPasswordEncoder().encode("OldPass123!"));
        user.setNickname("测试用户");
        when(userMapper.selectById(1L)).thenReturn(user);
    }

    @Test
    void changePasswordSuccess() {
        service.changePassword(1L, "OldPass123!", "NewPass456@");
        assertTrue(new BCryptPasswor���q�^
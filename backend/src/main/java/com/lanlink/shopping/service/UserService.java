package com.lanlink.shopping.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lanlink.shopping.common.BusinessException;
import com.lanlink.shopping.dto.LoginDTO;
import com.lanlink.shopping.dto.RegisterDTO;
import com.lanlink.shopping.entity.Role;
import com.lanlink.shopping.entity.User;
import com.lanlink.shopping.mapper.RoleMapper;
import com.lanlink.shopping.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 用户与鉴权服务
 */
@Service
public class UserService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserMapper userMapper, RoleMapper roleMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public User register(RegisterDTO dto) {
        if (userMapper.selectCount(Wrappers.<User>lambdaQuery().eq(User::getPhone, dto.getPhone())) > 0) {
            throw new BusinessException("该手机号已注册");
        }
        Role role = roleMapper.selectOne(Wrappers.<Role>lambdaQuery().eq(Role::getRoleCode, dto.getRoleCode()));
        if (role == null) {
            throw new BusinessException("角色不存在: " + dto.getRoleCode());
        }
        User user = new User();
        user.setPhone(dto.getPhone());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(StringUtils.hasText(dto.getNickname()) ? dto.getNickname() : "用户" + dto.getPhone().substring(7));
        user.setRoleId(role.getRoleId());
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
        return user;
    }

    public User login(LoginDTO dto) {
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getPhone, dto.getPhone()));
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("手机号或密码错误");
        }
        return user;
    }

    public Role findRole(Long roleId) {
        return roleMapper.selectById(roleId);
    }

    public void changePassword(Long userId, String oldPw, String newPw) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException("用户不存在");
        if (!passwordEncoder.matches(oldPw, user.getPassword())) throw new BusinessException("原密码错误");
        if (newPw == null || newPw.length() < 8) throw new BusinessException("新密码至少 8 位");
        if (!newPw.matches(".*[a-zA-Z].*") || !newPw.matches(".*\\d.*"))
            throw new BusinessException("新密码需同时包含字母和数字");
        user.setPassword(passwordEncoder.encode(newPw));
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
    }
}

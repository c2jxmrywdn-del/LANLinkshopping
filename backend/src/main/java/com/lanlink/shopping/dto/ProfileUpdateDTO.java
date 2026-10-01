package com.lanlink.shopping.dto;

import lombok.Data;

/** 个人资料更新（含敏感字段，写库时按 TypeHandler 加密） */
@Data
public class ProfileUpdateDTO {
    private String realName;
    private String gender;
    private String birthday;
    private String avatar;
    private String bio;
    private String phone;
    private String email;
    private String idCard;
    private String bankAccount;
    private String address;
}

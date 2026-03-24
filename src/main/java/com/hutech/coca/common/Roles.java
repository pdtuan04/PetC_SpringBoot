package com.hutech.coca.common;


import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum Roles {
    ADMIN(1), // Vai trò quản trị viên, có quyền cao nhất trong hệ thống.
    USER(2), // Vai trò người dùng bình thường, có quyền hạn giới hạn.
    STAFF(3); // Có được 1 số quyền được chỉ định
    public final long value;
}
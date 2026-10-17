package com.stego.common;

import com.stego.common.model.User;

public interface UserDao {
    User findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    int create(User user);
}

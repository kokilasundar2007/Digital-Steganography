package com.stego.common;

import com.stego.common.exception.AuthException;
import com.stego.common.model.User;

public interface AuthService {
    User login(String username, String password) throws AuthException;
    void register(String fullName, String email, String username, String password) throws AuthException;
}

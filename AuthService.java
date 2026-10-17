package com.stego.common;

import com.stego.common.exception.AuthException;
import com.stego.common.model.User;

/** Implemented by M4 (AuthServiceImpl). Used by M1. */
public interface AuthService {

    /** @throws AuthException wrong username/password or account locked */
    User login(String username, String password) throws AuthException;

    /** @throws AuthException e.g. "Username already exists" (message is shown to the user) */
    void register(String fullName, String email, String username, String password) throws AuthException;
}

package com.stego.common;

import com.stego.common.model.User;

/**
 * Implemented by M4 (UserDaoImpl). Internal: used only by AuthServiceImpl.
 * No checked exceptions: wrap SQLException in a RuntimeException.
 */
public interface UserDao {

    /** @return the user (including password hash) or null if not found */
    User findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    /** Saves a new user. The id inside the given User is ignored. @return the new id */
    int create(User user);
}

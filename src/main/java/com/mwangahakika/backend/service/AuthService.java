package com.mwangahakika.backend.service;

import com.mwangahakika.backend.dto.AuthResponse;
import com.mwangahakika.backend.dto.LoginRequest;
import com.mwangahakika.backend.dto.RegisterRequest;
import com.mwangahakika.backend.dto.RegisterResponse;

public interface AuthService {
    AuthResponse login(LoginRequest request);

    RegisterResponse register(RegisterRequest request);
}

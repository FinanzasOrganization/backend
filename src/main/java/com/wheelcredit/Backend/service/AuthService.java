package com.wheelcredit.Backend.service;

import com.wheelcredit.Backend.dto.AuthenticationResponse;
import com.wheelcredit.Backend.dto.LoginDto;
import com.wheelcredit.Backend.dto.RegisterRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public interface AuthService {
    public abstract AuthenticationResponse register(RegisterRequest registerRequest);

    public abstract AuthenticationResponse login(LoginDto loginRequest);

    public void validateRegisterRequest(RegisterRequest registerRequest);
    public void existsUserByEmail(RegisterRequest registerRequest);
    public void refreshToken(HttpServletRequest request, HttpServletResponse response) throws IOException;
}

package com.wheelcredit.Backend.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wheelcredit.Backend.dto.AuthenticationResponse;
import com.wheelcredit.Backend.dto.LoginDto;
import com.wheelcredit.Backend.dto.RegisterRequest;
import com.wheelcredit.Backend.exception.ValidationException;
import com.wheelcredit.Backend.model.Client;
import com.wheelcredit.Backend.model.Roles;
import com.wheelcredit.Backend.model.Token;
import com.wheelcredit.Backend.model.TokenType;
import com.wheelcredit.Backend.repository.ClientRepository;
import com.wheelcredit.Backend.repository.TokenRepository;
import com.wheelcredit.Backend.service.AuthService;
import com.wheelcredit.Backend.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private TokenRepository tokenRepository;

    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    @Override
    public AuthenticationResponse register(RegisterRequest registerRequest) {
        var client = Client.builder()
                .name(registerRequest.getName())
                .lastname(registerRequest.getLastname())
                .email(registerRequest.getEmail())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .role(Roles.USER)
                .build();
        var savedClient = clientRepository. save(client);
        var jwtToken = jwtService.generateToken(client);
        var refreshToken = jwtService.generateRefreshToken(client);
        saveUserToken(savedClient, jwtToken);
        return AuthenticationResponse.builder()
                .user_id(client.getId())
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .build();

    }

    @Override
    public AuthenticationResponse login(LoginDto loginRequest) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()));
        var user = clientRepository.findByEmail(loginRequest.getEmail());
        var jwtToken = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);
        revokeAllUserTokens(user);
        saveUserToken(user, jwtToken);
        return AuthenticationResponse.builder()
                .user_id(user.getId())
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Override
    public void refreshToken(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        final String refreshToken;
        final String userEmail;
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return;
        }
        refreshToken = authHeader.substring(7);
        userEmail = jwtService.extractUsername(refreshToken);
        if (userEmail != null) {
            var user = this.clientRepository.findByEmail(userEmail);
            if (jwtService.isTokenValid(refreshToken, user)) {
                var accessToken = jwtService.generateToken(user);
                revokeAllUserTokens(user);
                saveUserToken(user, accessToken);
                var authResponse = AuthenticationResponse.builder()
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .build();
                new ObjectMapper().writeValue(response.getOutputStream(), authResponse);
            }
        }

    }

    private void saveUserToken(Client user, String jwtToken) {
        var token = Token.builder()
                .client(user)
                .token(jwtToken)
                .tokenType(TokenType.BEARER)
                .expired(false)
                .revoked(false)
                .build();
        tokenRepository.save(token);
    }

    private void revokeAllUserTokens(Client user) {
        var validUserTokens = tokenRepository.findAllValidTokenByClient(user.getId());
        if (validUserTokens.isEmpty())
            return;
        validUserTokens.forEach(token -> {
            token.setExpired(true);
            token.setRevoked(true);
        });
        tokenRepository.saveAll(validUserTokens);
    }

    public void validateRegisterRequest(RegisterRequest registerRequest)
    {
        if(registerRequest.getName()==null  ||
                registerRequest.getName().isEmpty())
        {
            throw new ValidationException("El nombre del usuario debe ser obligatorio");
        }
        if(registerRequest.getName().length()>50)
        {
            throw new ValidationException("El nombre del usuario no debe exceder los 50 caracteres");
        }
        if(registerRequest.getLastname()==null || registerRequest.getLastname().isEmpty())
        {
            throw new ValidationException("El apellido del usuario debe ser obligatorio");
        }
        if(registerRequest.getLastname().length()>50)
        {
            throw new ValidationException("El apellido del usuario no debe exceder los 50 caracteres");
        }
        if (registerRequest.getEmail() == null || registerRequest.getEmail().isEmpty()) {
            throw new ValidationException("El email del usuario debe ser obligatorio");
        }
        if (registerRequest.getEmail().length() > 50) {
            throw new ValidationException("El email del usuario no debe exceder los 50 caracteres");
        }
        if (registerRequest.getPassword() == null || registerRequest.getPassword().isEmpty()) {
            throw new ValidationException("La contraseña del usuario debe ser obligatorio");
        }
        if (registerRequest.getPassword().length() > 100) {
            throw new ValidationException("La contraseña del usuario no debe exceder los 100 caracteres");
        }
    }

    public void existsUserByEmail(RegisterRequest registerRequest) {
        if (clientRepository.existsByEmail(registerRequest.getEmail())) {
            throw new ValidationException("Ya existe un usuario con el email " + registerRequest.getEmail());
        }
    }

}

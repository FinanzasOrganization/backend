package com.wheelcredit.Backend.controller;

import com.wheelcredit.Backend.dto.AuthenticationResponse;
import com.wheelcredit.Backend.dto.LoginDto;
import com.wheelcredit.Backend.dto.RegisterRequest;
import com.wheelcredit.Backend.repository.ClientRepository;
import com.wheelcredit.Backend.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/go-finance/v1/auth")
public class AuthenticationController {
    @Autowired
    private AuthService authService;
    @Autowired
    private ClientRepository clientRepository;

    // URL: http://localhost:8090/api/wheel-credit/v1/auth/signup
    // Method: POST
    @Transactional
    @PostMapping("/signup")
    public ResponseEntity<AuthenticationResponse> registerClient(@RequestBody RegisterRequest request){
        authService.existsUserByEmail(request);
        authService.validateRegisterRequest(request);
        AuthenticationResponse registeredClient = authService.register(request);
        return new ResponseEntity<AuthenticationResponse>(registeredClient, HttpStatus.CREATED);
    }

    // URL: http://localhost:8090/api/wheel-credit/v1/auth/login
    // Method: POST
    @Transactional(readOnly = true)
    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(@RequestBody LoginDto request) {

        AuthenticationResponse loggedClient = authService.login(request);

        return new ResponseEntity<AuthenticationResponse>(loggedClient, HttpStatus.OK);
    }
    //http://localhost:8090/api/wheel-credit/v1/auth/refresh-token
    @PostMapping("/refresh-token")
    public void refreshToken(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        authService.refreshToken(request, response);
    }

}

package com.wheelcredit.Backend.service;

import io.jsonwebtoken.Claims;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Map;
import java.util.function.Function;

public interface JwtService {

    public abstract String extractUsername(String token);

    public abstract <T> T extractClaim(String token, Function<Claims, T> claimsResolver);

    public abstract String generateToken(UserDetails userDetails);

    public abstract String generateToken(Map<String, Object> extraClaims,
                                         UserDetails userDetails);

    public abstract String generateRefreshToken(UserDetails userDetails);

    public abstract boolean isTokenValid(String token, UserDetails userDetails);

}

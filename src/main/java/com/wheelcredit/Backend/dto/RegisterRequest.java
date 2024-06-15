package com.wheelcredit.Backend.dto;

import com.wheelcredit.Backend.model.Roles;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RegisterRequest {
    private String name;
    private String lastname;
    private String email;
    private String password;
    private Roles role;
}

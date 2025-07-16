package com.fspl.medica_healthcare.models;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthRequest {

    @Email(message = "Username must be a valid email address")
    @Pattern(
            regexp = "^[a-zA-Z][a-zA-Z0-9_.-]*@[a-zA-Z]+\\.[a-zA-Z]{2,}$",
            message = "Username must be a valid email address and should not start with a number or contain invalid domains"
    )
    private String username;

    @NotBlank(message = "Password is mandatory")
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}

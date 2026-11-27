package com.marcela_paz.backend.dto;

public record ResetPasswordRequest(String email, String codigo, String nuevaPassword) {
}

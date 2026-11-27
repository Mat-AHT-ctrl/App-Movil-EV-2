package com.marcela_paz.backend.dto;

public record ChangePasswordRequest(String email, String passwordActual, String nuevaPassword) {
}

package com.ledgerbid.api.dto;

public record LoginResponse(String token, UserDto user) {
}

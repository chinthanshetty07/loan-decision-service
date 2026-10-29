package com.chinthanshetty.los.service;

/** The authenticated user and the institution (bank) their token is scoped to. */
public record Caller(String username, String institutionCode) {
}

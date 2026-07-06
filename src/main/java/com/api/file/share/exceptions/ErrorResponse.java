package com.api.file.share.exceptions;

public record ErrorResponse(String statusCode, String title, String detail) {
}

package com.saigonplantravel.backend.media;

public record MediaUpload(byte[] content, String contentType, String storageKey) {}

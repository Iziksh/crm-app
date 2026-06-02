package com.crm.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ActivityNoteRequest(@NotBlank String text) {}

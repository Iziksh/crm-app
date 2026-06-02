package com.crm.dto.response;

import java.util.List;

public record ImportResultResponse(int imported, int skipped, List<String> errors) {}

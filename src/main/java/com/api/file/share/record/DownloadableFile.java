package com.api.file.share.record;

import lombok.Builder;
import org.springframework.core.io.Resource;

@Builder
public record DownloadableFile(
        Resource resource,
        String filename,
        String contentType
) {
}

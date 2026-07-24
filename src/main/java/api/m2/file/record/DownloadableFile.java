package api.m2.file.record;

import lombok.Builder;
import org.springframework.core.io.Resource;

@Builder
public record DownloadableFile(
        Resource resource,
        String filename,
        String contentType
) {
}

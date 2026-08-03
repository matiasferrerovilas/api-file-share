package api.m2.file.record;

import lombok.Builder;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Builder
public record DownloadableFile(
        StreamingResponseBody body,
        String filename,
        String contentType
) {
}

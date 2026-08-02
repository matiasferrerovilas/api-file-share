package api.m2.file.record;

import api.m2.file.enums.SharePermission;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record FileShareResponse(
        Long id,
        Long fileId,
        String apiName,
        SharePermission permission,
        LocalDateTime createdAt
) {
}

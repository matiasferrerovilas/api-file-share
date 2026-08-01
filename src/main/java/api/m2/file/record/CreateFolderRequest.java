package api.m2.file.record;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateFolderRequest(
        @NotNull(message = "El workspace es requerido")
        Long workspaceId,
        Long parentId,
        @NotBlank(message = "El nombre es requerido")
        String name) {
}

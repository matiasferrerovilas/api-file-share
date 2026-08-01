package api.m2.file.record;

import jakarta.validation.constraints.NotBlank;

public record RenameNodeRequest(
        @NotBlank(message = "El nombre es requerido")
        String name) {
}

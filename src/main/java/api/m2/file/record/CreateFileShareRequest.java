package api.m2.file.record;

import api.m2.file.enums.SharePermission;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateFileShareRequest(
        @NotNull(message = "El archivo o carpeta es requerido")
        Long fileId,
        @NotBlank(message = "El nombre de la api es requerido")
        String apiName,
        @NotNull(message = "El permiso es requerido")
        SharePermission permission) {
}

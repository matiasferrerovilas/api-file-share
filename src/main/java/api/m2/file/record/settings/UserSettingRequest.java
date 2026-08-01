package api.m2.file.record.settings;

import jakarta.validation.constraints.NotNull;

public record UserSettingRequest(
        @NotNull(message = "El valor es requerido")
        Long value) {
}

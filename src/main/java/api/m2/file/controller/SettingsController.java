package api.m2.file.controller;

import api.m2.file.enums.UserSettingKey;
import api.m2.file.record.settings.UserSettingRequest;
import api.m2.file.record.settings.UserSettingResponse;
import api.m2.file.service.settings.UserSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/settings")
@Tag(name = "Settings", description = "API para la configuración")
public class SettingsController {
    private final UserSettingService userSettingService;

    @Operation(
            summary = "Obtener default por clave",
            description = "Retorna el setting de default para una clave específica (DEFAULT_WORKSPACE)"
    )
    @ApiResponse(responseCode = "200", description = "Default encontrado")
    @ApiResponse(responseCode = "404", description = "Default no configurado para esa clave")
    @GetMapping("/defaults/{key}")
    public UserSettingResponse getDefaultByKey(@PathVariable UserSettingKey key) {
        return userSettingService.getByKey(key);
    }

    @Operation(
            summary = "Setear o actualizar un default",
            description = "Crea o actualiza el default para una clave específica"
    )
    @ApiResponse(responseCode = "200", description = "Default actualizado")
    @PutMapping("/defaults/{key}")
    @ResponseStatus(HttpStatus.OK)
    public UserSettingResponse upsertDefault(@PathVariable UserSettingKey key,
                                             @RequestBody @Valid UserSettingRequest request) {
        return userSettingService.upsert(key, request.value());
    }
}

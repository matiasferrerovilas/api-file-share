package com.api.file.share.controller;

import com.api.file.share.record.UserMeRecord;
import com.api.file.share.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/users")
@Tag(name = "Users", description = "API de usuarios")
public class UserController {

    private final UserService userService;

    @Operation(
            summary = "Obtener datos del usuario autenticado",
            description = "Retorna el ID interno, email, estado de onboarding y tipo de usuario del usuario autenticado. "
                    + "Si el usuario no existe aún en la base de datos, se crea a partir de los claims del JWT y se persiste.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Datos del usuario autenticado",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = UserMeRecord.class)
                            )
                    )
            }
    )
    @GetMapping("/me")
    public UserMeRecord getMe() {
        return userService.getMe();
    }
}

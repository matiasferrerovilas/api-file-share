package api.m2.file.controller;

import api.m2.file.clients.identity.requests.AddWorkspaceRecord;
import api.m2.file.clients.identity.response.WorkspaceMemberDTO;
import api.m2.file.service.workspace.WorkspaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/workspace")
@Tag(name = "Workspaces", description = "API para la gestión de workspaces")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    @Operation(
            summary = "Crear un nuevo workspace",
            description = "Crea un workspace asociado al usuario autenticado.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Workspace creado correctamente")
            }
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void createWorkspace(@Valid @RequestBody AddWorkspaceRecord body) {
        workspaceService.createWorkspace(body);
    }

    @Operation(
            summary = "Listar workspaces del usuario",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Workspaces obtenidos correctamente")
            }
    )
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<WorkspaceMemberDTO> getWorkspaces() {
        return workspaceService.getWorkspaces();
    }
}

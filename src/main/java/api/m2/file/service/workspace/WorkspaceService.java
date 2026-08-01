package api.m2.file.service.workspace;

import api.m2.file.clients.identity.IdentityClient;
import api.m2.file.clients.identity.requests.AddWorkspaceRecord;
import api.m2.file.clients.identity.response.WorkspaceAdded;
import api.m2.file.clients.identity.response.WorkspaceMemberDTO;
import api.m2.file.exceptions.BusinessException;
import api.m2.file.exceptions.PermissionDeniedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkspaceService {
    private final IdentityClient identityClient;

    public List<WorkspaceMemberDTO> getWorkspaces() {
        return identityClient.getWorkspaces();
    }

    public void verifyUserIsMemberOfWorkspace(Long workspaceId, Long userId) {
        try {
            identityClient.verifyMembership(workspaceId, userId);
        } catch (RestClientResponseException e) {
            throw new PermissionDeniedException("No tienes permiso para operar sobre este recurso");
        }
    }

    @Transactional
    public void createWorkspace(AddWorkspaceRecord addWorkspaceRecord) {
        if (addWorkspaceRecord.description() == null || addWorkspaceRecord.description().isBlank()) {
            throw new BusinessException("La descripción del workspace no puede estar vacía");
        }
        identityClient.createWorkspaces(List.of(addWorkspaceRecord));
    }

    public List<WorkspaceAdded> createWorkspaces(List<AddWorkspaceRecord> workspacesToAdd) {
        return identityClient.createWorkspaces(workspacesToAdd);
    }
}

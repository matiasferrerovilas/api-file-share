package api.m2.file.service.onboarding;

import api.m2.file.clients.identity.IdentityClient;
import api.m2.file.clients.identity.requests.AddWorkspaceRecord;
import api.m2.file.enums.UserSettingKey;
import api.m2.file.record.onboarding.OnBoardingForm;
import api.m2.file.service.FileService;
import api.m2.file.service.UserService;
import api.m2.file.service.settings.UserSettingService;
import api.m2.file.service.workspace.WorkspaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OnboardingService {
    private static final String DEFAULT_WORKSPACE_NAME = "DEFAULT";

    private final IdentityClient identityClient;
    private final FileService fileService;
    private final UserService userService;
    private final WorkspaceService workspaceService;
    private final UserSettingService userSettingService;

    @Transactional(rollbackFor = Exception.class)
    public void finish(@Valid OnBoardingForm onBoardingForm) {
        var owner = userService.createLogInUser();

        var defaultWorkspace = workspaceService
                .createWorkspaces(List.of(new AddWorkspaceRecord(DEFAULT_WORKSPACE_NAME)))
                .getFirst();

        userSettingService.upsertForUser(owner.id(), UserSettingKey.DEFAULT_WORKSPACE, defaultWorkspace.id());

        fileService.getPersonalFolder(defaultWorkspace.id());
        onBoardingForm.filesToAdd().forEach(file -> fileService.uploadFile(defaultWorkspace.id(), null, file));

        identityClient.changeUserFirstLoginStatus(owner.id());
    }

    public void markTourAsSeen() {
        identityClient.markTourAsSeen();
    }
}

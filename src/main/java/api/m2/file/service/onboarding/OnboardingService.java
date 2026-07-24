package api.m2.file.service.onboarding;

import api.m2.file.clients.identity.IdentityClient;
import api.m2.file.clients.identity.response.UserMe;
import api.m2.file.configuration.properties.StorageProperties;
import api.m2.file.entity.FileEntity;
import api.m2.file.enums.FileType;
import api.m2.file.record.onboarding.OnBoardingForm;
import api.m2.file.repository.FileRepository;
import api.m2.file.service.FileService;
import api.m2.file.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class OnboardingService {
    private static final String ROOT_PATH = "Home";
    private final IdentityClient identityClient;
    private final FileRepository fileRepository;
    private final StorageProperties storageProperties;
    private final FileService fileService;
    private final UserService userService;

    public void finish(@Valid OnBoardingForm onBoardingForm) {
        var owner =userService.createLogInUser();
        this.createRootFolder(owner);
        onBoardingForm.filesToAdd().forEach( file -> fileService.uploadFile(null, file));

        identityClient.changeUserFirstLoginStatus(owner.id());
    }

    public void markTourAsSeen() {
        identityClient.markTourAsSeen();
    }

    private void createRootFolder(UserMe owner) {

        LocalDateTime now = LocalDateTime.now();
        FileEntity root = FileEntity.builder()
                .ownerId(owner.id())
                .workspaceId(12L)
                .name(ROOT_PATH)
                .type(FileType.FOLDER)
                .location("%s/%s".formatted(storageProperties.basePath(), owner.id()))
                .createdAt(now)
                .updatedAt(now)
                .build();

        fileRepository.save(root);
    }
}

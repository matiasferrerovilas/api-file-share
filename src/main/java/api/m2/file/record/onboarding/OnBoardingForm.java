package api.m2.file.record.onboarding;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public record OnBoardingForm(
        List<MultipartFile> filesToAdd,
        Long existingDefaultWorkspaceId,
        List<String> workspacesToAdd) {}

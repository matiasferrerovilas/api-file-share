package api.m2.file.clients.identity;

import api.m2.file.clients.identity.requests.UserToAdd;
import api.m2.file.clients.identity.response.UserMe;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PatchExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;

@HttpExchange
public interface IdentityClient {

    @PostExchange("/v1/users")
    UserMe createLogInUser(@RequestBody UserToAdd user);

    @PatchExchange("/v1/onboarding/{userId}/first-login")
    void changeUserFirstLoginStatus(@PathVariable Long userId);

    @GetExchange("/v1/users/me")
    UserMe getMe();


    @GetExchange("/v1/workspaces/{workspaceId}/members/{userId}")
    void verifyMembership(@PathVariable Long workspaceId, @PathVariable Long userId);

    @DeleteExchange("/v1/workspaces/{workspaceId}")
    void leaveWorkspace(@PathVariable Long workspaceId);

    @PutExchange("/v1/onboarding/tour")
    void markTourAsSeen();

}

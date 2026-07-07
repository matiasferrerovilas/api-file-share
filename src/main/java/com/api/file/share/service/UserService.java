package com.api.file.share.service;

import com.api.file.share.entity.UserEntity;
import com.api.file.share.exceptions.PermissionDeniedException;
import com.api.file.share.exceptions.ServiceException;
import com.api.file.share.record.UserMeRecord;
import com.api.file.share.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private static final String REALM_ACCESS_CLAIM = "realm_access";
    private static final String ROLES_CLAIM = "roles";
    private static final List<String> ROLE_PRIORITY = List.of("ROLE_ADMIN", "ROLE_FAMILY", "ROLE_GUEST");
    private static final String DEFAULT_USER_TYPE = "GUEST";
    private static final String ROLE_PREFIX = "ROLE_";

    private final UserRepository userRepository;

    @Transactional
    public UserMeRecord getMe() {
        Jwt jwt = currentJwt();
        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank()) {
            throw new ServiceException("El token no contiene el claim 'email'");
        }

        UserEntity user = userRepository.findByEmail(email)
                .orElseGet(() -> createUser(jwt, email));

        return toRecord(user);
    }

    private Jwt currentJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            throw new PermissionDeniedException("Usuario no autenticado");
        }
        return jwtAuthentication.getToken();
    }

    private UserEntity createUser(Jwt jwt, String email) {
        LocalDateTime now = LocalDateTime.now();

        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setGivenName(jwt.getClaimAsString("given_name"));
        user.setFamilyName(jwt.getClaimAsString("family_name"));
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user.setFirstLogin(true);
        user.setHasSeenTour(false);
        user.setUserType(resolveUserType(jwt));

        log.info("Creando usuario nuevo: {}", email);
        return userRepository.save(user);
    }

    private String resolveUserType(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap(REALM_ACCESS_CLAIM);
        if (realmAccess == null) {
            return DEFAULT_USER_TYPE;
        }

        @SuppressWarnings("unchecked")
        Collection<String> roles = (Collection<String>) realmAccess.get(ROLES_CLAIM);
        if (roles == null) {
            return DEFAULT_USER_TYPE;
        }

        return ROLE_PRIORITY.stream()
                .filter(roles::contains)
                .findFirst()
                .map(role -> role.substring(ROLE_PREFIX.length()))
                .orElse(DEFAULT_USER_TYPE);
    }

    private UserMeRecord toRecord(UserEntity user) {
        return new UserMeRecord(
                user.getId(),
                user.getEmail(),
                user.getGivenName(),
                user.getFamilyName(),
                user.isFirstLogin(),
                user.getUserType(),
                user.isHasSeenTour()
        );
    }
}

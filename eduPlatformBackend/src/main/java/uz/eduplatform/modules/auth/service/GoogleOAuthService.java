package uz.eduplatform.modules.auth.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uz.eduplatform.core.common.exception.BusinessException;

import java.util.Collections;
import java.util.Map;

@Slf4j
@Service
public class GoogleOAuthService {

    private final GoogleIdTokenVerifier verifier;
    private final boolean enabled;

    public GoogleOAuthService(@Value("${app.google.client-id:}") String clientId) {
        this.enabled = clientId != null && !clientId.isBlank();
        if (this.enabled) {
            this.verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance()
            ).setAudience(Collections.singletonList(clientId)).build();
        } else {
            this.verifier = null;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public GoogleUserInfo verifyIdToken(String idToken) {
        if (!enabled) {
            throw BusinessException.ofKey("auth.google.not.configured");
        }
        try {
            GoogleIdToken token = verifier.verify(idToken);
            if (token == null) {
                throw BusinessException.ofKey("auth.google.token.invalid");
            }
            GoogleIdToken.Payload payload = token.getPayload();
            return GoogleUserInfo.builder()
                    .id(payload.getSubject())
                    .email(payload.getEmail())
                    .emailVerified(payload.getEmailVerified())
                    .name((String) payload.get("name"))
                    .givenName((String) payload.get("given_name"))
                    .familyName((String) payload.get("family_name"))
                    .picture((String) payload.get("picture"))
                    .build();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to verify Google ID token", e);
            throw BusinessException.ofKey("auth.google.token.invalid");
        }
    }

    @SuppressWarnings("unchecked")
    public GoogleUserInfo verifyAccessToken(String accessToken) {
        if (!enabled) {
            throw BusinessException.ofKey("auth.google.not.configured");
        }
        try {
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<Map> response = restTemplate.getForEntity(
                    "https://www.googleapis.com/oauth2/v3/userinfo?access_token=" + accessToken,
                    Map.class
            );
            Map<String, Object> body = response.getBody();
            if (body == null || body.containsKey("error")) {
                throw BusinessException.ofKey("auth.google.token.invalid");
            }
            return GoogleUserInfo.builder()
                    .id((String) body.get("sub"))
                    .email((String) body.get("email"))
                    .emailVerified(Boolean.TRUE.equals(body.get("email_verified")))
                    .name((String) body.get("name"))
                    .givenName((String) body.get("given_name"))
                    .familyName((String) body.get("family_name"))
                    .picture((String) body.get("picture"))
                    .build();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to verify Google access token", e);
            throw BusinessException.ofKey("auth.google.token.invalid");
        }
    }

    @Data
    @Builder
    public static class GoogleUserInfo {
        private String id;
        private String email;
        private Boolean emailVerified;
        private String name;
        private String givenName;
        private String familyName;
        private String picture;
    }
}

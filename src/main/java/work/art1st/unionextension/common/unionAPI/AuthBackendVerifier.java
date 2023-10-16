package work.art1st.unionextension.common.unionAPI;

import lombok.Getter;
import lombok.SneakyThrows;
import net.kyori.adventure.text.Component;
import work.art1st.unionextension.UnionExtension;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AuthBackendVerifier {
    public interface Callback {
        void kick(Component reason);
    }

    public static final String DEFAULT_UNION_QUERY_API = "https://skin.mualliance.ltd/api/union/backend/{code}/security/level";
    public static final String DEFAULT_UNION_YGG_API = "https://skin.mualliance.ltd/api/union/yggdrasil";
    public static final String UNION_API_PROFILE_PROPERTY_NAME = "union_api";
    private final List<String> allowed;
    private final List<String> blocked;
    private final String unionQueryAPI;
    private final PublicKey unionPublicKey;

    public enum VerificationResult {
        ALLOWED,
        BLOCKED,
        ERROR
    }

    @Getter
    private static class YggdrasilAPIResponse {
        private String signaturePublicKey;
    }
    public AuthBackendVerifier(List<String> allowed, List<String> blocked, String unionQueryAPI, String unionYggAPI) throws IOException, InterruptedException, NoSuchAlgorithmException, InvalidKeySpecException, URISyntaxException {
        this.allowed = allowed;
        this.blocked = blocked;
        this.unionQueryAPI = unionQueryAPI;
        String yggdrasilAPIresponse = httpClient.send(HttpRequest.newBuilder()
                .uri(new URL(unionYggAPI).toURI())
                .GET()
                .build(), HttpResponse.BodyHandlers.ofString()).body();
        UnionExtension.getLogger().info(yggdrasilAPIresponse);
        this.unionPublicKey = RSAUtil.convertStringToPublicKey(UnionExtension.getGson().fromJson(yggdrasilAPIresponse, YggdrasilAPIResponse.class).getSignaturePublicKey());

        if (!allowed.isEmpty() && !blocked.isEmpty()) {
            UnionExtension.getLogger().warn("Note: You enabled both whitelist and blacklist of skin service backends. The plugin will use the WHITELIST ONLY.");
        }
    }

    HttpClient httpClient = HttpClient.newHttpClient();
    protected URI buildQueryURI(String backendCode) throws MalformedURLException, URISyntaxException {
        return new URL(unionQueryAPI.replace("{code}", backendCode)).toURI();
    }

    @SneakyThrows
    public boolean isBackendInfoSigned(String backendInfoJson, String signature) {
        return RSAUtil.validate(backendInfoJson, signature, unionPublicKey);
    }

    public VerificationResult isBackendAllowed(String backendInfoJson) {
        BackendInfo backendInfo = UnionExtension.getGson().fromJson(backendInfoJson, BackendInfo.class);
        try {
            Pattern slx = Pattern.compile("^SL\\d+$");
            if (!allowed.isEmpty()) {
                for (String allowedBackend :
                        allowed) {
                    Matcher matcher = slx.matcher(allowedBackend);
                    if (matcher.find()) {
                        int sl = Integer.parseInt(allowedBackend.substring(2));
                        int level = Integer.parseInt(httpClient.send(HttpRequest.newBuilder()
                                .uri(buildQueryURI(backendInfo.getBackendScopes().getSelf()))
                                .GET()
                                .build(), HttpResponse.BodyHandlers.ofString()).body());
                        if (level >= sl) {
                            return VerificationResult.ALLOWED;
                        }
                    } else {
                        for (String backend :
                                backendInfo.getBackendScopes().getAll()) {
                            if (backend.equals(allowedBackend)) {
                                return VerificationResult.ALLOWED;
                            }
                        }
                    }
                }
                return VerificationResult.BLOCKED;
            } else {
                for (String blockedBackend :
                        blocked) {
                    Matcher matcher = slx.matcher(blockedBackend);
                    if (matcher.find()) {
                        int sl = Integer.parseInt(blockedBackend.substring(2));
                        int level = Integer.parseInt(httpClient.send(HttpRequest.newBuilder()
                                .uri(buildQueryURI(backendInfo.getBackendScopes().getSelf()))
                                .GET()
                                .build(), HttpResponse.BodyHandlers.ofString()).body());
                        if (level <= sl) {
                            return VerificationResult.BLOCKED;
                        }
                    }
                    for (String backend :
                            backendInfo.getBackendScopes().getAll()) {
                        if (backend.equals(blockedBackend)) {
                            return VerificationResult.BLOCKED;
                        }
                    }
                }
                return VerificationResult.ALLOWED;
            }
        } catch (IOException | URISyntaxException | InterruptedException e) {
            return VerificationResult.ERROR;
        }
    }
}

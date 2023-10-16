package work.art1st.unionextension.platform.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.util.GameProfile;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;
import work.art1st.unionextension.BuildConstants;
import work.art1st.unionextension.UnionExtension;
import work.art1st.unionextension.common.unionAPI.AuthBackendVerifier;

import java.nio.file.Path;

@Plugin(
        id = "union-extension",
        name = "UnionExtension",
        version = BuildConstants.VERSION
)
public class VelocityMain {

    @Inject
    private Logger logger;
    @DataDirectory
    @Inject
    private Path dataDirectory;

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        UnionExtension.initialize(logger, dataDirectory);
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        if (!UnionExtension.isEnabled()) {
            return;
        }
        for (GameProfile.Property property:
            event.getPlayer().getGameProfileProperties()) {
            if (property.getName().equals(AuthBackendVerifier.UNION_API_PROFILE_PROPERTY_NAME)) {
                if (!UnionExtension.getVerifier().isBackendInfoSigned(property.getValue(), property.getSignature())) {
                    event.getPlayer().disconnect(Component.text("Authentication backend signature validation failure."));
                    return;
                }
                AuthBackendVerifier.VerificationResult result =
                        UnionExtension.getVerifier().isBackendAllowed(property.getValue());
                switch (result) {
                    case ERROR:
                        event.getPlayer().disconnect(Component.text("Error on authentication backend verification."));
                        return;
                    case BLOCKED:
                        event.getPlayer().disconnect(Component.text("Authentication backend is blocked on this server."));
                        return;
                    default:
                        return;
                }
            }
        }
    }
}

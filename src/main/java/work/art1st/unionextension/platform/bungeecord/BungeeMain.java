package work.art1st.unionextension.platform.bungeecord;

import net.md_5.bungee.UserConnection;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.protocol.Property;
import org.slf4j.LoggerFactory;
import work.art1st.unionextension.UnionExtension;
import work.art1st.unionextension.common.unionAPI.AuthBackendVerifier;

public class BungeeMain extends Plugin implements Listener {

    @Override
    public void onEnable() {
        UnionExtension.initialize(LoggerFactory.getLogger(getLogger().getClass()), getDataFolder().toPath());
    }

    @EventHandler
    public void onPostLoginEvent(PostLoginEvent event) {
        if (!UnionExtension.isEnabled()) {
            return;
        }
        assert event.getPlayer() instanceof UserConnection;
        for (Property property:
                ((UserConnection) event.getPlayer()).getPendingConnection().getLoginProfile().getProperties()) {
            if (property.getName().equals(AuthBackendVerifier.UNION_API_PROFILE_PROPERTY_NAME)) {
                if (!UnionExtension.getVerifier().isBackendInfoSigned(property.getValue(), property.getSignature())) {
                    event.getPlayer().disconnect(new TextComponent("Authentication backend signature validation failure."));
                    return;
                }
                AuthBackendVerifier.VerificationResult result =
                        UnionExtension.getVerifier().isBackendAllowed(property.getValue());
                switch (result) {
                    case ERROR:
                        event.getPlayer().disconnect(new TextComponent("Error on authentication backend verification."));
                        return;
                    case BLOCKED:
                        event.getPlayer().disconnect(new TextComponent("Authentication backend is blocked on this server."));
                        return;
                    default:
                        return;
                }
            }
        }
    }

}

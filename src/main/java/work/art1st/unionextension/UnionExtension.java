package work.art1st.unionextension;

import com.electronwill.nightconfig.core.file.FileConfig;
import com.google.gson.Gson;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import work.art1st.unionextension.common.unionAPI.AuthBackendVerifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;

public class UnionExtension {
    @Getter
    private static final Gson gson = new Gson();
    @Getter
    private static Logger logger;
    @Getter
    private static AuthBackendVerifier verifier;
    @Getter
    private static Path dataDirectory;
    @Getter
    private static boolean enabled;

    public static void initialize(Logger logger, @NotNull Path dataDirectory) {
        UnionExtension.logger = logger;
        UnionExtension.dataDirectory = dataDirectory;
        try {
            copyResourceFile("config.toml", "config.toml", false);
            FileConfig configFile = FileConfig.of(dataDirectory.resolve("config.toml").toFile());
            configFile.load();
            verifier = new AuthBackendVerifier(
                    configFile.getOrElse("union.auth-backend.allowed", new ArrayList<>()),
                    configFile.getOrElse("union.auth-backend.blocked", new ArrayList<>()),
                    configFile.getOrElse("union.auth-backend.union-query-api", AuthBackendVerifier.DEFAULT_UNION_QUERY_API),
                    configFile.getOrElse("union.auth-backend.union-ygg-api", AuthBackendVerifier.DEFAULT_UNION_YGG_API)
            );
            configFile.close();
            enabled = true;
        } catch (MalformedURLException | URISyntaxException e) {
            logger.error("Bad URL.");
            enabled = false;
        } catch (IOException e) {
            logger.error("Loading config failed.");
            enabled = false;
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            logger.error("Bad RSA public key.");
            enabled = false;
        } catch (InterruptedException e) {
            logger.error("Network error.");
            enabled = false;
        }
    }

    private static void copyResourceFile(String filename, String destination, boolean replace) throws IOException {
        File file = getDataDirectory().resolve(destination).toFile();
        if (!replace && file.exists()) {
            return;
        }
        file.delete();
        file.getParentFile().mkdirs();
        file.createNewFile();
        InputStream is = UnionExtension.class.getResourceAsStream("/" + filename);
        FileOutputStream fos = new FileOutputStream(getDataDirectory().resolve(destination).toString());
        byte[] b = new byte[1024];
        int length;
        while ((length = is.read(b)) > 0) {
            fos.write(b, 0, length);
        }
        is.close();
        fos.close();
    }
}

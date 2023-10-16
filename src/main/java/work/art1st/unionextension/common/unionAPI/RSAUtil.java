package work.art1st.unionextension.common.unionAPI;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class RSAUtil {
    public static PublicKey convertStringToPublicKey(String input) throws NoSuchAlgorithmException, InvalidKeySpecException {
        String publicKeyPEM = input
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replaceAll("\n", "")
                .replace("-----END PUBLIC KEY-----", "");

        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyPEM));
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return factory.generatePublic(keySpec);
    }

    public static boolean validate(String content, String signature, PublicKey publicKey) throws InvalidKeyException {
        try {
            Signature signer = Signature.getInstance("SHA1withRSA");
            signer.initVerify(publicKey);
            signer.update(content.getBytes());
            byte[] signatureBytes = Base64.getDecoder().decode(signature);
            return signer.verify(signatureBytes);
        } catch (NoSuchAlgorithmException | SignatureException e) {
            throw new RuntimeException(e);
        }
    }
}

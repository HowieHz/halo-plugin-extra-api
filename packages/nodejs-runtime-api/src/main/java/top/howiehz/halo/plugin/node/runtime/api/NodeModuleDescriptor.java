package top.howiehz.halo.plugin.node.runtime.api;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public record NodeModuleDescriptor(
    String moduleId,
    String source,
    String integrity
) {

    public NodeModuleDescriptor {
        NodeRuntimeValidator.requireModuleId(moduleId);
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("source must not be blank");
        }
        if (integrity == null || integrity.isBlank()) {
            throw new IllegalArgumentException("integrity must not be blank");
        }
    }

    public static NodeModuleDescriptor fromResource(ClassLoader classLoader, String moduleId,
        String resourcePath) {
        try (InputStream inputStream = classLoader.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("Resource not found: " + resourcePath);
            }
            byte[] bytes = inputStream.readAllBytes();
            String source = new String(bytes, StandardCharsets.UTF_8);
            return new NodeModuleDescriptor(moduleId, source, sha256(bytes));
        } catch (IOException e) {
            throw new NodeRuntimeException("Failed to read module resource: " + resourcePath, e);
        }
    }

    public static NodeModuleDescriptor fromResource(Class<?> owner, String moduleId,
        String resourcePath) {
        return fromResource(owner.getClassLoader(), moduleId, resourcePath);
    }

    private static String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return "sha256-" + HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
    }
}

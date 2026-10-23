package br.com.muxe.core.common.messaging.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class HmacEnvelopeSigner implements EnvelopeSigner {

    public static final int MIN_SECRET_BYTES = 32;

    private static final String ALGORITHM = "HmacSHA256";
    private static final int SIGNATURE_BYTES = 32;

    private final ThreadLocal<Mac> signingMac;
    private final List<ThreadLocal<Mac>> verificationMacs;

    private HmacEnvelopeSigner(byte[] signingKey, List<byte[]> previousKeys) {
        this.signingMac = macFor(signingKey);
        List<ThreadLocal<Mac>> macs = new ArrayList<>();
        macs.add(signingMac);
        for (byte[] key : previousKeys) {
            macs.add(macFor(key));
        }
        this.verificationMacs = List.copyOf(macs);
    }

    public static HmacEnvelopeSigner of(String secret) {
        return of(secret, List.of());
    }

    public static HmacEnvelopeSigner of(String secret, List<String> previousSecrets) {
        List<byte[]> previous = new ArrayList<>();
        for (String value : previousSecrets) {
            previous.add(keyBytes(value));
        }
        return new HmacEnvelopeSigner(keyBytes(secret), previous);
    }

    @Override
    public byte[] sign(byte[] data) {
        return signingMac.get().doFinal(data);
    }

    @Override
    public boolean verify(byte[] data, byte[] signature) {
        if (signature.length != SIGNATURE_BYTES) {
            return false;
        }
        for (ThreadLocal<Mac> mac : verificationMacs) {
            if (MessageDigest.isEqual(mac.get().doFinal(data), signature)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int signatureLength() {
        return SIGNATURE_BYTES;
    }

    private static byte[] keyBytes(String secret) {
        if (secret == null) {
            throw new IllegalArgumentException("Messaging secret is missing");
        }
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalArgumentException("Messaging secret must have at least " + MIN_SECRET_BYTES + " bytes");
        }
        return bytes;
    }

    private static ThreadLocal<Mac> macFor(byte[] key) {
        SecretKeySpec spec = new SecretKeySpec(key, ALGORITHM);
        return ThreadLocal.withInitial(() -> {
            try {
                Mac mac = Mac.getInstance(ALGORITHM);
                mac.init(spec);
                return mac;
            } catch (GeneralSecurityException exception) {
                throw new IllegalStateException("HMAC-SHA256 is not available", exception);
            }
        });
    }
}

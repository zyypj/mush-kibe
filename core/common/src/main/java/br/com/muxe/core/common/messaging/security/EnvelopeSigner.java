package br.com.muxe.core.common.messaging.security;

public interface EnvelopeSigner {

    byte[] sign(byte[] data);

    boolean verify(byte[] data, byte[] signature);

    int signatureLength();
}

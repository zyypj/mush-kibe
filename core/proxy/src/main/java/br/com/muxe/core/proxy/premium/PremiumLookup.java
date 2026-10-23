package br.com.muxe.core.proxy.premium;

@FunctionalInterface
public interface PremiumLookup {

    PremiumStatus lookup(String username);
}

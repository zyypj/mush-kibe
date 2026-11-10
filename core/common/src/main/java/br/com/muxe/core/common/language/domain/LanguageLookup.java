package br.com.muxe.core.common.language.domain;

import java.util.UUID;

@FunctionalInterface
public interface LanguageLookup {

    LanguageLookup DEFAULT = player -> Language.DEFAULT;

    Language of(UUID player);
}

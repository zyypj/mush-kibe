package br.com.muxe.core.common.preference.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class PreferencesTest {

    @Test
    void aNewPlayerHasTheReferenceDefaults() {
        Preferences preferences = Preferences.DEFAULTS;

        assertThat(preferences.on(Preference.CHAT)).isTrue();
        assertThat(preferences.on(Preference.CONFIRM_LOBBY)).isFalse();
        assertThat(preferences.privateMessages()).isEqualTo(PrivateMessages.PUBLIC);
        assertThat(preferences.on(Preference.PARTY_INVITES)).isTrue();
        assertThat(preferences.on(Preference.CLAN_INVITES)).isTrue();
        assertThat(preferences.on(Preference.FRIEND_REQUESTS)).isTrue();
        assertThat(preferences.on(Preference.STATS_VISIBLE)).isTrue();
        assertThat(preferences.on(Preference.SITE_DISCORD)).isTrue();
        assertThat(preferences.on(Preference.SITE_FREEZE_STATS)).isTrue();
    }

    @Test
    void aToggleFlipsAndPrivateMessagesCycleThroughTheFourOptions() {
        Preferences preferences = Preferences.DEFAULTS.with(Preference.CHAT, Preferences.DEFAULTS.next(Preference.CHAT));

        assertThat(preferences.on(Preference.CHAT)).isFalse();
        assertThat(preferences.next(Preference.CHAT)).isEqualTo("true");
        assertThat(preferences.next(Preference.PRIVATE_MESSAGES)).isEqualTo("LIMITED");
        assertThat(preferences.with(Preference.PRIVATE_MESSAGES, "LIMITED").next(Preference.PRIVATE_MESSAGES)).isEqualTo("FRIENDS");
        assertThat(preferences.with(Preference.PRIVATE_MESSAGES, "FRIENDS").next(Preference.PRIVATE_MESSAGES)).isEqualTo("OFF");
        assertThat(preferences.with(Preference.PRIVATE_MESSAGES, "OFF").next(Preference.PRIVATE_MESSAGES)).isEqualTo("PUBLIC");
    }

    @Test
    void anUnknownValueFallsBackToTheDefault() {
        Preferences preferences = new Preferences(Map.of(Preference.CHAT, "maybe", Preference.PRIVATE_MESSAGES, "EVERYONE"));

        assertThat(preferences.on(Preference.CHAT)).isTrue();
        assertThat(preferences.privateMessages()).isEqualTo(PrivateMessages.PUBLIC);
        assertThat(Preference.byKey("party-invites")).contains(Preference.PARTY_INVITES);
        assertThat(Preference.byKey("nothing")).isEmpty();
    }
}

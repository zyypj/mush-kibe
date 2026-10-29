package br.com.muxe.core.bukkit.command.framework.message;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.shop.ShopFixtures;
import org.bukkit.configuration.ConfigurationSection;
import org.junit.jupiter.api.Test;

class CommandMessagesTest {

    @Test
    void theCoreConfigurationHasEveryCommandMessage() {
        ConfigurationSection messages = ShopFixtures.CONFIG.getConfigurationSection("messages");

        assertThat(CommandMessages.ALL).allSatisfy(key -> assertThat(messages.isString(key)).as(key).isTrue());
    }
}

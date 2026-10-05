package net.badgersmc.enthusiahub.utility;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TextUtilTest {
    private String plain(String input) {
        return PlainTextComponentSerializer.plainText().serialize(
                LegacyComponentSerializer.legacySection().deserialize(TextUtil.color(input)));
    }

    @Test void parsesLiveTeleportBowMultiStopGradient() {
        String input = "<gradient:#fdcdcd:#dec0fc:#cec0fc:#aec0fc:#94e1ea>[ ᴛᴇʟᴇᴘᴏʀᴛ ʙᴏᴡ ]</gradient>";
        assertEquals("[ ᴛᴇʟᴇᴘᴏʀᴛ ʙᴏᴡ ]", plain(input));
        assertTrue(TextUtil.color(input).contains("\u00a7x"));
        assertFalse(TextUtil.color(input).contains("gradient"));
    }

    @Test void mixesMiniMessageAndLegacySuffix() {
        assertEquals("[ SHOWN ] (Right-Click)", plain("<gradient:#a18cd1:#fbc2eb>[ SHOWN ]</gradient> &7(Right-Click)"));
        assertTrue(TextUtil.color("<red>Hello</red> &7world").contains("\u00a77world"));
        assertEquals("Hello!", plain("&e<gradient:#fdcdcd:#94e1ea>Hello!</gradient>"));
    }

    @Test void retainsLegacyAndIridiumFormatting() {
        assertEquals("\u00a7c\u00a7lHello", TextUtil.color("&c&lHello"));
        assertEquals("Hello", plain("{#ff0000}Hello{/#0000ff}"));
        assertEquals("Hello world", plain("{#ff0000}Hello{/#0000ff} <green>world</green>"));
        assertEquals("Use <menu>", plain("Use <menu>"));
        assertNull(TextUtil.color(null));
    }
}

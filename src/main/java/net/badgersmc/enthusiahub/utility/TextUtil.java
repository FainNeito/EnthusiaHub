package net.badgersmc.enthusiahub.utility;

import net.badgersmc.enthusiahub.utility.color.IridiumColorAPI;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Color;

import java.util.List;

public class TextUtil {

	private static final int CENTER_PX = 154;
	private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
	private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
			.character('\u00a7').hexColors().useUnusualXRepeatedCharacterHexFormat().build();
	private static final String[] LEGACY_TAGS = {
			"black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple",
			"gold", "gray", "dark_gray", "blue", "green", "aqua", "red", "light_purple", "yellow", "white"
	};

	public static String color(final String string) {
		if (string == null) return null;
		if (!string.contains("<")) return IridiumColorAPI.process(string);
		// Iridium's bare hex matcher also matches colors inside MiniMessage arguments.
		// Process only text outside tags so it cannot corrupt gradient parameters.
		java.util.regex.Matcher tags = java.util.regex.Pattern.compile("<[^<>]*>").matcher(string);
		StringBuilder processed = new StringBuilder();
		int end = 0;
		while (tags.find()) {
			processed.append(IridiumColorAPI.process(string.substring(end, tags.start())));
			processed.append(tags.group());
			end = tags.end();
		}
		processed.append(IridiumColorAPI.process(string.substring(end)));
		String legacy = processed.toString();
		return LEGACY.serialize(MINI_MESSAGE.deserialize(legacyToTags(legacy)));
	}

	// Translate legacy controls without escaping MiniMessage tags in mixed configuration text.
	private static String legacyToTags(String input) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < input.length(); i++) {
			if (input.charAt(i) != '\u00a7' || i + 1 >= input.length()) {
				result.append(input.charAt(i));
				continue;
			}
			char code = Character.toLowerCase(input.charAt(i + 1));
			if (code == 'x' && i + 13 < input.length()) {
				StringBuilder hex = new StringBuilder();
				for (int j = 0; j < 6; j++) {
					int offset = i + 2 + j * 2;
					if (input.charAt(offset) != '\u00a7' || Character.digit(input.charAt(offset + 1), 16) < 0) break;
					hex.append(input.charAt(offset + 1));
				}
				if (hex.length() == 6) {
					result.append("<reset><#").append(hex).append('>');
					i += 13;
					continue;
				}
			}
			int color = Character.digit(code, 16);
			String tag = color >= 0 ? "reset><" + LEGACY_TAGS[color] : switch (code) {
				case 'k' -> "obfuscated";
				case 'l' -> "bold";
				case 'm' -> "strikethrough";
				case 'n' -> "underlined";
				case 'o' -> "italic";
				case 'r' -> "reset";
				default -> null;
			};
			if (tag == null) result.append(input.charAt(i));
			else {
				result.append('<').append(tag).append('>');
				i++;
			}
		}
		return result.toString();
	}

	public static String getCenteredMessage(String message) {
		if (message == null || message.isEmpty()) return "";

		message = color(message).replace("<center>", "").replace("</center>", "");

		int messagePxSize = 0;
		boolean previousCode = false;
		boolean isBold = false;

		for (char c : message.toCharArray()) {
			if (c == '�') {
				previousCode = true;

			} else if (previousCode) {
				previousCode = false;
				isBold = c == 'l' || c == 'L';

			} else {
				DefaultFontInfo dFI = DefaultFontInfo.getDefaultFontInfo(c);
				messagePxSize += isBold ? dFI.getBoldLength() : dFI.getLength();
				messagePxSize++;
			}
		}

		int halvedMessageSize = messagePxSize / 2;
		int toCompensate = CENTER_PX - halvedMessageSize;
		int spaceLength = DefaultFontInfo.SPACE.getLength() + 1;
		int compensated = 0;
		StringBuilder sb = new StringBuilder();
		while (compensated < toCompensate) {
			sb.append(" ");
			compensated += spaceLength;
		}

		return sb + message;

	}

	public static String fromList(List<?> list) {
		if (list == null || list.isEmpty()) return null;
		StringBuilder builder = new StringBuilder();

		for (int i = 0; i < list.size(); i++) {
			if (org.bukkit.ChatColor.stripColor(list.get(i).toString()).equals("")) builder.append("\n&r");
			else builder.append(list.get(i).toString()).append(i + 1 != list.size() ? "\n" : "");
		}

		return builder.toString();
	}

	public static String joinString(int index, String[] args) {
		StringBuilder builder = new StringBuilder();
		for (int i = index; i < args.length; i++) {
			builder.append(args[i]).append(" ");
		}
		return builder.toString();
	}

	public static Color getColor(String s) {
		switch (s.toUpperCase()) {
			case "AQUA":
				return Color.AQUA;
			case "BLACK":
				return Color.BLACK;
			case "BLUE":
				return Color.BLUE;
			case "FUCHSIA":
				return Color.FUCHSIA;
			case "GRAY":
				return Color.GRAY;
			case "GREEN":
				return Color.GREEN;
			case "LIME":
				return Color.LIME;
			case "MAROON":
				return Color.MAROON;
			case "NAVY":
				return Color.NAVY;
			case "OLIVE":
				return Color.OLIVE;
			case "ORANGE":
				return Color.ORANGE;
			case "PURPLE":
				return Color.PURPLE;
			case "RED":
				return Color.RED;
			case "SILVER":
				return Color.SILVER;
			case "TEAL":
				return Color.TEAL;
			case "WHITE":
				return Color.WHITE;
			case "YELLOW":
				return Color.YELLOW;
			default:
				return null;
		}
	}

}

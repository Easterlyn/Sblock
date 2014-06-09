package co.sblock.chat;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import co.sblock.chat.channel.CanonNicks;
import co.sblock.chat.channel.Channel;
import co.sblock.chat.channel.ChannelType;
import co.sblock.users.User;
import co.sblock.utilities.rawmessages.EscapedElement;
import co.sblock.utilities.rawmessages.MessageClick;
import co.sblock.utilities.rawmessages.MessageElement;
import co.sblock.utilities.rawmessages.MessageHover;
import co.sblock.utilities.regex.RegexUtils;

/**
 * Used to better clarify a message's destination prior to formatting.
 * 
 * @author Jikoo
 */
public class Message {

	private User sender;
	private String name;
	private Channel channel;
	private String message;
	private boolean escape;

	public Message(User sender, Channel channel, String message) {
		this(channel, message);
		this.sender = sender;
	}

	public Message(String name, Channel channel, String message) {
		this(channel, message);
		this.name = name;
	}

	private Message(Channel channel, String message) {
		this.channel = channel;
		this.message = message;

		escape = message.length() > 0 && message.charAt(0) != '\\';
		if (!escape) {
			message = message.substring(1);
		}
	}

	public User getSender() {
		return sender;
	}

	public Channel getChannel() {
		return channel;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public boolean validate() {
		if (channel == null) {
			return false;
		}

		if (sender == null) {
			if (name != null) {
				return true;
			}
			return false;
		}

		// No sending messages to global chats while ignoring them
		if (channel.getType() == ChannelType.REGION && sender.isSuppressing()) {
			sender.sendMessage(ChatMsgs.errorSuppressingGlobal());
			return false;
		}

		// Must be in target channel to send messages
		if (!channel.getListening().contains(sender.getUUID())) {
			sender.sendMessage(ChatMsgs.errorNotListening(channel.getName()));
			return false;
		}

		// Nicks required in RP channels.
		if (channel.getType() == ChannelType.RP && !channel.hasNick(sender)) {
			sender.sendMessage(ChatMsgs.errorNickRequired(channel.getName()));
			return false;
		}

		return true;
	}

	public void send() {

		// Check if the sender can in fact send the message to the intended target
		if (!validate()) {
			return;
		}

		// Trim whitespace created by formatting codes, etc.
		message = RegexUtils.trimExtraWhitespace(message);
		if (message.length() > 1 && RegexUtils.appearsEmpty(message.substring(0 , 2).equals("#>") ? message.substring(2) : message)) {
			return;
		}

		// Get channel formatting
		message = channel.formatMessage(sender, message);
		if (sender == null) {
			message = message.replaceFirst("<nonhuman>", name);
		}

		// Create raw message and wrap links Minecraft would recognize
		message = wrapLinks(message);

		for (UUID uuid : channel.getListening()) {
			User u = User.getUser(uuid);
			if (u == null) {
				channel.removeListening(uuid);
				continue;
			}
			if (channel.getType() == ChannelType.REGION && u.isSuppressing()) {
				continue;
			}
			if (sender != null && sender.equals(u)) {
				// No self-highlight.
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw " + u.getPlayerName() + " " + message);
			} else {
				u.rawHighlight(message, channel.getNick(u));
			}
		}
	}

	private String wrapLinks(String message) {
		Matcher match = Pattern.compile("(https?://)?(([\\w]+\\.)+([a-zA-Z]{2,4}))(#|/|\\s)(\\S*)?").matcher(message);
		int lastEnd = 0;
		String lastColor = new String();
		MessageElement rawMsg;
		if (channel.getType() == ChannelType.RP) {
			rawMsg = new MessageElement("", CanonNicks.getNick(channel.getNick(sender)).getColor());
		} else {
			rawMsg = new MessageElement("");
		}
		while (match.find()) {
			rawMsg.addExtra(processMessageSegment(lastColor + message.substring(lastEnd, match.start())));
			lastColor = ChatColor.getLastColors(rawMsg.toString());
			String url = match.group();
			// If URL does not start with http:// or https:// the client will crash. Client autofills this for normal links.
			if (!match.group().matches("https?://.*")) {
				url = "http://" + url;
			}
			rawMsg.addExtra(new EscapedElement("[" + match.group(2) + "]", ChatColor.BLUE)
					.addClickEffect(new MessageClick(MessageClick.ClickEffect.OPEN_URL, url))
					.addHoverEffect(new MessageHover(MessageHover.HoverEffect.SHOW_TEXT, url)));
			lastEnd = match.end();
		}
		rawMsg.addExtra(processMessageSegment(lastColor + message.substring(lastEnd)));
		return rawMsg.toString();
	}

	private MessageElement processMessageSegment(String substring) {
		MessageElement msg;
		// Could do this more cleanly with casting, but ifs will work for now.
		if (sender != null && channel.getType() == ChannelType.RP) {
			CanonNicks nick = CanonNicks.getNick(channel.getNick(sender));
			if (escape) {
				msg = new EscapedElement(nick.applyQuirk(substring), nick.getColor());
			} else {
				msg = new MessageElement(nick.applyQuirk(substring), nick.getColor());
			}
		} else if (sender != null && channel.isMod(sender)) {
			if (escape) {
				msg = new EscapedElement(ChatColor.translateAlternateColorCodes('&', substring));
			} else {
				msg = new MessageElement(ChatColor.translateAlternateColorCodes('&', substring));
			}
		} else {
			if (escape) {
				msg = new EscapedElement(substring);
			} else {
				msg = new MessageElement(substring);
			}
		}
		return msg;
	}
}

package co.sblock.discord;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import co.sblock.Sblock;

import me.itsghost.jdiscord.event.EventListener;
import me.itsghost.jdiscord.events.UserChatEvent;

/**
 * EventListener for jDiscord events.
 * 
 * @author Jikoo
 */
public class DiscordListener implements EventListener {

	private final Discord discord;
	private final Cache<String, Boolean> warnings;

	public DiscordListener(Discord discord) {
		this.discord = discord;
		this.warnings = CacheBuilder.newBuilder().weakKeys().weakValues()
				.expireAfterWrite(2, TimeUnit.MINUTES).build();
	}

	public void onUserChat(UserChatEvent event) {
		if (event.getUser() == null) {
			// Additional context for links, etc.
			return;
		}
		if (event.getUser().getUser().getId().equals(discord.getAPI().getSelfInfo().getId())) {
			return;
		}
		Sblock sblock = discord.getPlugin();
		String msg = event.getMsg().getMessage();
		if (msg.startsWith("/link ")) {
			String register = msg.substring(6);
			if (!discord.getAuthCodes().containsValue(register)) {
				discord.postMessage("Sbot", "Invalid registration code!", event.getGroup().getId());
				return;
			}
			discord.postMessage("Sbot", "Registration complete!", event.getGroup().getId());
			UUID link = discord.getAuthCodes().inverse().remove(register);
			sblock.getConfig().set("discord.users." + event.getUser().getUser().getId(), link.toString());
			sblock.saveConfig();
			return;
		}
		boolean main = event.getServer() != null
				&& sblock.getConfig().getString("discord.server").equals(event.getServer().getId())
				&& sblock.getConfig().getString("discord.chat.main").equals(event.getGroup().getId());
		boolean command = msg.length() > 0 && msg.charAt(0) == '/';
		if (!main && !command) {
			return;
		}
		if (command) {
			if (discord.handleDiscordCommandFor(msg, event.getUser(), event.getGroup())) {
				return;
			}
		}
		DiscordPlayer sender = discord.getPlayerFor(event.getUser());
		if (sender == null) {
			if (main) {
				event.getMsg().deleteMessage();
			}
			String id = event.getUser().getUser().getId();
			if (warnings.getIfPresent(id) != null) {
				return;
			}
			warnings.put(id, true);
			event.getGroup().sendMessage("<@" + id
					+ ">, you must run /link in Minecraft to use this feature!");
			return;
		}
		if (command) {
			discord.handleMinecraftCommandFor(sender, msg.substring(1), event.getGroup());
			return;
		}
		if (main) {
			discord.postMessageFor(event, sender);
			return;
		}
	}

}

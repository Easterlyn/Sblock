package co.sblock.chat.channel;

import java.util.UUID;

import co.sblock.chat.ChatMsgs;
import co.sblock.users.OfflineUser;

/**
 * Defines RP channel behavior
 * 
 * @author Dublek
 */
public class RPChannel extends NickChannel {
	
	/**
	 * @see co.sblock.chat.channel.Channel#Channel(String, AccessLevel, UUID)
	 */
	public RPChannel(String name, AccessLevel a, UUID creator, long lastAccessed) {
		super(name, a, creator, lastAccessed);
	}

	/**
	 * @see co.sblock.chat.channel.Channel#setNick(ChatUser, String)
	 */
	@Override
	public void setNick(OfflineUser sender, String nick) {
		CanonNick name = CanonNick.getNick(nick);
		if (name == null) {
			sender.sendMessage(ChatMsgs.errorNickNotCanon(nick));
			return;
		}
		for (String nickname : nickList.values()) {
			if (CanonNick.getNick(nickname).getDisplayName().equals(name.getDisplayName())) {
				sender.sendMessage(ChatMsgs.errorNickTaken(name.getDisplayName()));
				return;
			}
		}
		super.setNick(sender, name.getId());
	}
}

package co.sblock.commands.chat;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import com.google.common.collect.ImmutableList;

import co.sblock.Sblock;
import co.sblock.chat.ChannelManager;
import co.sblock.chat.Chat;
import co.sblock.chat.ChatMsgs;
import co.sblock.chat.Color;
import co.sblock.chat.channel.Channel;
import co.sblock.chat.channel.RegionChannel;
import co.sblock.commands.SblockCommand;
import co.sblock.users.User;
import co.sblock.users.Users;

/**
 * Command for joining a chat channel without focusing on it.
 * 
 * @author Jikoo
 */
public class ChatListenCommand extends SblockCommand {

	private final Users users;
	private final ChannelManager manager;

	public ChatListenCommand(Sblock plugin) {
		super(plugin, "listen");
		this.users = plugin.getModule(Users.class);
		this.manager = plugin.getModule(Chat.class).getChannelManager();
		setDescription("Join a chat channel without focusing on it.");
		setUsage(Color.COMMAND + "/listen <channel>"
				+ Color.GOOD + ": Listen to <channel>.");
	}

	@Override
	protected boolean onCommand(CommandSender sender, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage("Console support not offered at this time.");
			return true;
		}
		if (args.length == 0) {
			return false;
		}
		User user = users.getUser(((Player) sender).getUniqueId());
		Channel channel = manager.getChannel(args[0]);
		if (channel == null) {
			user.sendMessage(ChatMsgs.errorInvalidChannel(args[0]));
			return true;
		}
		if (channel instanceof RegionChannel) {
			user.sendMessage(ChatMsgs.errorRegionChannelJoin());
			return true;
		}
		user.addListening(channel);
		return true;
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String alias, String[] args)
			throws IllegalArgumentException {
		if (!(sender instanceof Player) || args.length > 1) {
			return ImmutableList.of();
		}
		if (args.length ==  1) {
			ArrayList<String> matches = new ArrayList<>();
			for (String channel : manager.getChannelList().keySet()) {
				if (StringUtil.startsWithIgnoreCase(channel, args[0])) {
					matches.add(channel);
				}
			}
			return matches;
		}
		return super.tabComplete(sender, alias, args);
	}
}

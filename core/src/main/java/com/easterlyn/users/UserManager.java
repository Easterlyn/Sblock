package com.easterlyn.users;

import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.MessageKeys;
import com.easterlyn.Easterlyn;
import com.easterlyn.event.UserUnloadEvent;
import com.easterlyn.util.PermissionUtil;
import com.easterlyn.util.PlayerUtil;
import com.easterlyn.util.event.SimpleListener;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Manager for loading users.
 *
 * @author Jikoo
 */
public class UserManager {

	private final LoadingCache<UUID, User> userCache;

	public UserManager(Easterlyn plugin) {
		this.userCache = CacheBuilder.newBuilder()
				.expireAfterAccess(30L, TimeUnit.MINUTES)
				.removalListener(notification -> {
			User user = (User) notification.getValue();
			plugin.getServer().getPluginManager().callEvent(new UserUnloadEvent(user));
			user.save();
			PermissionUtil.releasePermissionData(user.getUniqueId());
		}).build(new CacheLoader<UUID, User>() {
			@Override
			public User load(@NotNull final UUID uuid) {
				return User.load(plugin, uuid);
			}
		});

		PlayerQuitEvent.getHandlerList().register(new SimpleListener<>(PlayerQuitEvent.class,
				playerQuitEvent -> plugin.getServer().getScheduler().runTaskAsynchronously(
						plugin, () -> {
							// Keep permissions loaded if userdata is still loaded
							if (userCache.getIfPresent(playerQuitEvent.getPlayer().getUniqueId()) != null) {
								PermissionUtil.loadPermissionData(playerQuitEvent.getPlayer().getUniqueId());
							}
						}
				), EventPriority.NORMAL, plugin, true));
	}

	public User getUser(UUID uuid) {
		return userCache.getUnchecked(uuid);
	}

	public void registerCommandContext(Easterlyn plugin) {
		plugin.getCommandManager().getCommandContexts().registerIssuerAwareContext(User.class, context -> {
			if (context.hasFlag("self")) {
				if (context.getSender() instanceof Player) {
					return getUser(((Player) context.getSender()).getUniqueId());
				}
				throw new InvalidCommandArgument(MessageKeys.NOT_ALLOWED_ON_CONSOLE);
			}
			String potentialIdentifier = context.getFirstArg();
			try {
				return getUser(UUID.fromString(potentialIdentifier));
			} catch (IllegalArgumentException ignored) {}
			// TODO should other flag ignore self? Requires matcher modification
			Player player;
			try {
				player = PlayerUtil.matchPlayer(context.getSender(), potentialIdentifier, context.hasFlag("offline"), plugin);
			} catch (IllegalAccessException e) {
				context.getSender().sendMessage("Called PlayerUtil#matchPlayer on the main thread while executing! Please /report this message.");
				e.printStackTrace();
				player = PlayerUtil.matchOnlinePlayer(context.getSender(), potentialIdentifier);
			}
			if (player != null) {
				context.popFirstArg();
				return getUser(player.getUniqueId());
			}
			if (context.hasFlag("other") || !(context.getSender() instanceof Player)) {
				throw new InvalidCommandArgument(MessageKeys.COULD_NOT_FIND_PLAYER);
			}
			return getUser(((Player) context.getSender()).getUniqueId());
		});
	}

}

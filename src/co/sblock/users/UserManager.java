package co.sblock.users;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import co.sblock.chat.ColorDef;
import co.sblock.effects.EffectManager;
import co.sblock.users.User.UserSpawner;
import co.sblock.utilities.Broadcast;
import co.sblock.utilities.SpawnLocationInformation;

/**
 * Class that keeps track of players currently logged on to the game
 * 
 * @author FireNG, Jikoo, tmathmeyer
 */
public class UserManager {

	/* The Map of Player UUID and relevant SblockUsers currently online. */
	private static final Map<UUID, User> users = new HashMap<>();

	/**
	 * Adds a new user to the memstore,
	 * though wont override it if there is already one
	 * (in the case of multiple login-logouts)
	 * 
	 * @param player the name of the Player
	 */
	public static User addNewUser(UUID userID) {
		User u = users.get(userID);
		if (u == null) {
			u = new UserSpawner().build(userID);
			users.put(userID, u);
		}
		return u;
	}
	
	/**
	 * @param u the user to add
	 * @return whether there was already a user with this UUID in the map
	 */
	public static boolean addUser(User u) {
		return users.put(u.getUUID(), u) == null;
	}

	/**
	 * Removes a Player from the users list.
	 * 
	 * @param player the name of the Player to remove
	 * @return the SblockUser for the removed player, if any
	 */
	public static User removeUser(UUID userID) {
		return users.remove(userID);
	}

	/**
	 * 
	 * @param userID the UUID of the Player to look up
	 * 
	 * @return the SblockUser associated with the given Player, or null if no
	 *         Player with the given ID is currently online.
	 */
	public static User getUser(UUID userID) {
		if (users.containsKey(userID)) {
			return users.get(userID);
		}
		return null;
	}

	/**
	 * Gets a Collection of SblockUsers currently online.
	 * 
	 * @return the SblockUsers currently online
	 */
	public static Collection<User> getUsers() {
		return users.values();
	}

	/**
	 * Add a Player to their group's Team.
	 * 
	 * @param p the Player
	 */
	public static void team(Player p) {
		String teamPrefix = null;
		for (ChatColor c : ChatColor.values()) {
			if (p.hasPermission("sblockchat." + c.name().toLowerCase())) {
				teamPrefix = c.toString();
				break;
			}
		}
		if (teamPrefix != null) {
			// Do nothing, we've got a fancy override going on
		} else if (p.hasPermission("group.horrorterror")) {
			teamPrefix = ColorDef.RANK_HORRORTERROR.toString();
		} else if (p.hasPermission("group.denizen")) {
			teamPrefix = ColorDef.RANK_DENIZEN.toString();
		} else if (p.hasPermission("group.felt")) {
			teamPrefix = ColorDef.RANK_FELT.toString();
		} else if (p.hasPermission("group.helper")) {
			teamPrefix = ColorDef.RANK_HELPER.toString();
		} else if (p.hasPermission("group.donator")) {
			teamPrefix = ColorDef.RANK_DONATOR.toString();
		} else if (p.hasPermission("group.godtier")) {
			teamPrefix = ColorDef.RANK_GODTIER.toString();
		} else {
			teamPrefix = ColorDef.RANK_HERO.toString();
		}
		Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
		Team team = board.getTeam(p.getName());
		if (team == null) {
			team = board.registerNewTeam(p.getName());
		}
		team.setPrefix(teamPrefix);
		team.addPlayer(p);
	}

	public static void unteam(Player p) {
		Team team = Bukkit.getScoreboardManager().getMainScoreboard().getTeam(p.getName());
		if (team != null) {
			team.unregister();
		}
	}

	/**
	 * 
	 * @param p the player to build the user around
	 */
	public static User doFirstLogin(Player p)
	{
		//player's first login
		Broadcast.lilHal("It would seem that " + p.getName() + " is joining us for the first time! Please welcome them.");
		p.teleport(SpawnLocationInformation.getSpawnLocation());

		User user = new UserSpawner().build(p.getUniqueId());
		user.loginAddListening(new String[]{"#" , "#" + user.getPlayerRegion().name()});
		// TODO: oh god plz
		user.updateCurrentRegion(user.getPlayerRegion());

		addUser(user);
		user.setAllPassiveEffects(EffectManager.passiveScan(p));
		EffectManager.applyPassiveEffects(user);

		user.setLoaded();

		return user;
	}

}

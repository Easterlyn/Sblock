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
import co.sblock.data.SblockData;

/**
 * Class that keeps track of players currently logged on to the game
 * 
 * @author FireNG, Jikoo, tmathmeyer
 */
public class UserManager {

	/* The Map of Player UUID and relevant SblockUsers currently online. */
	private static final Map<UUID, User> users = new HashMap<>();

	/**
	 * Adds a Player to the users list
	 * 
	 * @param player the name of the Player
	 */
	public static User addUser(UUID userID) {
		User u = users.get(userID);
		if (u == null) {
			u = new User(userID);
			users.put(userID, u);
		}
		return u;
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
	 * Gets a SblockUserby Player name.
	 * 
	 * @param name the name of the Player to look up
	 * 
	 * @return the SblockUser associated with the given Player, or null if no
	 *         Player with the given name is currently online.
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
	public static Collection<User> getUserlist() {
		return users.values();
	}

	/**
	 * Add a Player to their group's Team.
	 * 
	 * @param p the Player
	 */
	public static void team(Player p) {
		String teamName = null;
		for (ChatColor c : ChatColor.values()) {
			if (p.hasPermission("sblockchat." + c.name().toLowerCase())) {
				teamName = c.name();
				break;
			}
		}
		if (teamName != null) {
			// Do nothing, we've got a fancy override going on
		} else if (p.hasPermission("group.horrorterror")) {
			teamName = ColorDef.RANK_HORRORTERROR.name();
		} else if (p.hasPermission("group.denizen")) {
			teamName = ColorDef.RANK_DENIZEN.name();
		} else if (p.hasPermission("group.felt")) {
			teamName = ColorDef.RANK_FELT.name();
		} else if (p.hasPermission("group.helper")) {
			teamName = ColorDef.RANK_HELPER.name();
		} else if (p.hasPermission("group.donator")) {
			teamName = ColorDef.RANK_DONATOR.name();
		} else if (p.hasPermission("group.godtier")) {
			teamName = ColorDef.RANK_GODTIER.name();
		} else {
			teamName = ColorDef.RANK_HERO.name();
		}
		Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
		Team team = board.getTeam(teamName);
		if (team == null) {
			team = board.registerNewTeam(teamName);
			team.setPrefix(getTeamPrefix(teamName));
		}
		team.addPlayer(p);
	}

	/**
	 * Fetches team prefixes.
	 */
	private static String getTeamPrefix(String teamName) {
		return ChatColor.valueOf(teamName).toString();
	}
}

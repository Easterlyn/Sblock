package co.sblock.data.redis;

import java.sql.Connection;
import java.util.UUID;

import org.bukkit.command.CommandSender;

import redis.clients.jedis.Jedis;
import co.sblock.chat.channel.Channel;
import co.sblock.data.SblockData;
import co.sblock.machines.type.Machine;
import co.sblock.users.TowerData;
import co.sblock.users.User;
import co.sblock.utilities.Log;

/**
 * Database Implementation for redis
 * 
 * @author tmathmeyer
 */
public class RedisClient extends SblockData{

	private final Log logger = Log.getLog("SblockData - Redis");
	private Jedis connection;
	
	@Override
	public Log getLogger() {
		return logger;
	}

	@Override
	public boolean enable() {
		try {
			connection = new Jedis("localhost");
		} catch (Exception e) {
			return false;
		}
		return connection != null;
	}

	@Override
	public void disable() {
		if (connection != null) {
			connection.shutdown();
		}
	}

	@Override
	protected Connection connection() {
		// TODO this needs to go, but that requires centralising all database code (ie, out of those classes)
		return null;
	}

	@Override
	public void saveUserData(UUID userID) {
		//User user = UserManager.getUserByUUID();
		//connection.hset("Users", userID.toString(), user.getRedisSerialisation());
		//TODO: implement serialisability
	}

	@Override
	public User loadUserData(UUID userID) {
		//String serialisedUser = connection.hget("Users", userID.toString());
		//return User.redisDeserialise(serialisedUser);
		return null;
	}

	@Override
	public void startOfflineLookup(CommandSender sender, String name) {
		// TODO Auto-generated method stub
	}

	@Override
	public void deleteUser(UUID userID) {
		//connection.hdel("Users", UserManager.getUserByID(userID).getUUID());
	}

	@Override
	public void saveChannelData(Channel c) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void loadAllChannelData() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void deleteChannel(String channelName) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void saveMachine(Machine m) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void deleteMachine(Machine m) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void loadAllMachines() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void loadTowerData() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void saveTowerData(TowerData towers) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getUserFromIP(String hostAddress) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getBanReason(String user, String ip) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addBan(User target, String reason) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeBan(String target) {
		// TODO Auto-generated method stub
		
	}
	
}

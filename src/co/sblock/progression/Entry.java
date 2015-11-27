package co.sblock.progression;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import co.sblock.Sblock;
import co.sblock.captcha.Captcha;
import co.sblock.events.packets.ParticleEffectWrapper;
import co.sblock.machines.Machines;
import co.sblock.machines.type.Machine;
import co.sblock.micromodules.Meteorite;
import co.sblock.micromodules.ParticleUtils;
import co.sblock.module.Dependency;
import co.sblock.module.Module;
import co.sblock.users.User;
import co.sblock.users.ProgressionState;
import co.sblock.users.Region;
import co.sblock.users.Users;
import co.sblock.utilities.InventoryUtils;

import net.md_5.bungee.api.ChatColor;

/**
 * Class containing functions controlling the Entry sequence.
 * 
 * @author Jikoo
 */
@Dependency("HolographicDisplays")
public class Entry extends Module {

	private final Material[] materials;
	private final HashMap<UUID, Triple<Meteorite, EntryTimer, Material>> data;

	private Captcha captcha;
	private Machines machines;
	private ParticleUtils particles;
	private Users users;

	public Entry(Sblock plugin) {
		super(plugin);
		this.materials = createMaterialList();
		this.data = new HashMap<>();
	}

	@Override
	protected void onEnable() {
		this.captcha = getPlugin().getModule(Captcha.class);
		this.machines = getPlugin().getModule(Machines.class);
		this.particles = getPlugin().getModule(ParticleUtils.class);
		this.users = getPlugin().getModule(Users.class);
	}

	@Override
	protected void onDisable() {
		for (UUID uuid : data.keySet().toArray(new UUID[data.size()])) {
			User user = users.getUser(uuid);

			// Complete success sans animation if player logs out
			if (user.getProgression() == ProgressionState.ENTRY_COMPLETING) {
				finalizeSuccess(user.getPlayer(), user);
			}

			// Fail Entry if in progress
			if (user.getProgression() == ProgressionState.ENTRY_UNDERWAY) {
				fail(user);
			}
		}
	}

	public boolean canStart(User user) {
		if (!data.containsKey(user.getUUID()) && user.getPrograms().contains("SburbClient")
				&& user.getProgression() == ProgressionState.NONE) {
			return true;
		}
		// User has started or finished Entry already or not installed the SburbClient.
		return false;
	}

	public boolean isEntering(User user) {
		return data.containsKey(user.getUUID());
	}


	public void startEntry(User user, Location cruxtruder) {
		if (!canStart(user)) {
			return;
		}

		user.setProgression(ProgressionState.ENTRY_UNDERWAY);

		// Center hologram inside the space above the block
		final Location holoLoc = cruxtruder.clone().add(new Vector(0.5, 0.4, 0.5));
		// 4:13 = 253 seconds, 2 second display of 0:00
		EntryTimer task = new EntryTimer(this, holoLoc, user.getUUID());
		task.runTaskTimer(getPlugin(), 20L, 20L);
		Meteorite meteorite = new Meteorite(getPlugin(), holoLoc, Material.NETHERRACK, 3, true, -1);
		// 254 seconds * 20 ticks per second = 5080
		meteorite.hoverMeteorite(5080);
		Material material = materials[(int) (materials.length *  Math.random())];
		ItemStack is = new ItemStack(material);
		ItemMeta im = is.getItemMeta();
		im.setDisplayName(ChatColor.AQUA + "Cruxite " + InventoryUtils.getMaterialDataName(is.getType(), is.getDurability()));
		is.setItemMeta(im);
		is = captcha.captchaToPunch(captcha.itemToCaptcha(is));
		if (users.getUser(user.getServer()).isServer()) {
			Bukkit.getPlayer(user.getServer()).getInventory().addItem(is);
		} else {
			Player player = user.getPlayer();
			player.getWorld().dropItem(player.getLocation(), is);
		}
		data.put(user.getUUID(), new ImmutableTriple<>(meteorite, task, material));
	}

	private void finish(User user) {
		if (!isEntering(user)) {
			return;
		}

		// Drop the Meteor created.
		Meteorite meteorite = data.remove(user.getUUID()).getLeft();
		if (!meteorite.hasDropped()) {
			meteorite.dropMeteorite();
		}

		// Kicks the server out of server mode
		users.getUser(user.getServer()).stopServerMode();
	}

	public void fail(UUID uuid) {
		fail(users.getUser(uuid));
	}

	public void fail(User user) {

		if (user == null) {
			return;
		}

		finish(user);

		if (user.getProgression().ordinal() > ProgressionState.ENTRY_UNDERWAY.ordinal()) {
			return;
		}

		// Removes all free machines placed by the User or their server
		for (Pair<Machine, ConfigurationSection> pair : machines.getMachines(user.getUUID())) {
			if (pair.getLeft().isFree()) {
				pair.getLeft().remove(pair.getRight());
			}
		}
	}

	public void succeed(final User user) {
		finish(user);

		user.setProgression(ProgressionState.ENTRY_COMPLETING);

		Player player = user.getPlayer();
		final UUID uuid = player.getUniqueId();

		// Put player on top of the world because we can
		player.teleport(player.getWorld().getHighestBlockAt(player.getLocation()).getLocation().add(new Vector(0, 1, 0)));
		player.getWorld().playEffect(player.getLocation(), Effect.EXPLOSION_HUGE, 0);

		final Firework firework = (Firework) player.getWorld().spawnEntity(player.getLocation(), EntityType.FIREWORK);
		FireworkMeta fm = firework.getFireworkMeta();
		fm.setPower(4);
		firework.setFireworkMeta(fm);
		firework.setPassenger(player);

		particles.addEntity(firework, new ParticleEffectWrapper(Effect.FIREWORKS_SPARK, 5));

		Bukkit.getScheduler().scheduleSyncDelayedTask(getPlugin(), new Runnable() {

			@Override
			public void run() {
				firework.remove();
				Player player = Bukkit.getPlayer(uuid);
				if (player != null) {
					finalizeSuccess(player, user);
				}
			}
		}, 40L);
	}

	public void finalizeSuccess(Player player, User user) {
		user.setProgression(ProgressionState.ENTRY);
		Location target = getEntryLocation(user.getMediumPlanet());
		player.teleport(target);
		player.setBedSpawnLocation(target);
		ItemStack house = new ItemStack(Material.ENDER_CHEST);
		ItemMeta im = house.getItemMeta();
		im.setDisplayName(ChatColor.AQUA + "Prebuilt House");
		ArrayList<String> lore = new ArrayList<>();
		lore.add(ChatColor.YELLOW + "Structure: " + ChatColor.AQUA + ChatColor.ITALIC + "house");
		lore.add(ChatColor.YELLOW + "Place in a free space to build!");
		im.setLore(lore);
		house.setItemMeta(im);
		target.getWorld().dropItem(target, house).setPickupDelay(0);
		for (Entity e : target.getWorld().getEntitiesByClasses(Zombie.class, Skeleton.class, Creeper.class, Slime.class)) {
			if (e.getCustomName() != null) {
				// Named mob
				continue;
			}
			if (((LivingEntity) e).getLocation().distanceSquared(target) < 2048) {
				e.remove();
			}
		}
	}

	private Material[] createMaterialList() {
		return new Material[] { Material.MELON, Material.ARROW, Material.COAL, Material.WATER_LILY,
				Material.INK_SACK, Material.CARROT_STICK, Material.LAVA_BUCKET,
				Material.WATER_BUCKET, Material.APPLE, Material.EGG, Material.SULPHUR,
				Material.SUGAR, Material.QUARTZ, Material.BLAZE_ROD };
	}
	public Material[] getMaterialList() {
		return materials;
	}

	private Location getEntryLocation(Region mPlanet) {
		// TODO should we just use /spreadplayers for this?
		double angle = Math.random() * Math.PI * 2;
		Location l = Bukkit.getWorld(mPlanet.getWorldName())
				.getHighestBlockAt((int) (Math.cos(angle) * 2600), (int) (Math.sin(angle) * 2600))
				.getLocation().add(new Vector(0, 1, 0));
		if (isSafeLocation(l)) {
			return l;
		}
		return getEntryLocation(mPlanet);
	}

	private boolean isSafeLocation(Location l) {
		return !l.getBlock().getType().isSolid()
				&& !l.clone().add(new Vector(0, 1, 0)).getBlock().getType().isSolid()
				&& l.clone().add(new Vector(0, -1, 0)).getBlock().getType().isSolid();
	}
	
	public HashMap<UUID, Triple<Meteorite, EntryTimer, Material>> getData() {
		return data;
	}

	@Override
	public String getName() {
		return "Entry";
	}
}

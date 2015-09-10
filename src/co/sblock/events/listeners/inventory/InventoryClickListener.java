package co.sblock.events.listeners.inventory;

import org.apache.commons.lang3.tuple.Pair;

import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Repairable;
import org.bukkit.scheduler.BukkitRunnable;

import co.sblock.Sblock;
import co.sblock.captcha.Captcha;
import co.sblock.machines.MachineInventoryTracker;
import co.sblock.machines.Machines;
import co.sblock.machines.type.Computer;
import co.sblock.machines.type.Machine;
import co.sblock.machines.type.computer.EmailWriter;
import co.sblock.machines.type.computer.Programs;
import co.sblock.users.OfflineUser;
import co.sblock.users.OnlineUser;
import co.sblock.users.Users;
import co.sblock.utilities.InventoryUtils;

import net.md_5.bungee.api.ChatColor;

/**
 * Listener for InventoryClickEvents.
 * 
 * @author Jikoo
 */
public class InventoryClickListener implements Listener {

	private final ItemStack computer;
	private final EmailWriter mail;

	public InventoryClickListener() {
		this.computer = Machines.getMachineByName("Computer").getUniqueDrop();
		this.mail = (EmailWriter) Programs.getProgramByName("EmailWriter");
	}

	/**
	 * EventHandler for all InventoryClickEvents.
	 * 
	 * @param event the InventoryClickEvent
	 */
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onInventoryClick(InventoryClickEvent event) {
		InventoryHolder ih = event.getView().getTopInventory().getHolder();

		// Finds inventories of physical blocks opened by Machines
		if (ih != null && ih instanceof BlockState) {
			Pair<Machine, ConfigurationSection> pair = Machines.getInstance().getMachineByBlock(((BlockState) ih).getBlock());
			if (pair != null) {
				event.setCancelled(pair.getLeft().handleClick(event, pair.getRight()));
			}
		}

		// Finds inventories forcibly opened by Machines
		Pair<Machine, ConfigurationSection> pair = MachineInventoryTracker.getTracker().getOpenMachine((Player) event.getWhoClicked());
		if (pair != null) {
			event.setCancelled(pair.getLeft().handleClick(event, pair.getRight()));
			return;
		}

		// Lowest priority Machine check, one with no identifying block
		Machine m;
		if (ih != null && ih instanceof Machine) {
			m = (Machine) ih;
			if (m != null) {
				event.setCancelled(m.handleClick(event, null));
				return;
			}
		}

		boolean top = event.getRawSlot() == event.getView().convertSlot(event.getRawSlot());
		switch (event.getClick()) {
		case DOUBLE_CLICK:
			itemGather(event);
			if (event.isCancelled()) {
				break;
			}
			if (top) {
				itemRemoveTop(event);
			} else {
				itemRemoveBottom(event);
			}
			break;
		case NUMBER_KEY:
			if (top) {
				itemShiftTopToBottom(event);
			}
			if (!event.isCancelled()) {
				itemSwapToHotbar(event);
			}
			break;
		case LEFT:
		case RIGHT:
			if (event.getCursor() == null || event.getCursor().getType() == Material.AIR) {
				if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
					return;
				}
				if (top) {
					itemRemoveTop(event);
				} else {
					itemRemoveBottom(event);
				}
			} else if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
				if (top) {
					itemAddTop(event);
				} else {
					itemAddBottom(event);
				}
			} else {
				if (top) {
					itemSwapIntoTop(event);
				} else {
					itemSwapIntoBottom(event);
				}
			}
			break;
		case SHIFT_LEFT:
		case SHIFT_RIGHT:
			if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
				break;
			}
			if (top) {
				itemShiftTopToBottom(event);
			} else {
				itemShiftBottomToTop(event);
			}
			break;
		case CONTROL_DROP:
		case DROP:
		case WINDOW_BORDER_LEFT:
		case WINDOW_BORDER_RIGHT:
			if (top) {
				itemRemoveTop(event);
			} else {
				itemRemoveBottom(event);
			}
			break;
		case CREATIVE:
		case MIDDLE:
		case UNKNOWN:
		default:
			return;
		}
	}

	// doubleclick gather
	private void itemGather(InventoryClickEvent event) {}

	// remove top
	private void itemRemoveTop(InventoryClickEvent event) {}

	// add top
	private void itemAddTop(InventoryClickEvent event) {
		// Cruxite items should not be tradeable.
		if (event.getCursor() != null && event.getCursor().getItemMeta().hasDisplayName()
				&& event.getCursor().getItemMeta().getDisplayName().startsWith(ChatColor.AQUA + "Cruxite ")) {
			event.setCancelled(true);
			return;
		}

		// No putting special Sblock items into anvils, it'll ruin them.
		if (event.getView().getTopInventory().getType() == InventoryType.ANVIL
				&& InventoryUtils.isUniqueItem(event.getCursor())) {
			event.setResult(Result.DENY);
		}
	}

	// move top to bottom
	private void itemShiftTopToBottom(InventoryClickEvent event) {}

	// switch top
	private void itemSwapIntoTop(InventoryClickEvent event) {
		// Cruxite items should not be tradeable.
		if (event.getCursor() != null && event.getCursor().getItemMeta().hasDisplayName()
				&& event.getCursor().getItemMeta().getDisplayName().startsWith(ChatColor.AQUA + "Cruxite ")) {
			event.setCancelled(true);
			return;
		}

		// No putting special Sblock items into anvils, it'll ruin them.
		if (event.getView().getTopInventory().getType() == InventoryType.ANVIL
				&& InventoryUtils.isUniqueItem(event.getCursor())) {
			event.setResult(Result.DENY);
			return;
		}

		// Captcha: attempt to captcha item on cursor
		Captcha.handleCaptcha(event);
	}

	// remove bottom
	private void itemRemoveBottom(InventoryClickEvent event) {

		// Letters cannot be moved. The lore specifically warns against attempting to.
		if (mail.isLetter(event.getCurrentItem())) {
			event.setCurrentItem(null);
			return;
		}

		// Server: Click computer icon -> open computer interface
		OfflineUser user = Users.getGuaranteedUser(event.getWhoClicked().getUniqueId());
		if (user instanceof OnlineUser && ((OnlineUser) user).isServer()) {
			if (computer.isSimilar(event.getCurrentItem())) {
				// Right click air: Open computer
				event.setCancelled(true);
				((Computer) Machines.getMachineByName("Computer")).openInventory((Player) event.getWhoClicked());
			}
			return;
		}
	}

	// add bottom
	private void itemAddBottom(InventoryClickEvent event) {}

	// move bottom to top
	private void itemShiftBottomToTop(InventoryClickEvent event) {

		// Letters cannot be moved. The lore specifically warns against attempting to.
		if (mail.isLetter(event.getCurrentItem())) {
			event.setCurrentItem(null);
			return;
		}

		// Cruxite items should not be tradeable.
		if (event.getCurrentItem() != null && event.getCurrentItem().getItemMeta().hasDisplayName()
				&& event.getCurrentItem().getItemMeta().getDisplayName().startsWith(ChatColor.AQUA + "Cruxite ")) {
			event.setCancelled(true);
			return;
		}

		// No putting special Sblock items into anvils, it'll ruin them.
		if (event.getView().getTopInventory().getType() == InventoryType.ANVIL
				&& InventoryUtils.isUniqueItem(event.getCurrentItem())) {
			event.setResult(Result.DENY);
			return;
		}
	}

	// switch bottom
	private void itemSwapIntoBottom(InventoryClickEvent event) {

		// Letters cannot be moved. The lore specifically warns against attempting to.
		if (mail.isLetter(event.getCurrentItem())) {
			event.setCurrentItem(null);
			// This is now an attempt to add an item to the bottom inventory rather than a swap.
			itemAddBottom(event);
			return;
		}

		// Server: No picking up computer icon
		OfflineUser user = Users.getGuaranteedUser(event.getWhoClicked().getUniqueId());
		if (user instanceof OnlineUser && ((OnlineUser) user).isServer()
				&& computer.isSimilar(event.getCurrentItem())) {
			event.setCancelled(true);
			return;
		}

		// Captcha: attempt to captcha item on cursor
		Captcha.handleCaptcha(event);
	}

	// hotbar with inv
	private void itemSwapToHotbar(InventoryClickEvent event) {
		ItemStack hotbar = event.getView().getBottomInventory().getItem(event.getHotbarButton());

		// Letters cannot be moved. The lore specifically warns against attempting to.
		if (mail.isLetter(event.getCurrentItem())) {
			event.setCurrentItem(null);
			return;
		}
		if (mail.isLetter(hotbar)) {
			event.getView().getBottomInventory().setItem(event.getHotbarButton(), null);
			return;
		}

		OfflineUser user = Users.getGuaranteedUser(event.getWhoClicked().getUniqueId());
		if (user instanceof OnlineUser && ((OnlineUser) user).isServer()
				&& (computer.isSimilar(event.getCurrentItem()) || computer.isSimilar(hotbar))) {
			event.setCancelled(true);
			return;
		}

		// No putting special Sblock items into anvils, it'll ruin them.
		if (event.getView().getTopInventory().getType() == InventoryType.ANVIL
				&& (InventoryUtils.isUniqueItem(event.getCursor())
						|| InventoryUtils.isUniqueItem(hotbar))) {
			event.setResult(Result.DENY);
			return;
		}

		// Captcha: attempt to captcha item in clicked slot
		Captcha.handleCaptcha(event);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onInventoryClickLowestPriority(InventoryClickEvent event) {
		ItemStack clicked = event.getCurrentItem();
		if (event.getWhoClicked().hasPermission("sblock.felt")) {
			return;
		}
		if (clicked != null && clicked.getAmount() < 0) {
			event.setCurrentItem(null);
		}
	}

	/**
	 * EventHandler for inventory clicks that are guaranteed to have occurred.
	 * 
	 * @param event
	 */
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onInventoryClickHasOccurred(InventoryClickEvent event) {
		final InventoryView view = event.getView();

		if (view.getTopInventory().getType() != InventoryType.ANVIL
				|| !((Player) event.getWhoClicked()).hasPermission("sblock.blaze")) {
			return;
		}

		new BukkitRunnable() {
			@Override
			public void run() {
				createBlazingSaddle(view);
			}
		}.runTask(Sblock.getInstance());
	}

	private void createBlazingSaddle(InventoryView view) {
		Inventory top = view.getTopInventory();
		ItemStack firstSlot = top.getItem(0);
		ItemStack secondSlot = top.getItem(1);

		if (firstSlot == null || secondSlot == null || firstSlot.getType() != Material.SADDLE
				|| firstSlot.containsEnchantment(Enchantment.ARROW_FIRE)
				|| secondSlot.getType() != Material.ENCHANTED_BOOK || !secondSlot.hasItemMeta()) {
			return;
		}

		EnchantmentStorageMeta esm = (EnchantmentStorageMeta) secondSlot.getItemMeta();
		int fire = esm.getStoredEnchantLevel(Enchantment.ARROW_FIRE);

		if (fire < 1) {
			return;
		}

		ItemStack blazingSaddle = new ItemStack(firstSlot);
		ItemMeta saddleMeta = blazingSaddle.getItemMeta();

		saddleMeta.addEnchant(Enchantment.ARROW_FIRE, 1, true);

		Repairable repairable = (Repairable) saddleMeta;
		int cost = repairable.hasRepairCost() ? repairable.getRepairCost() : 0;

		// Next cost is always current * 2 + 1
		((Repairable) saddleMeta).setRepairCost(cost * 2 + 1);

		// Flame from a book costs 2
		cost += 2;

		String displayName = InventoryUtils.getNameFromAnvil(view);
		if (saddleMeta.hasDisplayName() && !saddleMeta.getDisplayName().equals(displayName)
				|| !saddleMeta.hasDisplayName() && displayName != null) {
			saddleMeta.setDisplayName(displayName);
			// Renaming adds 1
			cost += 1;
		}

		blazingSaddle.setItemMeta(saddleMeta);

		top.setItem(2, blazingSaddle);
		InventoryUtils.setAnvilExpCost(view, cost);
		if (view.getPlayer() instanceof Player) {
			((Player) view.getPlayer()).updateInventory();
		}
		InventoryUtils.updateAnvilExpCost(view);
	}

}

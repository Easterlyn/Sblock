package com.easterlyn.util.inventory;

import com.easterlyn.util.GenericUtil;
import com.easterlyn.util.NumberUtil;
import com.easterlyn.util.StringUtil;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.CookingRecipe;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.FireworkEffectMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A set of useful methods for inventory functions.
 *
 * @author Jikoo
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class ItemUtil {

	public static final ItemStack AIR = new ItemStack(Material.AIR);
	public static final String UNIQUE_KEYED_PREFIX = "easterlyn_unique_";
	private static final Set<Function<ItemStack, Boolean>> UNIQUE_CHECKS = new HashSet<>();

	static {
		addUniqueCheck(itemStack -> {
			if (!itemStack.hasItemMeta()) {
				return false;
			}
			ItemMeta itemMeta = itemStack.getItemMeta();
			if (itemMeta == null || !itemMeta.hasLore()) {
				return false;
			}
			List<String> lore = itemMeta.getLore();
			return lore != null && lore.contains(ChatColor.DARK_PURPLE + "Unique");
		});
	}

	private static BiMap<String, String> items;

	private static BiMap<String, String> getItems() {
		if (items != null) {
			return items;
		}
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(
				Objects.requireNonNull(Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("EasterlynCore")).getResource("items.csv"))))) {
			items = HashBiMap.create();
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.isEmpty()) {
					continue;
				}
				String[] row = line.split(",");
				items.put(row[0], row[1]);
			}
		} catch (IOException e) {
			throw new RuntimeException("Could not load items from items.csv!", e);
		}
		return items;
	}

	public static String getItemName(ItemStack item) {
		Material material = item.getType();
		String name = getItems().get(material.name());
		if (name == null) {
			// Even special-cased materials should have an entry.
			name = StringUtil.getFriendlyName(material);
		}
		if (material == Material.POTION || material == Material.SPLASH_POTION || material == Material.LINGERING_POTION
				|| material == Material.TIPPED_ARROW) {
			if (!item.hasItemMeta()) {
				return name;
			}
			ItemMeta meta = item.getItemMeta();
			if (meta instanceof PotionMeta) {
				return StringUtil.getFriendlyName(material) + " of " + getPotionName((PotionMeta) meta);
			}
			return name;
		}
		return name;
	}

	private static String getPotionName(PotionMeta meta) {
		PotionData base;
		try {
			base = meta.getBasePotionData();
		} catch (IllegalArgumentException e) {
			// This can be thrown by Spigot when converting a valid potion with odd data values.
			return "Questionable Validity";
		}
		if (base.getType() != PotionType.UNCRAFTABLE) {
			StringBuilder name = new StringBuilder();
			if (base.isExtended()) {
				name.append("Extended ");
			}
			name.append(StringUtil.getFriendlyName(base.getType()));
			if (base.isUpgraded()) {
				name.append(" II");
			}
			return name.toString();
		}
		if (!meta.hasCustomEffects()) {
			return "No Effect";
		}
		if (meta.getCustomEffects().size() > 1) {
			return "Multiple Effects";
		}
		PotionEffect effect = meta.getCustomEffects().get(0);
		PotionEffectType type = effect.getType();
		boolean extended = !type.isInstant() && effect.getDuration() > 3600;
		StringBuilder name = new StringBuilder();
		if (extended) {
			name.append("Extended ");
		}
		name.append(StringUtil.getFriendlyName(type.getName()));
		if (effect.getAmplifier() > 0) {
			// Effect power is 0-indexed
			name.append(' ').append(NumberUtil.romanFromInt(effect.getAmplifier() + 1));
		}
		return name.toString();
	}

	public static boolean isMisleadinglyNamed(String name, Material material) {
		String materialName = getItems().inverse().get(name);
		return materialName != null && !materialName.equals(material.name());
	}

	public static Material matchMaterial(String search) {
		String searchMaterialName = search.toUpperCase().replace(' ', '_');

		try {
			return Material.valueOf(searchMaterialName);
		} catch (IllegalArgumentException ignored) {}

		String searchFriendlyName = search.replace('_', ' ');

		// TODO ignoreCase
		String materialName = getItems().inverse().get(searchFriendlyName);
		if (materialName != null) {
			return Material.valueOf(materialName);
		}

		Material material = null;

		float matchLevel = 0F;
		search = searchFriendlyName.toLowerCase(Locale.ENGLISH);
		for (Entry<String, String> entry : getItems().entrySet()) {
			float current = StringUtil.compare(search, entry.getValue().toLowerCase(Locale.ENGLISH));
			if (current > matchLevel) {
				matchLevel = current;
				material = Material.getMaterial(entry.getKey());
			}
			if (current == 1F) {
				return material;
			}
		}

		// Allow more fuzziness for longer named items
		if (material != null && matchLevel > (.7F - (1F / material.name().length()))) {
			return material;
		}
		return null;
	}

	public static ItemStack cleanNBT(ItemStack originalItem) {
		if (originalItem == null || !originalItem.hasItemMeta()) {
			return originalItem;
		}

		ItemMeta originalMeta = originalItem.getItemMeta();
		if (originalMeta == null) {
			// Unnecessary, but it keeps the compiler happy.
			return originalItem;
		}

		ItemStack cleanedItem = new ItemStack(originalItem.getType());
		cleanedItem.setAmount(originalItem.getAmount());

		ItemMeta cleanedMeta = cleanedItem.getItemMeta();
		if (cleanedMeta == null) {
			return cleanedItem;
		}

		// Banners
		GenericUtil.biConsumeAs(BannerMeta.class, originalMeta, cleanedMeta,
				(oldMeta, newMeta) -> newMeta.setPatterns(oldMeta.getPatterns()));

		// Book and quill/Written books
		GenericUtil.biConsumeAs(BookMeta.class, originalMeta, cleanedMeta, (oldMeta, newMeta) -> {
			if (oldMeta.hasPages()) {
				newMeta.setPages(oldMeta.getPages());
			}
			if (oldMeta.hasAuthor()) {
				newMeta.setAuthor(oldMeta.getAuthor());
			}
			if (oldMeta.hasTitle()) {
				newMeta.setTitle(oldMeta.getTitle());
			}
		});

		// Durability
		GenericUtil.biConsumeAs(Damageable.class, originalMeta, cleanedMeta, (oldMeta, newMeta) ->
				newMeta.setDamage(Math.max(Math.min(oldMeta.getDamage(), originalItem.getType().getMaxDurability()), 0)));

		// Single effect fireworks
		GenericUtil.biConsumeAs(FireworkEffectMeta.class, originalMeta, cleanedMeta,
				(oldMeta, newMeta) -> newMeta.setEffect(oldMeta.getEffect()));

		// Fireworks/Firework stars
		GenericUtil.biConsumeAs(FireworkMeta.class, originalMeta, cleanedMeta, (oldMeta, newMeta) -> {
			newMeta.setPower(oldMeta.getPower());
			newMeta.addEffects(oldMeta.getEffects());
		});

		// Leather armor color
		GenericUtil.biConsumeAs(LeatherArmorMeta.class, originalMeta, cleanedMeta,
				(oldMeta, newMeta) -> newMeta.setColor(oldMeta.getColor()));

		// Enchanted books
		GenericUtil.biConsumeAs(EnchantmentStorageMeta.class, originalMeta, cleanedMeta,
				(oldMeta, newMeta) -> oldMeta.getStoredEnchants().forEach(
						(enchantment, level) -> newMeta.addStoredEnchant(enchantment, level, true)));

		// Maps
		GenericUtil.biConsumeAs(MapMeta.class, originalMeta, cleanedMeta, (oldMeta, newMeta) -> {
				newMeta.setMapView(oldMeta.getMapView());
				newMeta.setColor(oldMeta.getColor());
				newMeta.setLocationName(oldMeta.getLocationName());
				newMeta.setScaling(oldMeta.isScaling());
		});

		// Potions
		GenericUtil.biConsumeAs(PotionMeta.class, originalMeta, cleanedMeta, (oldMeta, newMeta) -> {
				newMeta.setBasePotionData(oldMeta.getBasePotionData());
				newMeta.setColor(oldMeta.getColor());
				oldMeta.getCustomEffects().forEach(effect -> {
					// Custom effects are fine, but amplifiers that are way too high are not
					if (effect.getAmplifier() < 5 && effect.getAmplifier() >= 0) {
						newMeta.addCustomEffect(effect, true);
					}
				});
		});

		// Repairable would preserve anvil tags on tools, we'll avoid that

		// Skulls
		GenericUtil.biConsumeAs(SkullMeta.class, originalMeta, cleanedMeta,
				(oldMeta, newMeta) -> newMeta.setPlayerProfile(oldMeta.getPlayerProfile()));

		// Normal meta
		if (originalMeta.hasDisplayName()) {
			cleanedMeta.setDisplayName(originalMeta.getDisplayName());
		}

		if (originalMeta.hasEnchants()) {
			for (Entry<Enchantment, Integer> entry : originalMeta.getEnchants().entrySet()) {
				cleanedMeta.addEnchant(entry.getKey(), entry.getValue(), true);
			}
		}

		if (originalMeta.hasLore()) {
			cleanedMeta.setLore(originalMeta.getLore());
		}

		cleanedItem.setItemMeta(cleanedMeta);
		return cleanedItem;
	}

	public static boolean isUniqueItem(ItemStack toCheck) {
		if (toCheck == null) {
			return false;
		}

		for (Function<ItemStack, Boolean> function : UNIQUE_CHECKS) {
			if (function.apply(toCheck)) {
				return true;
			}
		}

		return false;
	}

	public static void addUniqueCheck(Function<ItemStack, Boolean> function) {
		UNIQUE_CHECKS.add(function);
	}

	public static int getAddFailures(Map<Integer, ItemStack> failures) {
		int count = 0;
		for (ItemStack is : failures.values()) {
			count += is.getAmount();
		}
		return count;
	}

	/**
	 * Reduces an ItemStack by the given quantity. If the ItemStack would have a
	 * quantity of 0, returns air.
	 *
	 * @param itemStack the ItemStack to reduce
	 * @param amount the amount to reduce the ItemStack by
	 *
	 * @return the reduced ItemStack
	 */
	@NotNull
	public static ItemStack decrement(@Nullable ItemStack itemStack, int amount) {
		if (itemStack == null || itemStack.getType() == Material.AIR) {
			return AIR;
		}
		if (itemStack.getAmount() > amount) {
			itemStack.setAmount(itemStack.getAmount() - amount);
		} else {
			itemStack = AIR;
		}
		return itemStack;
	}

	public static void decrementHeldItem(@NotNull PlayerInteractEvent event, int amount) {
		boolean main = isMainHand(event);
		PlayerInventory inv = event.getPlayer().getInventory();
		setHeldItem(inv, main, decrement(getHeldItem(inv, main), amount));
	}

	public static boolean isMainHand(@NotNull PlayerInteractEvent event) {
		return event.getHand() == EquipmentSlot.HAND;
	}

	@NotNull
	public static ItemStack getHeldItem(@NotNull PlayerInteractEvent event) {
		return getHeldItem(event.getPlayer().getInventory(), isMainHand(event));
	}

	@NotNull
	private static ItemStack getHeldItem(@NotNull PlayerInventory inv, boolean mainHand) {
		return mainHand ? inv.getItemInMainHand() : inv.getItemInOffHand();
	}

	public static void setHeldItem(@NotNull PlayerInventory inv, boolean mainHand, @Nullable ItemStack item) {
		if (mainHand) {
			inv.setItemInMainHand(item);
		} else {
			inv.setItemInOffHand(item);
		}
	}

	/**
	 * Checks if there is space in the given Inventory to add the given ItemStack.
	 *
	 * @param is the ItemStack
	 * @param inv the Inventory to check
	 *
	 * @return true if the ItemStack can be fully added
	 */
	public static boolean hasSpaceFor(@Nullable ItemStack is, @NotNull Inventory inv) {
		if (is == null || is.getType() == Material.AIR) {
			return true;
		}
		ItemStack toAdd = is.clone();
		for (ItemStack invStack : inv.getContents()) {
			//noinspection ConstantConditions
			if (invStack == null) {
				return true;
			}
			if (!invStack.isSimilar(toAdd)) {
				continue;
			}
			toAdd.setAmount(toAdd.getAmount() - toAdd.getMaxStackSize() + invStack.getAmount());
			if (toAdd.getAmount() <= 0) {
				return true;
			}
		}
		return false;
	}

	@NotNull
	public static String recipeToText(@NotNull Recipe recipe) {
		if (recipe instanceof CookingRecipe) {
			String type = recipe.getClass().getName().toUpperCase().replace("RECIPE", "");
			return String.format("%s: %s -> %s", type, itemToText(((FurnaceRecipe) recipe).getInput()), itemToText(recipe.getResult()));
		} else if (recipe instanceof ShapelessRecipe) {
			StringBuilder builder = new StringBuilder("SHAPELESS: ");
			for (ItemStack ingredient : ((ShapelessRecipe) recipe).getIngredientList()) {
				builder.append(itemToText(ingredient)).append(" + ");
			}
			builder.replace(builder.length() - 2, builder.length(), "-> ").append(recipe.getResult());
			return builder.toString();
		} else if (recipe instanceof ShapedRecipe) {
			StringBuilder builder = new StringBuilder("SHAPED:\n");
			Map<Character, String> mappings = new HashMap<>();
			int longestMapping = 3; // "AIR".length() == 3
			for (Entry<Character, ItemStack> mapping : ((ShapedRecipe) recipe).getIngredientMap().entrySet()) {
				String newMapping = itemToText(mapping.getValue());
				longestMapping = Math.max(longestMapping, newMapping.length());
				mappings.put(mapping.getKey(), newMapping);
			}
			for (String line : ((ShapedRecipe) recipe).getShape()) {
				for (char character : line.toCharArray()) {
					builder.append('[');
					String mapping = mappings.getOrDefault(character, "AIR");
					double padding = (longestMapping - mapping.length()) / 2.0;
					double roundPadding = Math.floor(padding);
					for (int i = 0; i < roundPadding; i++) {
						builder.append(' ');
					}
					builder.append(mapping);
					// Post-pad additional space for odd numbers
					padding = Math.floor(padding + 0.5);
					for (int i = 0; i < padding; i++) {
						builder.append(' ');
					}
					builder.append("] ");
				}
				builder.delete(builder.length() - 1, builder.length());
				builder.append('\n');
			}
			builder.delete(builder.length() - 1, builder.length()).append(" -> ").append(itemToText(recipe.getResult()));
			return builder.toString();
		}
		return recipe.toString();
	}

	@NotNull
	private static String itemToText(@Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR) {
			return "AIR";
		}
		StringBuilder builder = new StringBuilder();
		builder.append(item.getType().name());
		if (item.getAmount() != 1) {
			builder.append('x').append(item.getAmount());
		}
		return builder.toString();
	}

}

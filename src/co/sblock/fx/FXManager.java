package co.sblock.fx;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.reflections.Reflections;

import co.sblock.Sblock;
import co.sblock.users.OnlineUser;
import co.sblock.utilities.captcha.Captcha;

public class FXManager {

	private static Map<String, Class<? extends SblockFX>> validEffects; 

	public FXManager() {
		validEffects = new HashMap<String, Class<? extends SblockFX>>();

		Reflections reflections = new Reflections("co.sblock.fx");
		Set<Class<? extends SblockFX>> effects = reflections.getSubTypesOf(SblockFX.class);
		for (Class<? extends SblockFX> effect : effects) {
			try {
				SblockFX instance = effect.newInstance();
				validEffects.put(instance.getCanonicalName(), effect);
			} catch (IllegalAccessException | IllegalArgumentException | SecurityException
					| InstantiationException e) {
				Sblock.getLog().severe("Keiko you fuck fix your FXManager");
				e.printStackTrace();
			}
		}
	}

	public static Map<String, Class<? extends SblockFX>> getValidEffects() {
		return validEffects;
	}

	/**
	 * Scans the entire inventory of a User for effects
	 * 
	 * @param u the User to scan
	 */
	public static void fullEffectsScan(OnlineUser u) {
		Player p = u.getPlayer();
		PlayerInventory pInv = p.getInventory();
		ItemStack[] iS = pInv.getContents();
		ItemStack[] iSA = pInv.getArmorContents();
		ArrayList<String> playerLore = new ArrayList<String>();
		HashMap<String, SblockFX> output = new HashMap<String, SblockFX>();

		u.removeAllEffects();

		for (ItemStack i : iS) { // Inventory
			if (i != null) {
				// System.out.println(i.getType().toString());
				if (i.hasItemMeta() && i.getItemMeta().hasLore() && !(Captcha.isCard(i))) {
					playerLore.addAll(i.getItemMeta().getLore());
				}
			}
		}
		for (ItemStack i : iSA) { // Armor
			// System.out.println(i.getType().toString());
			if (i != null && i.hasItemMeta() && i.getItemMeta().hasLore() && !(Captcha.isCard(i))) {
				playerLore.addAll(i.getItemMeta().getLore());
			}
		}
		for (String s : playerLore) { //Removes all invalid lore
			for(Class<? extends SblockFX> valid : validEffects.values()) {
				try {
					SblockFX newEffect = valid.newInstance();
					if (newEffect.isValidName(s)) {
						if(output.containsKey(newEffect.getCanonicalName())) {
							output.get(newEffect.getCanonicalName()).setMultiplier(
									output.get(newEffect.getCanonicalName()).getMultiplier() + 1);
						}
						else {
							output.put(newEffect.getCanonicalName(), newEffect);
						}
					}
				} catch (InstantiationException | IllegalAccessException e) {
					Sblock.getLog().severe("Keiko you fuck fix your effectsScan");
					e.printStackTrace();
				}
			}
		}
		u.setAllEffects(output);
	}
	/**
	 * Scans a specific ItemStack for all valid Effects
	 * 
	 * @param iS the ItemStack to scan
	 */
	public static HashMap<String, SblockFX> itemScan(ItemStack iS) {
		ArrayList<String> playerLore = new ArrayList<String>();
		HashMap<String, SblockFX> output = new HashMap<String, SblockFX>();

		if (iS != null) {
			if (iS.hasItemMeta() && iS.getItemMeta().hasLore() && !(Captcha.isCard(iS))) {
				playerLore.addAll(iS.getItemMeta().getLore());
			}
		}
		for (String s : playerLore) {
			for(Class<? extends SblockFX> valid : validEffects.values()) {
				try {
					SblockFX newEffect = valid.newInstance();
					if (newEffect.isValidName(s)) {
						if(output.containsKey(newEffect.getCanonicalName())) {
							output.get(newEffect.getCanonicalName()).setMultiplier(
									output.get(newEffect.getCanonicalName()).getMultiplier() + 1);
						}
						else {
							output.put(newEffect.getCanonicalName(), newEffect);
						}
					}
				} catch (InstantiationException | IllegalAccessException e) {
					Sblock.getLog().severe("Keiko you fuck fix your effectsScan");
					e.printStackTrace();
				}
			}
		}
		return output;
	}	
}

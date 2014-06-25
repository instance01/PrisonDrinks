package com.comze_instancelabs.prisondrinks;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Random;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Main extends JavaPlugin implements Listener {

	// maybe add villager (bartender)

	Economy econ;

	public LinkedHashMap<String, Long> drinks = new LinkedHashMap<String, Long>();

	DecimalFormat formatter = new DecimalFormat("#,###");

	public void onEnable() {
		Bukkit.getPluginManager().registerEvents(this, this);
		this.setupEconomy();

		drinks.put("Bud Light", 1L);
		drinks.put("Yeungling Premium Lager", 5L);
		drinks.put("Heineken", 50L);
		drinks.put("Miller Light", 250L);
		drinks.put("Keystone Light", 500L);
		drinks.put("Michelob Ultra Light", 1000L);
		drinks.put("Corona Light", 5000L);
		drinks.put("Coors Light", 10000L);
		drinks.put("Rainbow Milk", 100000L); // more reward here

	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equalsIgnoreCase("drinks")) {
			if (sender instanceof Player) {
				Player p = (Player) sender;
				this.openShop(p);
			}
			return true;
		}
		return false;
	}

	private boolean setupEconomy() {
		if (getServer().getPluginManager().getPlugin("Vault") == null) {
			return false;
		}
		RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
		if (rsp == null) {
			return false;
		}
		econ = rsp.getProvider();
		return econ != null;
	}

	@EventHandler
	public void onDrink(PlayerItemConsumeEvent event) {
		if (event.getItem().getType() != Material.POTION) {
			return;
		}
		ItemMeta itemm = event.getItem().getItemMeta();
		String name = itemm.getDisplayName();
		name = stripColorCodes(name);
		Random r = new Random();

		String playername = event.getPlayer().getName();

		if (name.startsWith("Rainbow")) {
			if (r.nextInt(9) > 5) { // 6 7 8 9
				econ.depositPlayer(playername, 1000000000000L);
				event.getPlayer().sendMessage(ChatColor.GREEN + "You won 1.000.000.000!");
			} else {
				econ.withdrawPlayer(playername, 100000 * 1000000);
				event.getPlayer().sendMessage(ChatColor.RED + "You lost.");
				event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20, 1));
				event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 1));
				event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 20, 1));
			}
			return;
		}
		if (r.nextInt(9) > 5) { // 6 7 8 9
			// win
			event.getPlayer().sendMessage(ChatColor.GREEN + "You won " + formatter.format(Long.toString(getPrice(name) * 2)) + "!");
			econ.depositPlayer(playername, getPrice(name) * 2);
		} else {
			// lose
			econ.withdrawPlayer(playername, getPrice(name));
			event.getPlayer().sendMessage(ChatColor.RED + "You lost.");
			event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20, 1));
			event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 1));
			event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 20, 1));
		}

		event.getPlayer().getInventory().remove(new ItemStack(Material.POTION, 1));
		event.getPlayer().updateInventory();
	}

	public static String stripColorCodes(String str) {
		if (str.startsWith("§d§lR")) {
			return "Rainbow Milk";
		}
		if (str.length() > 2) {
			return str.substring(2);
		}
		return "Potion";
	}

	public long getPrice(String drink) {
		if (drinks.containsKey(drink)) {
			return drinks.get(drink) * 1000000L;
		}
		return 0;
	}

	public void openShop(Player p) {
		IconMenu iconm = new IconMenu("§4§lMatrix §a§lLiquor §4§lBar", 9, new IconMenu.OptionClickEventHandler() {
			@Override
			public void onOptionClick(IconMenu.OptionClickEvent event) {
				String d = stripColorCodes(event.getName());
				Player p = event.getPlayer();
				double currentcredits = econ.getBalance(p.getName());
				if (currentcredits >= getPrice(d)) {
					ItemStack pot = new ItemStack(Material.POTION, 1);
					ItemMeta m = pot.getItemMeta();
					Random r = new Random();
					String name = "§" + Integer.toString((r.nextInt(8) + 1)) + d;
					if (d.startsWith("Rainbow")) {
						name = ChatColor.translateAlternateColorCodes('&', "&d&lR&b&la&a&li&e&ln&d&lb&a&lo&e&lw &d&lM&a&li&e&ll&d&lk");
						pot = new ItemStack(Material.POTION, 1, (short) 8193);
					}
					m.setDisplayName(name);
					m.setLore(new ArrayList<String>(Arrays.asList(formatter.format(getPrice(d)))));
					pot.setItemMeta(m);
					p.getInventory().addItem(pot);
					p.updateInventory();
					p.sendMessage(ChatColor.GREEN + "You bought a " + d + "!");

					// remove money
					econ.withdrawPlayer(p.getName(), getPrice(d));
				} else {
					p.sendMessage(ChatColor.RED + "You don't have enough money!");
				}
				event.setWillClose(true);
			}
		}, this);

		int count = 0;
		for (String drink : drinks.keySet()) {
			Random r = new Random();
			String name = "§" + Integer.toString((r.nextInt(8) + 1)) + drink;
			ItemStack pot = new ItemStack(Material.POTION, 1);
			if (drink.startsWith("Rainbow")) {
				name = ChatColor.translateAlternateColorCodes('&', "&d&lR&b&la&a&li&e&ln&d&lb&a&lo&e&lw &d&lM&a&li&e&ll&d&lk");
				pot = new ItemStack(Material.POTION, 1, (short) 8193);
			}

			iconm.setOption(count, pot, name, false, "Cost: " + formatter.format(this.getPrice(drink)));
			count++;
		}

		iconm.open(p);
	}
}

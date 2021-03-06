/*
 *   Copyright 2016, 2017, 2020 Moros <https://github.com/PrimordialMoros>
 *
 * 	  This file is part of Hyperion.
 *
 *    Hyperion is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    Hyperion is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with Hyperion.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.moros.hyperion.listeners;

import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.event.AbilityStartEvent;
import com.projectkorra.projectkorra.event.BendingReloadEvent;
import me.moros.hyperion.Hyperion;
import me.moros.hyperion.abilities.earthbending.EarthGuard;
import me.moros.hyperion.abilities.earthbending.EarthShot;
import me.moros.hyperion.abilities.earthbending.MetalCable;
import me.moros.hyperion.abilities.firebending.Bolt;
import me.moros.hyperion.abilities.firebending.Bolt.BoltInfo;
import me.moros.hyperion.configuration.ConfigManager;
import me.moros.hyperion.methods.CoreMethods;
import me.moros.hyperion.util.BendingFallingBlock;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.PlayerInventory;

public class CoreListener implements Listener {

	@EventHandler(priority = EventPriority.HIGHEST)
	public void EntityChangeBlockEvent(final EntityChangeBlockEvent event) {
		if (event.getEntityType().equals(EntityType.FALLING_BLOCK)) {
			final FallingBlock fb = (FallingBlock) event.getEntity();
			if (BendingFallingBlock.isBendingFallingBlock(fb)) {
				final BendingFallingBlock bfb = BendingFallingBlock.get(fb);
				final CoreAbility ability = bfb.getAbility();
				if (ability instanceof EarthShot) {
					((EarthShot) ability).checkBlast(true);
				} else if (ability instanceof EarthGuard) {
					ability.remove();
				}
				bfb.remove();
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onArrowHit(final ProjectileHitEvent event) {
		if (event.getEntity() instanceof Arrow && event.getEntity().hasMetadata(CoreMethods.CABLE_KEY)) {
			if (event.getHitBlock() != null) {
				final MetalCable cable = (MetalCable) event.getEntity().getMetadata(CoreMethods.CABLE_KEY).get(0).value();
				if (cable != null) cable.setHitBlock(event.getHitBlock());
			} else if (event.getHitEntity() != null) {
				final MetalCable cable = (MetalCable) event.getEntity().getMetadata(CoreMethods.CABLE_KEY).get(0).value();
				if (cable != null) cable.setHitEntity(event.getHitEntity());
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onLightningStrike(final EntitySpawnEvent event) {
		if (event.getEntity() instanceof LightningStrike && event.getEntity().hasMetadata(CoreMethods.BOLT_KEY)) {
			final BoltInfo boltInfo = (BoltInfo) event.getEntity().getMetadata(CoreMethods.BOLT_KEY).get(0).value();
			if (boltInfo != null) {
				if (!Bolt.isNearbyChannel(boltInfo.getLocation(), boltInfo.getAbility().getPlayer()))
					Bolt.dealDamage(boltInfo);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onEntityDamageByEntity(final EntityDamageByEntityEvent event) {
		if (event.getDamager() instanceof Arrow && event.getDamager().hasMetadata(CoreMethods.CABLE_KEY)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onInteractAtEntity(final PlayerInteractAtEntityEvent event) {
		if (event.getRightClicked().hasMetadata(CoreMethods.NO_INTERACTION_KEY)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onItemPickup(final EntityPickupItemEvent event) {
		if (event.getItem().hasMetadata(CoreMethods.NO_PICKUP_KEY)) {
			event.setCancelled(true);
			event.getItem().remove();
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onHopperItemPickup(final InventoryPickupItemEvent event) {
		if (event.getItem().hasMetadata(CoreMethods.NO_PICKUP_KEY)) {
			event.setCancelled(true);
			event.getItem().remove();
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onInventoryClick(final InventoryClickEvent event) {
		if (event.isCancelled() || !(event.getClickedInventory() instanceof PlayerInventory) || event.getSlotType() != InventoryType.SlotType.ARMOR) {
			return;
		}
		final PlayerInventory inventory = (PlayerInventory) event.getClickedInventory();
		if (inventory.getHolder() instanceof Player) {
			final Player player = ((Player) inventory.getHolder()).getPlayer();
			if (CoreAbility.hasAbility(player, EarthGuard.class)) {
				final EarthGuard guard = CoreAbility.getAbility(player, EarthGuard.class);
				if (guard.hasActiveArmor()) event.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void onItemMerge(final ItemMergeEvent event) {
		if (event.getEntity().hasMetadata(CoreMethods.GLOVE_KEY) || event.getTarget().hasMetadata(CoreMethods.GLOVE_KEY)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerDeath(final PlayerDeathEvent event) {
		if (CoreAbility.hasAbility(event.getEntity(), EarthGuard.class)) {
			final EarthGuard guard = CoreAbility.getAbility(event.getEntity(), EarthGuard.class);
			if (guard.hasActiveArmor()) {
				event.getDrops().removeIf(item -> guard.getArmor(false).contains(item));
				event.getDrops().addAll(guard.getArmor(true));
				guard.remove();
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerLogout(final PlayerQuitEvent event) {
		if (CoreAbility.hasAbility(event.getPlayer(), EarthGuard.class)) {
			final EarthGuard guard = CoreAbility.getAbility(event.getPlayer(), EarthGuard.class);
			if (guard.hasActiveArmor()) guard.remove();
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPKReload(final BendingReloadEvent event) {
		Bukkit.getScheduler().runTaskLater(Hyperion.getPlugin(), Hyperion::reload, 1);
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onAbilityStart(final AbilityStartEvent event) {
		if (event.getAbility() instanceof CoreAbility) {
			CoreAbility ability = (CoreAbility) event.getAbility();
			final Player player = ability.getPlayer();
			final BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
			if (player == null || bPlayer == null) return;
			if (bPlayer.isAvatarState()) {
				ConfigurationSection section = ConfigManager.modifiersConfig.getConfig().getConfigurationSection("AvatarState." + ability.getName());
				if (section != null) {
					CoreMethods.setAttributes(section, ability);
				}
			}
		}
	}
}

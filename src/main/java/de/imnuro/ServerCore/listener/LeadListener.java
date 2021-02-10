package de.imnuro.ServerCore.listener;

import de.imnuro.ServerCore.Main;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import java.util.ArrayList;
import java.util.HashMap;

public class LeadListener implements Listener {
    private final HashMap<Player, ArrayList<LivingEntity>> leashPlayers = new HashMap<Player, ArrayList<LivingEntity>>();

    @EventHandler
    public void cancelLeash(PlayerLeashEntityEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void getMob(PlayerInteractEntityEvent event) {
        if(event.getRightClicked() instanceof LivingEntity && !(event.getRightClicked() instanceof Player)) {
            if (event.getHand() == EquipmentSlot.HAND && event.getPlayer().getInventory().getItemInMainHand().getType().equals(Material.LEAD) || event.getHand() == EquipmentSlot.HAND && ((LivingEntity) event.getRightClicked()).isLeashed()) {
                Player player = event.getPlayer();
                event.setCancelled(true);
                LivingEntity mob = (LivingEntity) event.getRightClicked();
                if (leashPlayers.containsKey(event.getPlayer())) {
                    ArrayList<LivingEntity> entities = leashPlayers.get(player);
                    if (entities.contains(mob)) {
                        player.getInventory().addItem(new ItemStack(Material.LEAD));
                        entities.remove(mob);
                        mob.setLeashHolder(null);
                    } else if (entities.size() >= 3) {
                        player.sendMessage("§7Du darfst maximal §63 Tiere §7an deiner Leine haben!");
                    } else if (!entities.contains(mob)) {
                        entities.add(mob);
                        removeItem(player, Material.LEAD, 1);
                        player.updateInventory();
                        Bukkit.getScheduler().runTaskLater(Main.getInstance(), (Runnable) () -> mob.setLeashHolder(player), 1);
                    }
                } else {
                    ArrayList<LivingEntity> entities = new ArrayList<LivingEntity>();
                    entities.add(mob);
                    removeItem(player, Material.LEAD, 1);
                    player.updateInventory();
                    Bukkit.getScheduler().runTaskLater(Main.getInstance(), (Runnable) () -> mob.setLeashHolder(player), 1);
                    leashPlayers.put(player, entities);
                }
            }
        }
    }

    @EventHandler
    public void onClick(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.HAND && (event.getClickedBlock() != null) && (event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            if (event.getClickedBlock().getType() == Material.ACACIA_FENCE ||
                    event.getClickedBlock().getType() == Material.OAK_FENCE ||
                    event.getClickedBlock().getType() == Material.DARK_OAK_FENCE ||
                    event.getClickedBlock().getType() == Material.BIRCH_FENCE ||
                    event.getClickedBlock().getType() == Material.NETHER_BRICK_FENCE ||
                    event.getClickedBlock().getType() == Material.SPRUCE_FENCE ||
                    event.getClickedBlock().getType() == Material.JUNGLE_FENCE) {
                if(leashPlayers.get(event.getPlayer()) != null) {
                    LeashHitch hitch = (LeashHitch) event.getClickedBlock().getWorld().spawnEntity(event.getClickedBlock().getLocation(), EntityType.LEASH_HITCH);
                    leashPlayers.get(event.getPlayer()).forEach(mob -> mob.setLeashHolder(hitch));
                    leashPlayers.remove(event.getPlayer());
                }
            }
        }
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        leashPlayers.remove(event.getPlayer());
    }

    @EventHandler
    public void changeWorld(PlayerChangedWorldEvent event) {
        if(leashPlayers.containsKey(event.getPlayer()) && leashPlayers.get(event.getPlayer()).size() > 0)
            leashPlayers.get(event.getPlayer()).forEach(players -> {
                leashPlayers.get(event.getPlayer()).forEach(animal -> animal.teleport(event.getPlayer()));
            });
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if(event.getEntity().isLeashed())
            leashPlayers.remove(event.getEntity().getLeashHolder(), event.getEntity());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event)  {
        leashPlayers.remove(event.getEntity());
    }

    private static void removeItem(Player p, Material mat, int removeAmount) {
        for (int i = 0; i < p.getInventory().getContents().length; i++) {
            ItemStack item = p.getInventory().getItem(i);
            if (item != null && item.getType() == mat) {
                int itemAmount = item.getAmount();
                if (p.getItemInHand().getAmount() <= removeAmount) {
                    p.getInventory().setItem(p.getInventory().getHeldItemSlot(), new ItemStack(Material.AIR));
                } else {
                    item.setAmount(itemAmount - removeAmount);
                }
            }
        }
    }
}




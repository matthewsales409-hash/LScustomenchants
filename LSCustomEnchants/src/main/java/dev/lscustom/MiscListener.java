package dev.lscustom;

import io.papermc.paper.event.player.PlayerArmorChangeEvent;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Iterator;

/** Bows/crossbows, rods, deaths, hunger, durability, anvil, and misc armor effects. */
public final class MiscListener implements Listener {

    private static void fx(LivingEntity e, PotionEffectType t, int amp, int ticks) {
        e.addPotionEffect(new PotionEffect(t, ticks, Math.max(0, amp), false, true, true));
    }

    // ------------------------ Ranged ------------------------
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onShoot(EntityShootBowEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        ItemStack bow = e.getBow();
        if (bow == null || !(e.getProjectile() instanceof AbstractArrow arrow)) return;
        int l;

        if ((l = Items.level(bow, CE.ARTEMIS)) > 0) {
            arrow.getPersistentDataContainer().set(Items.artemisKey(), PersistentDataType.INTEGER, l);
        }
        if ((l = Items.level(bow, CE.HOMING)) > 0) homing(arrow, p, l);

        if ((l = Items.level(bow, CE.VOLLEY)) > 0) {
            Vector base = arrow.getVelocity();
            for (int i = 1; i <= l * 2; i++) {
                double ang = Math.toRadians(6.0 * ((i + 1) / 2) * (i % 2 == 0 ? 1 : -1));
                Arrow extra = p.getWorld().spawn(arrow.getLocation(), Arrow.class);
                extra.setShooter(p);
                extra.setVelocity(base.clone().rotateAroundY(ang));
                extra.setCritical(arrow.isCritical());
                extra.setDamage(arrow.getDamage());
                extra.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
            }
        }

        // Autoloading Holster: reload the crossbow from inventory arrows right after firing
        if (Items.level(bow, CE.AUTOLOADING_HOLSTER) > 0) {
            EquipmentSlot hand = e.getHand();
            new BukkitRunnable() {
                @Override public void run() {
                    if (!p.isOnline() || hand == null) return;
                    ItemStack cb = p.getInventory().getItem(hand);
                    if (cb == null || cb.getType() != Material.CROSSBOW) return;
                    if (!p.getInventory().contains(Material.ARROW)) return;
                    CrossbowMeta m = (CrossbowMeta) cb.getItemMeta();
                    if (m.hasChargedProjectiles()) return;
                    m.addChargedProjectile(new ItemStack(Material.ARROW));
                    cb.setItemMeta(m);
                    p.getInventory().removeItem(new ItemStack(Material.ARROW, 1));
                }
            }.runTaskLater(CustomEnchants.get(), 3L);
        }
    }

    private void homing(AbstractArrow arrow, Player shooter, int lvl) {
        new BukkitRunnable() {
            int ticks = 0;
            @Override public void run() {
                if (!arrow.isValid() || arrow.isInBlock() || arrow.isOnGround() || ticks++ > 60) { cancel(); return; }
                LivingEntity best = null;
                double bd = 100;
                for (Entity n : arrow.getNearbyEntities(8, 8, 8)) {
                    if (!(n instanceof LivingEntity le) || n == shooter || n instanceof ArmorStand) continue;
                    if (n instanceof Tameable t && shooter.equals(t.getOwner())) continue;
                    double d = n.getLocation().distanceSquared(arrow.getLocation());
                    if (d < bd) { bd = d; best = le; }
                }
                if (best == null) return;
                Vector v = arrow.getVelocity();
                double speed = v.length();
                if (speed < 0.1) return;
                Vector dir = best.getEyeLocation().toVector().subtract(arrow.getLocation().toVector()).normalize();
                double steer = 0.08 * lvl;
                arrow.setVelocity(v.normalize().multiply(1 - steer).add(dir.multiply(steer)).normalize().multiply(speed));
            }
        }.runTaskTimer(CustomEnchants.get(), 2L, 1L);
    }

    // ------------------------ Hook ------------------------
    @EventHandler(ignoreCancelled = true)
    public void onFish(PlayerFishEvent e) {
        if (e.getState() != PlayerFishEvent.State.CAUGHT_ENTITY || !(e.getCaught() instanceof LivingEntity target)) return;
        Player p = e.getPlayer();
        int l = Items.hand(p, CE.HOOK);
        if (l <= 0) l = Items.level(p.getInventory().getItemInOffHand(), CE.HOOK);
        if (l <= 0) return;
        Vector pull = p.getLocation().toVector().subtract(target.getLocation().toVector());
        if (pull.lengthSquared() == 0) return;
        target.setVelocity(pull.normalize().multiply(0.8 + 0.2 * l).setY(0.35));
    }

    // ------------------------ Deaths ------------------------
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player dead = e.getEntity();

        // Soulbound: keep these items through death
        Iterator<ItemStack> it = e.getDrops().iterator();
        while (it.hasNext()) {
            ItemStack s = it.next();
            if (Items.level(s, CE.SOULBOUND) > 0) {
                e.getItemsToKeep().add(s);
                it.remove();
            }
        }

        // Headless: chance to drop the victim's head
        Player killer = dead.getKiller();
        if (killer != null) {
            int l = Items.hand(killer, CE.HEADLESS);
            if (l > 0 && Items.roll(20 * l)) {
                ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta sm = (SkullMeta) head.getItemMeta();
                sm.setOwningPlayer(dead);
                head.setItemMeta(sm);
                e.getDrops().add(head);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onKill(EntityDeathEvent e) {
        Player k = e.getEntity().getKiller();
        if (k == null) return;
        int l;
        if ((l = Items.hand(k, CE.FRENZY)) > 0) {
            fx(k, PotionEffectType.SPEED, l - 1, 100);
            fx(k, PotionEffectType.STRENGTH, 0, 100);
        }
        if ((l = Items.armor(k, CE.RALLY)) > 0) fx(k, PotionEffectType.ABSORPTION, l - 1, 200);
    }

    // ------------------------ Armor misc ------------------------
    @EventHandler(ignoreCancelled = true)
    public void onHunger(FoodLevelChangeEvent e) {
        if (!(e.getEntity() instanceof Player p) || e.getFoodLevel() >= p.getFoodLevel()) return;
        int l = Items.armor(p, CE.ENDURANCE);
        if (l > 0 && Items.roll(15 * l)) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onCombust(EntityCombustEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        int l = Items.armor(p, CE.EXTINGUISH);
        if (l > 0 && Items.roll(25 * l)) e.setCancelled(true);
    }

    @EventHandler
    public void onArmorChange(PlayerArmorChangeEvent e) {
        // remove Firefly's night vision as soon as the helmet comes off
        if (e.getSlotType() != PlayerArmorChangeEvent.SlotType.HEAD) return;
        if (Items.level(e.getOldItem(), CE.FIREFLY) > 0 && Items.level(e.getNewItem(), CE.FIREFLY) <= 0) {
            e.getPlayer().removePotionEffect(PotionEffectType.NIGHT_VISION);
        }
    }

    // ------------------------ Reinforce ------------------------
    @EventHandler(ignoreCancelled = true)
    public void onItemDamage(PlayerItemDamageEvent e) {
        int l = Items.level(e.getItem(), CE.REINFORCE);
        if (l > 0 && Items.roll(15 * l)) e.setCancelled(true);
    }

    // ------------------------ Anvil: apply books ------------------------
    @EventHandler
    public void onAnvil(PrepareAnvilEvent e) {
        Inventory inv = e.getInventory();
        ItemStack a = inv.getItem(0), b = inv.getItem(1);
        if (a == null || b == null) return;
        CE ce = Items.bookEnchant(b);
        if (ce == null || CustomEnchants.DISABLED.contains(ce)) return;
        if (a.getType() == Material.ENCHANTED_BOOK || !ce.kind().matches(a.getType())) return;

        int bl = Items.bookLevel(b);
        int cur = Items.level(a, ce);
        if (cur >= ce.max() && bl <= cur) { e.setResult(null); return; }
        int out = cur == bl ? Math.min(ce.max(), bl + 1) : Math.min(ce.max(), Math.max(cur, bl));

        ItemStack result = a.clone();
        Items.set(result, ce, out);
        e.setResult(result);
        e.getView().setRepairCost(Math.max(1, out * 3));
    }
}

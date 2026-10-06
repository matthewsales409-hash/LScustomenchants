package dev.lscustom;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CombatListener implements Listener {
    /** Guards against our own damage calls re-triggering enchants (infinite loops). */
    static boolean busy = false;

    private final Map<UUID, Integer> streak = new HashMap<>();
    private final Map<UUID, long[]> counter = new HashMap<>();      // {expiry, level}
    private final Map<UUID, UUID> comboTarget = new HashMap<>();
    private final Map<UUID, Integer> comboCount = new HashMap<>();
    private final Map<UUID, Long> comboTime = new HashMap<>();
    private final Map<String, Long> lastHit = new HashMap<>();
    private final Map<UUID, Long> heavenlyCd = new HashMap<>();

    private static void hit(LivingEntity target, double dmg, LivingEntity source) {
        busy = true;
        try { target.damage(dmg, source); } finally { busy = false; }
    }

    private static void fx(LivingEntity e, PotionEffectType t, int amp, int ticks) {
        e.addPotionEffect(new PotionEffect(t, ticks, Math.max(0, amp), false, true, true));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        streak.remove(id); counter.remove(id); comboTarget.remove(id);
        comboCount.remove(id); comboTime.remove(id); heavenlyCd.remove(id);
    }

    // ------------------------------------------------------------------
    //  Entity vs entity damage
    // ------------------------------------------------------------------
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent e) {
        if (busy) return;
        Entity damager = e.getDamager();

        if (e.getEntity() instanceof Player victim) {
            LivingEntity att = null;
            if (damager instanceof LivingEntity le) att = le;
            else if (damager instanceof Projectile pr && pr.getShooter() instanceof LivingEntity sh) att = sh;
            defend(victim, att);
        }

        if (damager instanceof Player p && e.getEntity() instanceof LivingEntity v && e.getCause() == DamageCause.ENTITY_ATTACK) {
            melee(e, p, v);
        } else if (damager instanceof Projectile pr && pr.getShooter() instanceof Player) {
            Integer art = pr.getPersistentDataContainer().get(Items.artemisKey(), org.bukkit.persistence.PersistentDataType.INTEGER);
            if (art != null && art > 0) e.setDamage(e.getDamage() * (1 + 0.1 * art));
        }
    }

    /** Armor enchants that react to being hit by something. */
    private void defend(Player v, LivingEntity att) {
        streak.remove(v.getUniqueId());
        int l;

        if ((l = Items.armor(v, CE.COUNTER)) > 0 && Items.roll(12 * l)) {
            counter.put(v.getUniqueId(), new long[]{System.currentTimeMillis() + 4000, l});
        }
        if ((l = Items.armor(v, CE.HARMONIC)) > 0 && Items.roll(8 * l)) fx(v, PotionEffectType.REGENERATION, 0, 100);
        if ((l = Items.armor(v, CE.UNSTABLE)) > 0 && Items.roll(10 * l)) {
            PotionEffectType[] pool = {PotionEffectType.SPEED, PotionEffectType.STRENGTH, PotionEffectType.RESISTANCE,
                PotionEffectType.REGENERATION, PotionEffectType.SLOWNESS, PotionEffectType.WEAKNESS, PotionEffectType.NAUSEA};
            fx(v, pool[(int) (Math.random() * pool.length)], 0, 100);
        }

        if (att == null || att == v) return;

        if ((l = Items.armor(v, CE.CLAPBACK)) > 0 && Items.roll(6 * l)) hit(att, 2 + l, v);

        if ((l = Items.armor(v, CE.BOUNCEBACK)) > 0 && Items.roll(10 * l)) {
            Vector d = att.getLocation().toVector().subtract(v.getLocation().toVector()).setY(0);
            if (d.lengthSquared() > 0) att.setVelocity(d.normalize().multiply(0.8 + 0.2 * l).setY(0.35));
        }
        if ((l = Items.armor(v, CE.DRUNKEN)) > 0 && Items.roll(10 * l)) {
            fx(att, PotionEffectType.NAUSEA, 0, 100);
            fx(att, PotionEffectType.SLOWNESS, 0, 40);
        }
        if ((l = Items.armor(v, CE.TRICKSTER)) > 0 && Items.roll(5 * l)) {
            var al = att.getLocation();
            var dest = al.clone().add(al.getDirection().setY(0).normalize().multiply(-1.5));
            Block b = dest.getBlock();
            if (b.isPassable() && b.getRelative(0, 1, 0).isPassable()) {
                dest.setDirection(al.getDirection());
                v.teleport(dest);
            }
        }
    }

    /** Weapon enchants (melee). */
    private void melee(EntityDamageByEntityEvent e, Player p, LivingEntity v) {
        ItemStack w = p.getInventory().getItemInMainHand();
        if (w.getType().isAir()) return;
        UUID id = p.getUniqueId();
        long now = System.currentTimeMillis();
        double mult = 1.0;
        int l;

        if ((l = Items.level(w, CE.BERSERK)) > 0 && p.getHealth() / Items.maxHp(p) < 0.5) mult += 0.10 * l;
        if ((l = Items.level(w, CE.CRITICAL)) > 0 && !p.isOnGround() && p.getFallDistance() > 0) mult += 0.10 * l;
        if ((l = Items.level(w, CE.LOW_GROUND)) > 0 && v.getY() > p.getY() + 0.5) mult += 0.08 * l;
        if ((l = Items.level(w, CE.UNDERDOG)) > 0 && p.getHealth() < v.getHealth()) mult += 0.06 * l;
        if ((l = Items.level(w, CE.SLAYER)) > 0 && v instanceof Monster) mult += 0.10 * l;

        if ((l = Items.level(w, CE.FIRST_STRIKE)) > 0) {
            String key = id + ":" + v.getUniqueId();
            Long last = lastHit.get(key);
            if (last == null || now - last > 10_000) mult += 0.15 * l;
            if (lastHit.size() > 2000) lastHit.clear();
            lastHit.put(key, now);
        }
        if ((l = Items.level(w, CE.COMPOUND)) > 0) {
            if (v.getUniqueId().equals(comboTarget.get(id)) && now - comboTime.getOrDefault(id, 0L) < 3000) {
                comboCount.merge(id, 1, Integer::sum);
            } else comboCount.put(id, 0);
            comboTarget.put(id, v.getUniqueId());
            comboTime.put(id, now);
            mult += 0.03 * l * Math.min(5, comboCount.get(id));
        }
        if ((l = Items.level(w, CE.UNBROKEN_CHAIN)) > 0) {
            int s = streak.getOrDefault(id, 0);
            mult += 0.03 * l * Math.min(6, s);
            streak.put(id, s + 1);
        }
        long[] c = counter.get(id);
        if (c != null) {
            if (c[0] > now) { mult += 0.12 * c[1]; }
            counter.remove(id);
        }

        e.setDamage(e.getDamage() * mult);
        double base = e.getDamage();

        // ---- on-hit procs ----
        if ((l = Items.level(w, CE.LIFESTEAL)) > 0) {
            p.setHealth(Math.min(Items.maxHp(p), p.getHealth() + e.getFinalDamage() * 0.05 * l));
        }
        if ((l = Items.level(w, CE.BOLT)) > 0 && Items.roll(5 * l)) {
            v.getWorld().strikeLightningEffect(v.getLocation());
            hit(v, 3, p);
        }
        if ((l = Items.level(w, CE.SHOCK)) > 0 && Items.roll(8 * l)) {
            fx(v, PotionEffectType.BLINDNESS, 0, 40);
            hit(v, 1 + l, p);
        }
        if ((l = Items.level(w, CE.CONCUSS)) > 0 && Items.roll(8 * l)) {
            fx(v, PotionEffectType.NAUSEA, 0, 60 + 20 * l);
            fx(v, PotionEffectType.SLOWNESS, 0, 40);
        }
        if ((l = Items.level(w, CE.DELUSIONAL_STRENGTH)) > 0 && Items.roll(10 * l)) {
            fx(p, PotionEffectType.STRENGTH, l - 1, 100);
            fx(p, PotionEffectType.NAUSEA, 0, 60);
        }
        if ((l = Items.level(w, CE.FLING)) > 0 && Items.roll(8 * l)) {
            v.setVelocity(v.getVelocity().add(new Vector(0, 0.6 + 0.15 * l, 0)));
        }
        if ((l = Items.level(w, CE.FROST)) > 0 && Items.roll(10 * l)) {
            fx(v, PotionEffectType.SLOWNESS, 1, 60 + 20 * l);
            v.setFreezeTicks(Math.max(v.getFreezeTicks(), 140));
        }
        if ((l = Items.level(w, CE.REAPER)) > 0 && Items.roll(8 * l)) {
            fx(v, PotionEffectType.WITHER, 0, 60 + 40 * l);
        }
        if ((l = Items.level(w, CE.SLASH)) > 0 && Items.roll(10 * l)) bleed(v, 3 + l);

        if ((l = Items.level(w, CE.CLEAVING)) > 0) {
            double dmg = base * (0.25 + 0.1 * l);
            for (Entity n : v.getNearbyEntities(2, 1, 2)) {
                if (!(n instanceof LivingEntity le) || n == p || n instanceof ArmorStand) continue;
                if (n instanceof Tameable t && p.equals(t.getOwner())) continue;
                hit(le, dmg, p);
            }
        }
    }

    private void bleed(LivingEntity v, int times) {
        new BukkitRunnable() {
            int n = 0;
            @Override public void run() {
                if (v.isDead() || !v.isValid() || n++ >= times) { cancel(); return; }
                v.damage(1.0);
            }
        }.runTaskTimer(CustomEnchants.get(), 20L, 20L);
    }

    // ------------------------------------------------------------------
    //  Any damage to a player (armor reductions, fall/explosion/fire, Heavenly)
    // ------------------------------------------------------------------
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAnyDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player v)) return;
        DamageCause c = e.getCause();
        int l;

        if (c == DamageCause.FALL && Items.armor(v, CE.WEIGHTLESS) > 0) { e.setCancelled(true); return; }

        if ((c == DamageCause.BLOCK_EXPLOSION || c == DamageCause.ENTITY_EXPLOSION)
            && (l = Items.armor(v, CE.DEMOLITIONIST)) > 0) {
            e.setDamage(e.getDamage() * (1 - 0.10 * l));
        }
        if (c == DamageCause.FALLING_BLOCK && (l = Items.armor(v, CE.HARDHAT)) > 0) {
            e.setDamage(e.getDamage() * (1 - 0.25 * l));
        }
        if (c == DamageCause.FIRE_TICK && (l = Items.armor(v, CE.EXTINGUISH)) > 0 && Items.roll(15 * l)) {
            v.setFireTicks(0);
            e.setCancelled(true);
            return;
        }
        if (e instanceof EntityDamageByEntityEvent d && d.getDamager() instanceof Projectile
            && (l = Items.armor(v, CE.DEFLECTOR)) > 0) {
            if (Items.roll(4 * l)) { e.setCancelled(true); return; }
            e.setDamage(e.getDamage() * (1 - 0.08 * l));
        }
        if ((l = Items.armor(v, CE.BULWARK)) > 0) e.setDamage(e.getDamage() * (1 - 0.03 * l));

        // Heavenly: survive a lethal hit (cooldown)
        if ((l = Items.armor(v, CE.HEAVENLY)) > 0 && e.getFinalDamage() >= v.getHealth()) {
            long now = System.currentTimeMillis();
            Long cd = heavenlyCd.get(v.getUniqueId());
            if (cd == null || now > cd) {
                e.setCancelled(true);
                v.setHealth(Math.min(Items.maxHp(v), 6 + 2 * l));
                fx(v, PotionEffectType.ABSORPTION, 1, 100);
                v.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, v.getLocation().add(0, 1, 0), 40, 0.4, 0.6, 0.4, 0.2);
                v.sendMessage(Component.text("Heavenly saved you!", NamedTextColor.GOLD));
                heavenlyCd.put(v.getUniqueId(), now + 300_000L - 60_000L * (l - 1));
            }
        }
    }
}

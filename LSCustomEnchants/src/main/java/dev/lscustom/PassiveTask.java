package dev.lscustom;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/** Runs every 2 seconds: worn-armor and held-tool passive effects. */
public final class PassiveTask extends BukkitRunnable {
    private int n = 0;

    static void fx(Player p, PotionEffectType t, int amp, int ticks) {
        p.addPotionEffect(new PotionEffect(t, ticks, Math.max(0, amp), true, false, true));
    }

    @Override
    public void run() {
        n++;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.isDead()) continue;
            int l;

            if ((l = Items.armor(p, CE.ATHLETIC)) > 0) fx(p, PotionEffectType.SPEED, l - 1, 60);
            if ((l = Items.armor(p, CE.ASCENT)) > 0) fx(p, PotionEffectType.JUMP_BOOST, l - 1, 60);
            if (Items.armor(p, CE.FIREFLY) > 0) fx(p, PotionEffectType.NIGHT_VISION, 0, 260);

            // Adrenaline: speed burst while under 40% health
            if ((l = Items.armor(p, CE.ADRENALINE)) > 0 && p.getHealth() / Items.maxHp(p) < 0.4) {
                fx(p, PotionEffectType.SPEED, l - 1, 80);
            }

            // Lifebloom: heal over time (every 4s)
            if ((l = Items.armor(p, CE.LIFEBLOOM)) > 0 && n % 2 == 0 && p.getHealth() < Items.maxHp(p)) {
                p.setHealth(Math.min(Items.maxHp(p), p.getHealth() + l));
            }

            // Sustenance: slow hunger refill (every 8s)
            if ((l = Items.armor(p, CE.SUSTENANCE)) > 0 && n % 4 == 0 && p.getFoodLevel() < 20) {
                p.setFoodLevel(Math.min(20, p.getFoodLevel() + l));
            }

            // Sand Paper: haste while holding the shovel
            if ((l = Items.hand(p, CE.SAND_PAPER)) > 0) fx(p, PotionEffectType.HASTE, l - 1, 60);
        }
    }
}

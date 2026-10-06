package dev.lscustom;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.function.Predicate;

/** Vein Miner, Timber, Autosmelt, Quarry, Vortex. */
public final class ToolListener implements Listener {
    private final Set<UUID> breaking = new HashSet<>();

    private static boolean isOre(Material m) {
        return m.name().endsWith("_ORE") || m == Material.ANCIENT_DEBRIS;
    }

    private static List<Block> flood(Block start, Predicate<Block> ok, int limit) {
        List<Block> out = new ArrayList<>();
        Set<Block> seen = new HashSet<>();
        ArrayDeque<Block> q = new ArrayDeque<>();
        q.add(start);
        seen.add(start);
        while (!q.isEmpty() && out.size() < limit) {
            Block c = q.poll();
            out.add(c);
            for (int dx = -1; dx <= 1; dx++)
                for (int dy = -1; dy <= 1; dy++)
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        Block n = c.getRelative(dx, dy, dz);
                        if (seen.add(n) && ok.test(n)) q.add(n);
                    }
        }
        return out;
    }

    private void breakAll(Player p, List<Block> blocks, Block origin) {
        UUID id = p.getUniqueId();
        breaking.add(id);
        try {
            for (Block b : blocks) {
                if (b.equals(origin)) continue;
                if (p.getInventory().getItemInMainHand().getType().isAir()) break;   // tool broke
                p.breakBlock(b);
            }
        } finally {
            breaking.remove(id);
        }
    }

    // Vein Miner + Timber: hold SNEAK while breaking to activate
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if (breaking.contains(p.getUniqueId()) || !p.isSneaking()) return;
        ItemStack tool = p.getInventory().getItemInMainHand();
        if (tool.getType().isAir()) return;
        Block b = e.getBlock();
        int l;

        if ((l = Items.level(tool, CE.VEIN_MINER)) > 0 && isOre(b.getType())) {
            Material type = b.getType();
            breakAll(p, flood(b, x -> x.getType() == type, 8 + 8 * l), b);
        } else if (Items.level(tool, CE.TIMBER) > 0 && Tag.LOGS.isTagged(b.getType())) {
            breakAll(p, flood(b, x -> Tag.LOGS.isTagged(x.getType()), 100), b);
        }
    }

    // Autosmelt, Quarry (double drops), Vortex (drops straight to inventory)
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrops(BlockDropItemEvent e) {
        Player p = e.getPlayer();
        ItemStack tool = p.getInventory().getItemInMainHand();
        if (tool.getType().isAir()) return;

        if (Items.level(tool, CE.AUTOSMELT) > 0) {
            for (Item it : e.getItems()) {
                ItemStack s = it.getItemStack();
                Material out = CustomEnchants.SMELT.get(s.getType());
                if (out != null) it.setItemStack(new ItemStack(out, s.getAmount()));
            }
        }

        int q = Items.level(tool, CE.QUARRY);
        if (q > 0 && Items.roll(12 * q)) {
            for (Item it : e.getItems()) {
                ItemStack s = it.getItemStack();
                s.setAmount(Math.min(s.getMaxStackSize(), s.getAmount() * 2));
                it.setItemStack(s);
            }
        }

        if (Items.level(tool, CE.VORTEX) > 0) {
            Iterator<Item> iter = e.getItems().iterator();
            while (iter.hasNext()) {
                Item it = iter.next();
                Map<Integer, ItemStack> left = p.getInventory().addItem(it.getItemStack());
                if (left.isEmpty()) iter.remove();
                else it.setItemStack(left.values().iterator().next());
            }
        }
    }
}

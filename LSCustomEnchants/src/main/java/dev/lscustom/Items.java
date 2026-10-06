package dev.lscustom;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/** Reading/writing enchant levels on items, plus small helpers. */
public final class Items {
    private Items() {}

    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();
    private static final String[] ROMAN = {"", "I", "II", "III", "IV", "V"};
    private static final Map<CE, NamespacedKey> KEYS = new EnumMap<>(CE.class);
    private static final String MARK = "◆ ";

    public static NamespacedKey key(CE ce) {
        return KEYS.computeIfAbsent(ce, c -> new NamespacedKey(CustomEnchants.get(), c.id()));
    }
    public static NamespacedKey bookIdKey() { return new NamespacedKey(CustomEnchants.get(), "book_id"); }
    public static NamespacedKey bookLvlKey() { return new NamespacedKey(CustomEnchants.get(), "book_level"); }
    public static NamespacedKey artemisKey() { return new NamespacedKey(CustomEnchants.get(), "artemis_arrow"); }

    public static String roman(int n) { return n >= 0 && n < ROMAN.length ? ROMAN[n] : String.valueOf(n); }

    public static boolean roll(double percent) {
        return ThreadLocalRandom.current().nextDouble() * 100.0 < percent;
    }

    public static double maxHp(LivingEntity e) {
        AttributeInstance a = e.getAttribute(Attribute.MAX_HEALTH);
        return a == null ? 20.0 : a.getValue();
    }

    // ---------- levels ----------

    public static int level(ItemStack it, CE ce) {
        if (it == null || it.getType().isAir() || !it.hasItemMeta() || CustomEnchants.DISABLED.contains(ce)) return 0;
        Integer v = it.getItemMeta().getPersistentDataContainer().get(key(ce), PersistentDataType.INTEGER);
        return v == null ? 0 : v;
    }

    /** Highest level of this enchant across the 4 worn armor pieces. */
    public static int armor(Player p, CE ce) {
        int best = 0;
        for (ItemStack a : p.getInventory().getArmorContents()) best = Math.max(best, level(a, ce));
        return best;
    }

    public static int hand(Player p, CE ce) { return level(p.getInventory().getItemInMainHand(), ce); }

    // ---------- editing ----------

    public static void set(ItemStack it, CE ce, int lvl) {
        ItemMeta m = it.getItemMeta();
        m.getPersistentDataContainer().set(key(ce), PersistentDataType.INTEGER, lvl);
        relore(m);
        it.setItemMeta(m);
    }

    public static boolean remove(ItemStack it, CE ce) {
        if (level(it, ce) <= 0 && !it.hasItemMeta()) return false;
        ItemMeta m = it.getItemMeta();
        PersistentDataContainer pdc = m.getPersistentDataContainer();
        if (!pdc.has(key(ce), PersistentDataType.INTEGER)) return false;
        pdc.remove(key(ce));
        relore(m);
        it.setItemMeta(m);
        return true;
    }

    private static void relore(ItemMeta m) {
        List<Component> lore = m.hasLore() && m.lore() != null ? new ArrayList<>(m.lore()) : new ArrayList<>();
        lore.removeIf(c -> PLAIN.serialize(c).startsWith(MARK));
        List<Component> mine = new ArrayList<>();
        for (CE ce : CE.values()) {
            Integer l = m.getPersistentDataContainer().get(key(ce), PersistentDataType.INTEGER);
            if (l != null && l > 0) {
                String name = MARK + ce.display() + (ce.max() > 1 ? " " + roman(l) : "");
                mine.add(Component.text(name, NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
            }
        }
        mine.addAll(lore);
        m.lore(mine.isEmpty() ? null : mine);
    }

    // ---------- books ----------

    public static ItemStack book(CE ce, int lvl) {
        ItemStack b = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta m = b.getItemMeta();
        String name = ce.display() + (ce.max() > 1 ? " " + roman(lvl) : "");
        m.displayName(Component.text(name + " Book", NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false));
        m.lore(List.of(
            Component.text("Custom Enchant", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
            Component.text("Applies to: " + ce.kind().label(), NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false),
            Component.text("Combine in an anvil.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)));
        m.getPersistentDataContainer().set(bookIdKey(), PersistentDataType.STRING, ce.id());
        m.getPersistentDataContainer().set(bookLvlKey(), PersistentDataType.INTEGER, lvl);
        m.setEnchantmentGlintOverride(true);
        b.setItemMeta(m);
        return b;
    }

    public static CE bookEnchant(ItemStack it) {
        if (it == null || it.getType() != Material.ENCHANTED_BOOK || !it.hasItemMeta()) return null;
        String id = it.getItemMeta().getPersistentDataContainer().get(bookIdKey(), PersistentDataType.STRING);
        return CE.parse(id);
    }

    public static int bookLevel(ItemStack it) {
        Integer v = it.getItemMeta().getPersistentDataContainer().get(bookLvlKey(), PersistentDataType.INTEGER);
        return v == null ? 1 : v;
    }
}

package dev.lscustom;

import org.bukkit.Material;

import java.util.function.Predicate;

/** What kind of item an enchant can be applied to. */
public enum Kind {
    MELEE(n -> n.endsWith("_SWORD") || n.endsWith("_AXE") || n.equals("MACE")),
    SWORD(n -> n.endsWith("_SWORD")),
    AXE(n -> n.endsWith("_AXE")),
    PICKAXE(n -> n.endsWith("_PICKAXE")),
    SHOVEL(n -> n.endsWith("_SHOVEL")),
    DIGGER(n -> n.endsWith("_PICKAXE") || n.endsWith("_AXE") || n.endsWith("_SHOVEL")),
    BOW(n -> n.equals("BOW")),
    CROSSBOW(n -> n.equals("CROSSBOW")),
    RANGED(n -> n.equals("BOW") || n.equals("CROSSBOW")),
    ROD(n -> n.equals("FISHING_ROD")),
    HELMET(n -> n.endsWith("_HELMET")),
    CHEST(n -> n.endsWith("_CHESTPLATE")),
    LEGS(n -> n.endsWith("_LEGGINGS")),
    BOOTS(n -> n.endsWith("_BOOTS")),
    ARMOR(n -> n.endsWith("_HELMET") || n.endsWith("_CHESTPLATE") || n.endsWith("_LEGGINGS") || n.endsWith("_BOOTS")),
    ANY(n -> true);

    private final Predicate<String> test;

    Kind(Predicate<String> test) { this.test = test; }

    public boolean matches(Material m) { return m != null && !m.isAir() && test.test(m.name()); }

    public String label() { return name().toLowerCase(); }
}

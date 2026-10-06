package dev.lscustom;

import java.util.Locale;

/** Every custom enchant (hoe enchants intentionally excluded). */
public enum CE {
    ADRENALINE("Adrenaline", 3, Kind.ARMOR),
    ARTEMIS("Artemis", 3, Kind.RANGED),
    ASCENT("Ascent", 2, Kind.BOOTS),
    ATHLETIC("Athletic", 3, Kind.BOOTS),
    AUTOLOADING_HOLSTER("Autoloading Holster", 1, Kind.CROSSBOW),
    AUTOSMELT("Autosmelt", 1, Kind.PICKAXE),
    BERSERK("Berserk", 3, Kind.MELEE),
    BOLT("Bolt", 3, Kind.MELEE),
    BOUNCEBACK("Bounceback", 3, Kind.ARMOR),
    BULWARK("Bulwark", 3, Kind.ARMOR),
    CLAPBACK("Clapback", 3, Kind.ARMOR),
    CLEAVING("Cleaving", 3, Kind.AXE),
    COMPOUND("Compound", 3, Kind.MELEE),
    CONCUSS("Concuss", 3, Kind.MELEE),
    COUNTER("Counter", 3, Kind.ARMOR),
    CRITICAL("Critical", 3, Kind.MELEE),
    DEFLECTOR("Deflector", 3, Kind.ARMOR),
    DELUSIONAL_STRENGTH("Delusional Strength", 2, Kind.MELEE),
    DEMOLITIONIST("Demolitionist", 3, Kind.ARMOR),
    DRUNKEN("Drunken", 3, Kind.ARMOR),
    ENDURANCE("Endurance", 3, Kind.ARMOR),
    EXTINGUISH("Extinguish", 3, Kind.ARMOR),
    FIREFLY("Firefly", 1, Kind.HELMET),
    FIRST_STRIKE("First Strike", 3, Kind.MELEE),
    FLING("Fling", 3, Kind.MELEE),
    FRENZY("Frenzy", 3, Kind.MELEE),
    FROST("Frost", 3, Kind.MELEE),
    HARDHAT("Hardhat", 3, Kind.HELMET),
    HARMONIC("Harmonic", 3, Kind.ARMOR),
    HEADLESS("Headless", 3, Kind.MELEE),
    HEAVENLY("Heavenly", 3, Kind.CHEST),
    HOMING("Homing", 3, Kind.RANGED),
    HOOK("Hook", 3, Kind.ROD),
    LIFEBLOOM("Lifebloom", 3, Kind.CHEST),
    LIFESTEAL("Lifesteal", 3, Kind.MELEE),
    LOW_GROUND("Low Ground", 3, Kind.MELEE),
    QUARRY("Quarry", 3, Kind.PICKAXE),
    RALLY("Rally", 3, Kind.ARMOR),
    REAPER("Reaper", 3, Kind.MELEE),
    REINFORCE("Reinforce", 3, Kind.ANY),
    SAND_PAPER("Sand Paper", 3, Kind.SHOVEL),
    SHOCK("Shock", 3, Kind.MELEE),
    SLASH("Slash", 3, Kind.MELEE),
    SLAYER("Slayer", 3, Kind.MELEE),
    SOULBOUND("Soulbound", 1, Kind.ANY),
    SUSTENANCE("Sustenance", 3, Kind.ARMOR),
    TIMBER("Timber", 1, Kind.AXE),
    TRICKSTER("Trickster", 3, Kind.ARMOR),
    UNBROKEN_CHAIN("Unbroken Chain", 3, Kind.MELEE),
    UNDERDOG("Underdog", 3, Kind.MELEE),
    UNSTABLE("Unstable", 3, Kind.ARMOR),
    VEIN_MINER("Vein Miner", 3, Kind.PICKAXE),
    VOLLEY("Volley", 3, Kind.BOW),
    VORTEX("Vortex", 1, Kind.DIGGER),
    WEIGHTLESS("Weightless", 1, Kind.BOOTS);

    private final String display;
    private final int max;
    private final Kind kind;

    CE(String display, int max, Kind kind) { this.display = display; this.max = max; this.kind = kind; }

    public String display() { return display; }
    public int max() { return max; }
    public Kind kind() { return kind; }
    public String id() { return name().toLowerCase(Locale.ROOT); }

    public static CE parse(String s) {
        if (s == null) return null;
        String k = s.toUpperCase(Locale.ROOT).trim().replace(' ', '_').replace('-', '_');
        try { return valueOf(k); } catch (IllegalArgumentException e) { return null; }
    }
}

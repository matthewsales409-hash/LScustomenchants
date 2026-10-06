package dev.lscustom;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public final class CustomEnchants extends JavaPlugin {
    private static CustomEnchants inst;
    public static CustomEnchants get() { return inst; }

    public static final Set<CE> DISABLED = EnumSet.noneOf(CE.class);
    public static final Map<Material, Material> SMELT = new EnumMap<>(Material.class);

    @Override
    public void onEnable() {
        inst = this;
        saveDefaultConfig();
        loadSettings();
        buildSmeltMap();

        Bukkit.getPluginManager().registerEvents(new CombatListener(), this);
        Bukkit.getPluginManager().registerEvents(new ToolListener(), this);
        Bukkit.getPluginManager().registerEvents(new MiscListener(), this);

        CeCommand cmd = new CeCommand();
        getCommand("ce").setExecutor(cmd);
        getCommand("ce").setTabCompleter(cmd);

        new PassiveTask().runTaskTimer(this, 40L, 40L);
        getLogger().info("Loaded " + CE.values().length + " custom enchants.");
    }

    public void loadSettings() {
        reloadConfig();
        DISABLED.clear();
        for (String s : getConfig().getStringList("disabled")) {
            CE ce = CE.parse(s);
            if (ce != null) DISABLED.add(ce);
        }
    }

    /** Autosmelt table built from the server's own furnace recipes (ores / raw metals only). */
    private void buildSmeltMap() {
        SMELT.clear();
        Iterator<Recipe> it = Bukkit.recipeIterator();
        while (it.hasNext()) {
            Recipe r = it.next();
            if (!(r instanceof FurnaceRecipe fr)) continue;
            if (fr.getInputChoice() instanceof RecipeChoice.MaterialChoice mc) {
                for (Material m : mc.getChoices()) {
                    String n = m.name();
                    if (n.endsWith("_ORE") || n.startsWith("RAW_") || n.equals("ANCIENT_DEBRIS")) {
                        SMELT.putIfAbsent(m, fr.getResult().getType());
                    }
                }
            }
        }
    }
}

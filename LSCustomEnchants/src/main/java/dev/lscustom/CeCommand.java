package dev.lscustom;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CeCommand implements TabExecutor {

    private static void msg(CommandSender s, String text, NamedTextColor c) {
        s.sendMessage(Component.text(text, c));
    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] a) {
        if (!s.hasPermission("ce.admin")) { msg(s, "No permission.", NamedTextColor.RED); return true; }
        if (a.length == 0) {
            msg(s, "/ce list | enchant <name> [lvl] | remove <name> | book <name> [lvl] [player] | reload", NamedTextColor.GRAY);
            return true;
        }

        switch (a[0].toLowerCase(Locale.ROOT)) {
            case "list" -> {
                msg(s, "Custom enchants (" + CE.values().length + "):", NamedTextColor.AQUA);
                for (CE ce : CE.values()) {
                    msg(s, " - " + ce.display() + " (max " + ce.max() + ", " + ce.kind().label() + ")"
                        + (CustomEnchants.DISABLED.contains(ce) ? " [disabled]" : ""), NamedTextColor.GRAY);
                }
            }
            case "reload" -> { CustomEnchants.get().loadSettings(); msg(s, "Reloaded.", NamedTextColor.GREEN); }
            case "enchant", "remove" -> {
                if (!(s instanceof Player p)) { msg(s, "Players only.", NamedTextColor.RED); return true; }
                if (a.length < 2) { msg(s, "Specify an enchant.", NamedTextColor.RED); return true; }
                CE ce = CE.parse(a[1]);
                if (ce == null) { msg(s, "Unknown enchant. Use /ce list", NamedTextColor.RED); return true; }
                ItemStack it = p.getInventory().getItemInMainHand();
                if (it.getType() == Material.AIR) { msg(s, "Hold an item.", NamedTextColor.RED); return true; }

                if (a[0].equalsIgnoreCase("remove")) {
                    msg(s, Items.remove(it, ce) ? "Removed " + ce.display() + "." : "Item doesn't have that enchant.",
                        NamedTextColor.YELLOW);
                    return true;
                }
                if (!ce.kind().matches(it.getType())) {
                    msg(s, ce.display() + " only applies to: " + ce.kind().label(), NamedTextColor.RED);
                    return true;
                }
                int lvl = a.length > 2 ? parse(a[2], 1) : 1;
                lvl = Math.max(1, Math.min(ce.max(), lvl));
                Items.set(it, ce, lvl);
                msg(s, "Applied " + ce.display() + " " + Items.roman(lvl) + ".", NamedTextColor.GREEN);
            }
            case "book" -> {
                if (a.length < 2) { msg(s, "Specify an enchant.", NamedTextColor.RED); return true; }
                CE ce = CE.parse(a[1]);
                if (ce == null) { msg(s, "Unknown enchant. Use /ce list", NamedTextColor.RED); return true; }
                int lvl = Math.max(1, Math.min(ce.max(), a.length > 2 ? parse(a[2], 1) : 1));
                Player target = a.length > 3 ? org.bukkit.Bukkit.getPlayerExact(a[3]) : (s instanceof Player pl ? pl : null);
                if (target == null) { msg(s, "Player not found.", NamedTextColor.RED); return true; }
                target.getInventory().addItem(Items.book(ce, lvl)).values()
                    .forEach(left -> target.getWorld().dropItem(target.getLocation(), left));
                msg(s, "Gave " + target.getName() + " a " + ce.display() + " book.", NamedTextColor.GREEN);
            }
            default -> msg(s, "Unknown subcommand.", NamedTextColor.RED);
        }
        return true;
    }

    private static int parse(String s, int def) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return def; }
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String label, String[] a) {
        List<String> out = new ArrayList<>();
        if (!s.hasPermission("ce.admin")) return out;
        if (a.length == 1) {
            for (String x : List.of("list", "enchant", "remove", "book", "reload"))
                if (x.startsWith(a[0].toLowerCase(Locale.ROOT))) out.add(x);
        } else if (a.length == 2 && !a[0].equalsIgnoreCase("list") && !a[0].equalsIgnoreCase("reload")) {
            for (CE ce : CE.values()) if (ce.id().startsWith(a[1].toLowerCase(Locale.ROOT))) out.add(ce.id());
        } else if (a.length == 3 && (a[0].equalsIgnoreCase("enchant") || a[0].equalsIgnoreCase("book"))) {
            CE ce = CE.parse(a[1]);
            if (ce != null) for (int i = 1; i <= ce.max(); i++) out.add(String.valueOf(i));
        } else if (a.length == 4 && a[0].equalsIgnoreCase("book")) {
            for (Player p : org.bukkit.Bukkit.getOnlinePlayers()) out.add(p.getName());
        }
        return out;
    }
}

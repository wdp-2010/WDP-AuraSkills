package dev.aurelium.auraskills.bukkit.menus;

import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.bukkit.menus.shared.GlobalItems;
import dev.aurelium.slate.builder.MenuBuilder;

public class SellItemsMenu {

    private final AuraSkills plugin;

    public SellItemsMenu(AuraSkills plugin) {
        this.plugin = plugin;
    }

    public void build(MenuBuilder menu) {
        var globalItems = new GlobalItems(plugin);
        menu.item("close", globalItems::close);
        menu.item("back", item -> {
            item.onClick(c -> plugin.getSlate().openMenu(c.player(), "shop"));
        });
        menu.fillItem(globalItems::fill);

        // Simple balance display without complex logic
        menu.item("balance", item -> {
            item.replace("skill_coins", p -> "0");
        });

        // Simple item placeholder
        menu.item("item", item -> {
            item.replace("display_name", p -> "Test Item");
            item.replace("price", p -> "100");
            item.replace("inventory_count", p -> "0");
            item.replace("cooldown_lore", p -> "Ready to sell");
        });
    }
}

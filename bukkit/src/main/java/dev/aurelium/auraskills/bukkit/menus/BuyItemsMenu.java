package dev.aurelium.auraskills.bukkit.menus;

import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.bukkit.menus.shared.GlobalItems;
import dev.aurelium.slate.builder.MenuBuilder;

public class BuyItemsMenu {

    private final AuraSkills plugin;

    public BuyItemsMenu(AuraSkills plugin) {
        this.plugin = plugin;
    }

    public void build(MenuBuilder menu) {
        var globalItems = new GlobalItems(plugin);
        menu.item("close", globalItems::close);
        menu.item("back", item -> {
            item.onClick(c -> plugin.getSlate().openMenu(c.player(), "shop"));
        });
        menu.fillItem(globalItems::fill);

        // Simple balance display
        menu.item("balance", item -> {
            item.replace("skill_coins", p -> "0");
        });

        // Simple item placeholder
        menu.item("item", item -> {
            item.replace("display_name", p -> "Test Buy Item");
            item.replace("price", p -> "50");
            item.replace("amount", p -> "1");
            item.replace("stock", p -> "∞");
            item.replace("afford_lore", p -> "Ready to buy");
            item.replace("stock_lore", p -> "In stock");
            item.replace("click_lore", p -> "Click to buy!");
        });
    }
}

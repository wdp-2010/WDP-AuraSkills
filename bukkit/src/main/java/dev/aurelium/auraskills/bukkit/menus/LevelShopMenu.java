package dev.aurelium.auraskills.bukkit.menus;

import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.bukkit.menus.shared.GlobalItems;
import dev.aurelium.slate.builder.MenuBuilder;

public class LevelShopMenu {

    private final AuraSkills plugin;

    public LevelShopMenu(AuraSkills plugin) {
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

        // Simple skill placeholder
        menu.item("skill", item -> {
            item.replace("skill_name", p -> "Test Skill");
            item.replace("current_level", p -> "1");
            item.replace("next_level", p -> "2");
            item.replace("max_level", p -> "100");
            item.replace("cost", p -> "100");
            item.replace("afford_lore", p -> "Ready to purchase");
            item.replace("level_lore", p -> "Level available");
            item.replace("click_lore", p -> "Click to buy level!");
            item.replace("material", p -> "DIAMOND_SWORD");
        });
    }
}

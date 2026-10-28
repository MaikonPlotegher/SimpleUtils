package io.github.mooy1.simpleutils.implementation.tools;

import javax.annotation.Nonnull;

import io.github.mooy1.simpleutils.SimpleUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

import io.github.thebusybiscuit.slimefun5.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun5.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun5.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun5.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun5.core.attributes.EnergyNetComponent;
import io.github.thebusybiscuit.slimefun5.core.attributes.NotPlaceable;
import io.github.thebusybiscuit.slimefun5.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun5.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun5.implementation.items.SimpleSlimefunItem;
import io.github.thebusybiscuit.slimefun5.libraries.dough.protection.Interaction;

public final class Wrench extends SimpleSlimefunItem<ItemUseHandler> implements NotPlaceable {

    private final Map<UUID, Long> coolDowns = new HashMap<>();

    private boolean checkCooldown(UUID uuid) {
        long now = System.currentTimeMillis();
        if (now - coolDowns.getOrDefault(uuid, 0L) > 50) {
            coolDowns.put(uuid, now);
            return true;
        }
        return false;
    }

    public Wrench(ItemGroup category, SlimefunItemStack item, RecipeType type, ItemStack[] recipe) {
        super(category, item, type, recipe);
    }

    @Nonnull
    @Override
    public ItemUseHandler getItemHandler() {
        return e -> {
            e.setUseItem(Event.Result.DENY);
            e.setUseBlock(Event.Result.DENY);

            if (this.checkCooldown(e.getPlayer().getUniqueId())
                    && e.getClickedBlock().isPresent() && e.getSlimefunBlock().isPresent()) {

                Block b = e.getClickedBlock().get();
                SlimefunItem sfItem = e.getSlimefunBlock().get();

                if (Slimefun.getProtectionManager().hasPermission(e.getPlayer(), b, Interaction.BREAK_BLOCK)
                        && !sfItem.useVanillaBlockBreaking()
                        && (b.getType() == Material.PLAYER_HEAD || b.getType() == Material.PLAYER_WALL_HEAD || sfItem instanceof EnergyNetComponent)) {

                    BlockBreakEvent event = new BlockBreakEvent(b, e.getPlayer());

                    Bukkit.getPluginManager().callEvent(event);

                    if (!event.isCancelled()) {
                        b.setType(Material.AIR);
                    }
                }
            }
        };
    }

    public void register(SimpleUtils plugin) {
        super.register(plugin);
    }
}

package io.github.mooy1.simpleutils.implementation.blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;

import io.github.mooy1.simpleutils.SimpleUtils;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import io.github.thebusybiscuit.slimefun5.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun5.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun5.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun5.core.multiblocks.MultiBlockMachine;
import io.github.thebusybiscuit.slimefun5.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun5.implementation.items.blocks.OutputChest;
import io.github.thebusybiscuit.slimefun5.libraries.dough.collections.RandomizedSet;
import io.github.thebusybiscuit.slimefun5.libraries.dough.items.ItemUtils;

public final class Sieve extends MultiBlockMachine {

    private final float itemChance = (float) Math.max(0.0, Math.min(100.0, SimpleUtils.getInstance().getConfig().getDouble("sieve-item-chance", 20.0)));
    private final RandomizedSet<ItemStack> recipes = new RandomizedSet<>();
    private final List<ItemStack> display = new ArrayList<>();

    public Sieve(ItemGroup category, SlimefunItemStack item, ItemStack[] recipe, BlockFace face) {
        super(category, item, recipe, face);

        float airWeight = 25 * (100 - this.itemChance);
        if (airWeight > 0) {
            this.recipes.add(new ItemStack(Material.AIR), airWeight);
        }
        addRecipe(SlimefunItems.ALUMINUM_DUST.item(), 2);
        addRecipe(SlimefunItems.COPPER_DUST.item(), 3);
        addRecipe(SlimefunItems.IRON_DUST.item(), 2);
        addRecipe(SlimefunItems.LEAD_DUST.item(), 1);
        addRecipe(SlimefunItems.GOLD_DUST.item(), 2);
        addRecipe(SlimefunItems.ZINC_DUST.item(), 2);
        addRecipe(SlimefunItems.MAGNESIUM_DUST.item(), 1);
        addRecipe(SlimefunItems.SILVER_DUST.item(), 1);
        addRecipe(SlimefunItems.TIN_DUST.item(), 1);
        addRecipe(new ItemStack(Material.CLAY_BALL), 4);
        addRecipe(new ItemStack(Material.FLINT), 3);
        addRecipe(new ItemStack(Material.IRON_NUGGET), 3);
    }

    private void addRecipe(ItemStack item, float chance) {
        float finalChance = chance * this.itemChance;
        if (finalChance > 0) {
            this.recipes.add(item, finalChance);
        }
        this.display.add(new ItemStack(Material.GRAVEL));
        ItemStack displayItem = item.clone();
        var meta = displayItem.getItemMeta();
        if (meta != null) {
            meta.lore(List.of(
                    Component.empty(),
                    Component.text(
                            "Chance: " + finalChance + "%",
                            NamedTextColor.GOLD)));
            displayItem.setItemMeta(meta);}
        this.display.add(displayItem);}

    @Nonnull
    @Override
    public List<ItemStack> getDisplayRecipes() {
        return this.display;
    }

    @Override
    public void onInteract(Player p, Block b) {
        ItemStack input = p.getInventory().getItemInMainHand();

        if (SlimefunItem.getByItem(input) != null || input.getType() != Material.GRAVEL) {
            p.sendMessage(ChatColor.RED + "Invalid Recipe!");
            return;
        }

        ItemStack item = this.recipes.getRandom();

        if (p.getGameMode() != GameMode.CREATIVE) {
            ItemUtils.consumeItem(input, 1, false);
        }

        if (item.getType() == Material.AIR) {
            return;
        }

        p.playSound(b.getLocation(), Sound.BLOCK_SAND_BREAK, 1, 1);

        ItemStack output = item.clone();

        org.bukkit.Bukkit.getScheduler().runTaskLater(SimpleUtils.getInstance(), () -> {
            Optional<Inventory> outputChest = OutputChest.findOutputChestFor(b.getRelative(BlockFace.DOWN), output);
            if (outputChest.isPresent()) {
                outputChest.get().addItem(output);
            } else {
                b.getWorld().dropItemNaturally(b.getLocation().add(0, .5, 0), output);
            }
            p.playSound(b.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1, 1);
        }, 40L);
    }

}

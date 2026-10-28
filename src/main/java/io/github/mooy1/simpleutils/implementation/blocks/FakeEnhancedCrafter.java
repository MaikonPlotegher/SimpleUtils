package io.github.mooy1.simpleutils.implementation.blocks;

import java.util.List;

import javax.annotation.Nullable;

import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun5.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun5.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun5.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun5.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun5.implementation.items.backpacks.SlimefunBackpack;
import io.github.thebusybiscuit.slimefun5.implementation.items.multiblocks.EnhancedCraftingTable;
import io.github.thebusybiscuit.slimefun5.utils.SlimefunUtils;

class FakeEnhancedCrafter extends EnhancedCraftingTable {

    FakeEnhancedCrafter(ItemGroup category, SlimefunItemStack item) {
        super(category, item);
    }

    @Nullable
    ItemStack craft(ItemStack[] input) {
        EnhancedCraftingTable real = SlimefunItems.ENHANCED_CRAFTING_TABLE.getItem(EnhancedCraftingTable.class);
        if (real == null) {
            return null;
        }

        List<ItemStack[]> recipes = real.getRecipes();
        loop: for (int i = 0; i < recipes.size(); i += 2) {
            ItemStack[] recipe = recipes.get(i);
            if (recipe == null) {
                continue;
            }

            for (int j = 0; j < 9; j++) {
                ItemStack in = (j < input.length) ? input[j] : null;
                ItemStack req = (j < recipe.length) ? recipe[j] : null;

                boolean inEmpty = in == null || in.getType().isAir();
                boolean reqEmpty = req == null || req.getType().isAir();

                if (inEmpty && reqEmpty) {
                    continue;
                }
                if (inEmpty || reqEmpty) {
                    continue loop;
                }

                if (!SlimefunUtils.isItemSimilar(in, req, true, true, false) && !sameWoodMatch(in, req)) {
                    if (SlimefunItem.getByItem(req) instanceof SlimefunBackpack) {
                        if (!SlimefunUtils.isItemSimilar(in, req, false, true, false)) {
                            continue loop;
                        }
                    } else {
                        continue loop;
                    }
                }
            }

            if (i + 1 < recipes.size() && recipes.get(i + 1).length > 0) {
                return recipes.get(i + 1)[0];
            }
        }

        return null;
    }

}

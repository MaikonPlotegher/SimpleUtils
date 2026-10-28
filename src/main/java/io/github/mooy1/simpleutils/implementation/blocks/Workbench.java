package io.github.mooy1.simpleutils.implementation.blocks;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import io.github.thebusybiscuit.slimefun5.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun5.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun5.implementation.handlers.SimpleBlockBreakHandler;
import io.github.thebusybiscuit.slimefun5.libraries.dough.protection.Interaction;
import io.github.thebusybiscuit.slimefun5.utils.ChestMenuUtils;
import io.github.thebusybiscuit.slimefun5.utils.compatibility.BukkitKeys;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.interfaces.InventoryBlock;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import io.github.thebusybiscuit.slimefun5.libraries.keys.NamespacedKey;
import io.github.mooy1.simpleutils.SimpleUtils;
import io.github.thebusybiscuit.slimefun5.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun5.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun5.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun5.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun5.implementation.items.backpacks.SlimefunBackpack;
import io.github.thebusybiscuit.slimefun5.libraries.dough.items.CustomItemStack;

import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;

@ParametersAreNonnullByDefault
public final class Workbench extends SlimefunItem implements InventoryBlock, Listener {

    private static final int[] INPUT_SLOTS = { 10,11,12,19,20,21,28,29,30};
    private static final int[] INPUT_BORDER = { 0,1,2,3,4,9,13,18,22,27,31,36,37,38,39,40};
    private static final int OUTPUT_SLOT = 24;
    private static final ItemStack NO_OUTPUT = CustomItemStack.create(Material.BARRIER, " ");
    private static final ItemStack INPUT_BORDER_ITEM = CustomItemStack.create(ChestMenuUtils.getInputSlotTexture(), "&9Input");
    private static final ItemStack OUTPUT_BORDER_ITEM = CustomItemStack.create(ChestMenuUtils.getOutputSlotTexture(), "&6Output");

    private final NamespacedKey displayKey = SimpleUtils.createPortedKey("display");
    private final BiFunction<ItemStack[], Player, ItemStack> craftItem;
    private final Map<UUID, BlockMenu> openMenus = new HashMap<>();
    private final FakeEnhancedCrafter fakeEnhancedCrafter;

    public Workbench(ItemGroup category, SlimefunItemStack itemStack, RecipeType recipeType, ItemStack[] r) {
        super(category, itemStack, recipeType, r);

        org.bukkit.Bukkit.getPluginManager().registerEvents(this, SimpleUtils.getInstance());

        fakeEnhancedCrafter = new FakeEnhancedCrafter(category, itemStack);
        craftItem = Workbench::craftItem;

        addItemHandler(onBlockBreak());

        new BlockMenuPreset(getId(), itemStack.getItemMeta().getDisplayName()) {

            @Override
            public void init() {
                optOutOfHeaderItem();
                drawBackground(INPUT_BORDER_ITEM, INPUT_BORDER);
                drawBackground(OUTPUT_BORDER_ITEM, new int[] {
                        14, 15, 16, 23, 25, 32, 33, 34
                });
                drawBackground(new int[] {
                        5, 6, 7, 8, 17, 26, 35, 41, 42, 43, 44
                });
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(ItemTransportFlow flow) {
                return flow == ItemTransportFlow.INSERT ? getInputSlots() : getOutputSlots();
            }

            @Override
            public boolean canOpen(Block b, Player p) {
                if (p.hasPermission("slimefun.inventory.bypass")) {
                    return true;
                }
                return canUse(p, false) && (
                        Slimefun.instance().isUnitTest()
                                || Slimefun.getProtectionManager().hasPermission(p, b.getLocation(), Interaction.INTERACT_BLOCK)
                );
            }

            @Override
            public void newInstance(@Nonnull BlockMenu menu, @Nonnull Block b) {
                menu.addMenuOpeningHandler(p -> Workbench.this.openMenus.put(p.getUniqueId(), menu));
                menu.addMenuCloseHandler(p -> Workbench.this.openMenus.remove(p.getUniqueId()));
                menu.addPlayerInventoryClickHandler((p, slot, item, action) -> {
                    if (action.isShiftClicked()) {
                        refreshOutput(menu, p);
                    }
                    return true;
                });
                for (int i : INPUT_SLOTS) {
                    menu.addMenuClickHandler(i, (p, slot, item, action) -> {
                        refreshOutput(menu, p);
                        return true;
                    });
                }
                menu.addMenuClickHandler(OUTPUT_SLOT, (p, slot, item, action) -> {
                    craft(p, menu, action.isShiftClicked());
                    return false;
                });
            }
        };
    }

    @Nonnull
    private BlockBreakHandler onBlockBreak() {
        return new SimpleBlockBreakHandler() {

            @Override
            public void onBlockBreak(Block b) {
                BlockMenu inv = BlockStorage.getInventory(b);

                if (inv != null) {
                    inv.dropItems(b.getLocation(), getInputSlots());
                }
            }

        };
    }

    @Override
    public int[] getInputSlots() {
        return INPUT_SLOTS;
    }

    @Override
    public int[] getOutputSlots() {
        return new int[0];
    }

    private void craft(Player p, BlockMenu menu, boolean max) {
        ItemStack[] input = new ItemStack[9];
        int[] amounts = new int[9];

        for (int i = 0 ; i < INPUT_SLOTS.length ; i++) {
            ItemStack in = menu.getItemInSlot(INPUT_SLOTS[i]);
            if (in != null) {
                input[i] = in;
                amounts[i] = in.getAmount();
            }
        }

        ItemStack output = fakeEnhancedCrafter.craft(input);

        if (output == null) {
            output = craftItem.apply(input, p);
            if (output == null) {
                return;
            }
        } else {
            SlimefunItem item = SlimefunItem.getByItem(output);

            if (item != null) {
                if (!item.canUse(p, true)) {
                    return;
                }
                if (item instanceof SlimefunBackpack) {
                    p.sendMessage(ChatColor.RED + "Use the enhanced crafting table to upgrade backpacks!");
                    return;
                }
            }
        }

        int lowestAmount = 65;
        for (int i : amounts) {
            if (i > 0 && i < lowestAmount) {
                lowestAmount = i;
            }
        }

        if (lowestAmount == 65) {
            return;
        }

        if (max) {
            int total = output.getAmount() * lowestAmount;
            int fullStacks = total / output.getMaxStackSize();
            int partialStack = total % output.getMaxStackSize();

            ItemStack[] arr;
            if (partialStack == 0) {
                arr = new ItemStack[fullStacks];
            }
            else {
                arr = new ItemStack[fullStacks + 1];
                arr[fullStacks] = CustomItemStack.create(output, partialStack);
            }

            while (fullStacks-- != 0) {
                arr[fullStacks] = CustomItemStack.create(output, output.getMaxStackSize());
            }

            Map<Integer, ItemStack> remaining = p.getInventory().addItem(arr);
            for (ItemStack stack : remaining.values()) {
                p.getWorld().dropItemNaturally(p.getLocation(), stack);
            }

            refreshOutput(menu, p);
        }
        else {
            Map<Integer, ItemStack> remaining = p.getInventory().addItem(output.clone());
            for (ItemStack stack : remaining.values()) {
                p.getWorld().dropItemNaturally(p.getLocation(), stack);
            }

            if (lowestAmount == 1) {
                refreshOutput(menu, p);
            }

            lowestAmount = 1;
        }

        for (int i = 0 ; i < 9 ; i++) {
            if (amounts[i] != 0) {
                menu.consumeItem(INPUT_SLOTS[i], lowestAmount, true);
            }
        }
    }

    private void refreshOutput(@Nonnull BlockMenu menu, Player p) {
        org.bukkit.Bukkit.getScheduler().runTask(SimpleUtils.getInstance(), () -> {
            ItemStack[] input = new ItemStack[9];
            for (int i = 0 ; i < INPUT_SLOTS.length ; i++) {
                input[i] = menu.getItemInSlot(INPUT_SLOTS[i]);
            }

            ItemStack output = fakeEnhancedCrafter.craft(input);

            if (output == null) {
                output = craftItem.apply(input, p);
            }

            if (output == null) {
                menu.replaceExistingItem(OUTPUT_SLOT, NO_OUTPUT);
            }
            else {
                output = output.clone();
                ItemMeta meta = output.getItemMeta();
                meta.getPersistentDataContainer().set(
                        (org.bukkit.NamespacedKey) BukkitKeys.toBukkit(this.displayKey),
                        PersistentDataType.BYTE,
                        (byte) 0
                );
                output.setItemMeta(meta);
                menu.replaceExistingItem(OUTPUT_SLOT, output);
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDrag(@Nonnull InventoryDragEvent e) {
        BlockMenu menu = this.openMenus.get(e.getWhoClicked().getUniqueId());
        if (menu != null) {
            int topSize = e.getView().getTopInventory().getSize();
            boolean touchesForbiddenSlot = false;

            for (int slot : e.getRawSlots()) {
                if (slot < topSize && !isInputSlot(slot)) {
                    touchesForbiddenSlot = true;
                    break;
                }
            }

            if (touchesForbiddenSlot) {
                e.setCancelled(true);
            } else {
                e.setCancelled(false);
                refreshOutput(menu, (Player) e.getWhoClicked());
            }
        }
    }

    private static boolean isInputSlot(int slot) {
        for (int inputSlot : INPUT_SLOTS) {
            if (inputSlot == slot) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private static ItemStack craftItem(ItemStack[] input, Player p) {
        ItemStack output = Bukkit.craftItem(input, p.getWorld(), p);
        return output.getType().isAir() ? null : output;
    }

}
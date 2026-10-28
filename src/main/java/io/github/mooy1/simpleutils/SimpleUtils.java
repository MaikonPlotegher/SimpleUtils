package io.github.mooy1.simpleutils;

import java.util.logging.Level;
import org.bukkit.plugin.java.JavaPlugin;
import io.github.thebusybiscuit.slimefun5.api.SlimefunAddon;
import io.github.thebusybiscuit.slimefun5.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun5.libraries.keys.NamespacedKey;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import io.github.mooy1.simpleutils.implementation.Items;

public final class SimpleUtils extends JavaPlugin implements SlimefunAddon {

    private static SimpleUtils instance;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        Items.setup(this);

        try {
            Slimefun.getItemTranslationService().registerTranslations(this);
        } catch (Exception | LinkageError e) {
            getLogger().log(Level.WARNING, "Failed to register translations: " + e.getMessage());
        }
    }

    @Override
    public void onDisable() {
        instance = null;
    }

    public static SimpleUtils getInstance() {
        return instance;
    }

    public static NamespacedKey createPortedKey(String key) {
        return new NamespacedKey("simpleutils", key);
    }

    @Override
    @NonNull 
    public JavaPlugin getJavaPlugin() {
        return this;
    }

    @Override
    @Nullable 
    public String getBugTrackerURL() {
        return "https://github.com/Mooy1/SimpleUtils/issues";
    }
}

package forbidden.enchantments;

import forbidden.ForbiddenMagic;
import forbidden.config.ForbiddenConfig;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * Registry keys and query helpers for Forbidden Magic dark enchantments.
 * Implements F03: if enchantment disabled/absent in config, level returns 0.
 */
public final class DarkEnchantments {
    public static final ResourceKey<Enchantment> CLUSTER = key("cluster");
    public static final ResourceKey<Enchantment> CONSUMING = key("consuming");
    public static final ResourceKey<Enchantment> CORRUPTING = key("corrupting");
    public static final ResourceKey<Enchantment> EDUCATIONAL = key("educational");
    public static final ResourceKey<Enchantment> GREEDY = key("greedy");
    public static final ResourceKey<Enchantment> IMPACT = key("impact");
    public static final ResourceKey<Enchantment> VOIDTOUCHED = key("voidtouched");
    public static final ResourceKey<Enchantment> WRATH = key("wrath");

    private static ResourceKey<Enchantment> key(String name) {
        return ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath(ForbiddenMagic.ID, name));
    }

    /**
     * Resolves the enchantment level on the given stack.
     * Enforces Decision F03: if enchantment subsystem is disabled or specific config is off, returns 0.
     */
    public static int getLevel(ItemStack stack, ResourceKey<Enchantment> key) {
        if (stack == null || stack.isEmpty() || !ForbiddenConfig.ENCHANTING.get()) {
            return 0;
        }
        if (key == GREEDY && !ForbiddenConfig.GREEDY.get()) {
            return 0;
        }
        ItemEnchantments enchants = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (enchants.isEmpty() && stack.has(DataComponents.STORED_ENCHANTMENTS)) {
            enchants = stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        }
        for (var entry : enchants.entrySet()) {
            if (entry.getKey().is(key)) {
                return entry.getIntValue();
            }
        }
        return 0;
    }

    private DarkEnchantments() {}
}

package forbidden.compat.tinkerer;

import forbidden.config.ForbiddenConfig;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.InterModComms;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.InterModEnqueueEvent;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/**
 * Compatibility handler for Thaumic Tinkerer (Contract T01).
 * Handles:
 * - Registration of 8 Forbidden Magic enchantments and their aspect costs for Osmotic Enchanter.
 * - Registration of KAMI aspects for kamiResource shards (nether_shard and ender_shard) when KAMI is present.
 * - Safe inter-mod communications (IMC) and reflection resilience.
 */
public final class TinkererCompat {
    public static final String MODID = "thaumictinkerer_aop";
    public static final String IMC_REGISTER_ENCHANTMENT = "RegisterEnchantmentCost";

    public static final ResourceLocation NETHER_SHARD = ResourceLocation.parse("thaumictinkerer:nether_shard");
    public static final ResourceLocation ENDER_SHARD = ResourceLocation.parse("thaumictinkerer:ender_shard");

    private static boolean kamiActive = false;

    public static boolean isAvailable() {
        try {
            return ForbiddenConfig.CROSS_MOD.get()
                    && ForbiddenConfig.TINKERER.get()
                    && isModLoaded(MODID);
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean isKamiAvailable() {
        if (!isAvailable()) return false;
        if (kamiActive) return true;
        try {
            // Check if flattened kamiResource shards or Kami classes are present
            boolean hasNetherShard = BuiltInRegistries.ITEM.containsKey(NETHER_SHARD);
            boolean hasEnderShard = BuiltInRegistries.ITEM.containsKey(ENDER_SHARD);
            return hasNetherShard || hasEnderShard;
        } catch (Throwable t) {
            return false;
        }
    }

    public static void setKamiActive(boolean active) {
        kamiActive = active;
    }

    public static void onEnqueueIMC(InterModEnqueueEvent event) {
        if (!isAvailable()) return;

        // Dispatch contract T01: 8 Osmotic Enchanter enchantment costs to Thaumic Tinkerer
        for (OsmoticCostEntry entry : TinkererEnchantmentRegistry.getAll()) {
            InterModComms.sendTo(MODID, IMC_REGISTER_ENCHANTMENT, () -> entry);
        }
    }

    /**
     * Directly registers the 8 Forbidden Magic enchantments into the receiver's catalog (Thaumic Tinkerer)
     * if Thaumic Tinkerer is active on classpath. Returns count of registered entries.
     */
    public static int registerWithReceiver() {
        int registered = 0;
        try {
            Class<?> imcClass = Class.forName("thaumic.tinkerer.compat.TinkererResearchImc");
            var method = imcClass.getMethod("registerEnchantmentFromPayload", Object.class, String.class);
            for (OsmoticCostEntry entry : TinkererEnchantmentRegistry.getAll()) {
                Object res = method.invoke(null, entry, "forbidden");
                if (Boolean.TRUE.equals(res)) registered++;
            }
        } catch (Throwable ignored) {}
        return registered;
    }

    /**
     * Resolves KAMI aspects for a shard item if KAMI is present.
     * Original 1.7.10 Compat.java:
     * - kamiResource 6 (nether shard): DarkAspects.NETHER (2), MAGIC (1), CRYSTAL (1)
     * - kamiResource 7 (ender shard): ELDRITCH (2), MAGIC (1), CRYSTAL (1)
     */
    public static AspectList getKamiAspects(ResourceLocation itemId) {
        if (itemId == null) return null;
        if (NETHER_SHARD.equals(itemId)) {
            Aspect infernus = Aspect.getAspect("forbidden:infernus");
            Aspect magic = Aspect.getAspect("praecantatio");
            Aspect crystal = Aspect.getAspect("vitreus");
            AspectList list = new AspectList();
            if (infernus != null) list.add(infernus, 2);
            if (magic != null) list.add(magic, 1);
            if (crystal != null) list.add(crystal, 1);
            return list;
        } else if (ENDER_SHARD.equals(itemId)) {
            Aspect eldritch = Aspect.getAspect("alienis");
            Aspect magic = Aspect.getAspect("praecantatio");
            Aspect crystal = Aspect.getAspect("vitreus");
            AspectList list = new AspectList();
            if (eldritch != null) list.add(eldritch, 2);
            if (magic != null) list.add(magic, 1);
            if (crystal != null) list.add(crystal, 1);
            return list;
        }
        return null;
    }

    private static boolean isModLoaded(String modId) {
        try {
            var list = ModList.get();
            return list != null && list.isLoaded(modId);
        } catch (Throwable t) {
            return false;
        }
    }

    private TinkererCompat() {}
}

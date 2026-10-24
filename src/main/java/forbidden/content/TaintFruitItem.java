package forbidden.content;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import thaumcraft.api.research.ResearchApi;

public final class TaintFruitItem extends Item {
    public TaintFruitItem(Properties properties) { super(properties); }
    @Override public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            ResearchApi.addWarp(player, ResearchApi.WarpKind.STICKY, 1);
            effect(player, "thaumcraft:flux_taint", false);
            var poison = new MobEffectInstance(MobEffects.POISON, 600, 0, false, true);
            poison.getCures().clear(); player.addEffect(poison);
            if (level.random.nextFloat() < .4F) {
                player.sendSystemMessage(Component.translatable("warp.text.15").withStyle(ChatFormatting.DARK_PURPLE));
                effect(player, "thaumcraft:thaumarhia", true);
            }
        }
        return super.finishUsingItem(stack, level, entity);
    }
    private static void effect(ServerPlayer player, String id, boolean ambient) {
        var holder = BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse(id)).orElseThrow();
        var effect = new MobEffectInstance(holder, 600, 0, ambient, true);
        effect.getCures().clear(); player.addEffect(effect);
    }
}

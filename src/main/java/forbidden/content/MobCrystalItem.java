package forbidden.content;

import forbidden.component.ForbiddenComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;

public final class MobCrystalItem extends Item {
    public MobCrystalItem(Properties properties){super(properties);}
    @Override public Component getName(ItemStack stack){
        var mob=stack.get(ForbiddenComponents.MOB.get());
        if(mob==null)return Component.translatable("item.MobCrystalEmpty.name");
        var type=BuiltInRegistries.ENTITY_TYPE.getOptional(mob);
        return Component.translatable("item.MobCrystal.name",type.<Component>map(t->t.getDescription()).orElse(Component.literal(mob.toString())));
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,java.util.List<Component> lines,TooltipFlag flag){
        var aspect=CageMobCatalog.entries().get(stack.get(ForbiddenComponents.MOB.get()));
        if(aspect!=null)lines.add(Component.literal(aspect.getName()));
    }
}

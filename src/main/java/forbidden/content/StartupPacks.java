package forbidden.content;

import forbidden.config.ForbiddenConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.*;
import net.neoforged.neoforge.event.AddPackFindersEvent;

/** Required packs selected from STARTUP config, before data and assets are loaded. */
public final class StartupPacks {
    public static void add(AddPackFindersEvent event){
        add(event,ForbiddenConfig.NO_LUST.get()?"no_lust":"lust");
        if(ForbiddenConfig.ENCHANTING.get())add(event,"enchanting");
        if(ForbiddenConfig.CROSS_MOD.get()&&ForbiddenConfig.BOTANIA.get()&&net.neoforged.fml.ModList.get().isLoaded("botania"))add(event,"botania");
    }
    private static void add(AddPackFindersEvent event,String name){event.addPackFinders(ResourceLocation.fromNamespaceAndPath("forbidden","packs/"+name),event.getPackType(),Component.literal("Forbidden Magic: "+name),PackSource.BUILT_IN,true,Pack.Position.TOP);}
    private StartupPacks(){}
}

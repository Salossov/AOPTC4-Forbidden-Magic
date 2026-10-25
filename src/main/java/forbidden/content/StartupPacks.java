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
        if(ForbiddenConfig.ENCHANTING.get()&&ForbiddenConfig.GREEDY.get())add(event,"greedy");
        if(ForbiddenConfig.WRATH_CAGE.get())add(event,"cage");
        if(ForbiddenConfig.GLUTTONY.get()!=1)add(event,"gluttony");
        if(ForbiddenConfig.GLUTTONY.get()==2)add(event,"gluttony_hard");
        if(ForbiddenConfig.EMERALD_TRANSMUTATION.get())add(event,"emerald");
        if(ForbiddenConfig.CROSS_MOD.get()&&((ForbiddenConfig.BOTANIA.get()&&net.neoforged.fml.ModList.get().isLoaded("botania"))
            ||(ForbiddenConfig.BLOOD_MAGIC.get()&&net.neoforged.fml.ModList.get().isLoaded("bloodmagic"))
            ||(ForbiddenConfig.ARS_MAGICA.get()&&net.neoforged.fml.ModList.get().isLoaded("arsmagica2"))))add(event,"schools");
        if(ForbiddenConfig.CROSS_MOD.get()&&ForbiddenConfig.BOTANIA.get()&&net.neoforged.fml.ModList.get().isLoaded("botania"))add(event,"botania");
        if(ForbiddenConfig.CROSS_MOD.get()&&ForbiddenConfig.BLOOD_MAGIC.get()&&net.neoforged.fml.ModList.get().isLoaded("bloodmagic"))add(event,"bloodmagic");
        if(forbidden.compat.botania.BotaniaCompat.isAvailable()&&forbidden.compat.bloodmagic.BloodMagicCompat.isAvailable())add(event,"bloodthorn");
    }
    private static void add(AddPackFindersEvent event,String name){event.addPackFinders(ResourceLocation.fromNamespaceAndPath("forbidden","packs/"+name),event.getPackType(),Component.literal("Forbidden Magic: "+name),PackSource.BUILT_IN,true,Pack.Position.TOP);}
    private StartupPacks(){}
}

package forbidden;

import forbidden.compat.Compat;
import forbidden.compat.bloodmagic.BloodMagicCompat;
import forbidden.compat.botania.BotaniaCompat;
import forbidden.component.ForbiddenComponents;
import forbidden.config.ForbiddenConfig;
import forbidden.content.ForbiddenContent;
import forbidden.content.ForbiddenTools;
import forbidden.content.StartupPacks;
import forbidden.content.ForbiddenAspects;
import forbidden.content.ForbiddenWands;
import forbidden.content.ContentCondition;
import forbidden.content.ForbiddenWandVisuals;
import forbidden.content.wands.ForbiddenWandMechanics;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

/** Forbidden Magic port entry point. */
@Mod(ForbiddenMagic.ID)
public final class ForbiddenMagic {
    public static final String ID = "forbidden";

    public ForbiddenMagic(IEventBus bus, ModContainer container) {
        container.registerConfig(ModConfig.Type.STARTUP, ForbiddenConfig.SPEC);
        ForbiddenComponents.register(bus);
        ForbiddenContent.register(bus);
        ForbiddenTools.register(bus);
        BloodMagicCompat.register(bus);
        BotaniaCompat.register(bus);
        ContentCondition.register(bus);
        bus.addListener(StartupPacks::add);
        bus.addListener(Compat::onCommonSetup);
        bus.addListener(Compat::onEnqueueIMC);
        bus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) -> event.enqueueWork(() -> {
            ForbiddenAspects.register();
            ForbiddenWands.register();
            ForbiddenWandVisuals.register();
            ForbiddenWandMechanics.registerAll();
        }));
    }
}

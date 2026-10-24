package forbidden;

import forbidden.component.ForbiddenComponents;
import forbidden.config.ForbiddenConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

/** Foundation only; gameplay registrations follow in tracker blocks B/C. */
@Mod(ForbiddenMagic.ID)
public final class ForbiddenMagic {
    public static final String ID = "forbidden";

    public ForbiddenMagic(IEventBus bus, ModContainer container) {
        container.registerConfig(ModConfig.Type.STARTUP, ForbiddenConfig.SPEC);
        ForbiddenComponents.register(bus);
    }
}

package forbidden.compat;

import forbidden.compat.bloodmagic.BloodMagicCompat;
import forbidden.compat.botania.BotaniaCompat;
import forbidden.compat.tinkerer.TinkererCompat;
import forbidden.compat.twilight.TwilightCompat;
import forbidden.compat.viewers.ViewersCompat;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.InterModEnqueueEvent;

/**
 * Top-level compatibility coordinator for Forbidden Magic cross-mod integrations.
 * Preserves 1.7.10 Compat contracts and tracks modernization/archival statuses.
 */
public final class Compat {
    public static boolean tt = false;
    public static boolean kami = false;
    public static boolean twilight = false;
    public static boolean viewers = false;
    public static boolean bloodmagic = false;
    public static boolean botania = false;

    // Archival and deferred integration statuses
    public static final String SPECIAL_MOBS_STATUS = "Deferred: Special Mobs not ported; reserved for future porting";
    public static final String ARS_MAGICA_STATUS = "Blocked: target mod unavailable on 1.21.1 NeoForge (D8)";
    public static final String EE3_STATUS = "Blocked: target mod unavailable on 1.21.1 NeoForge (D8)";
    public static final String NATURA_STATUS = "Blocked: target mod unavailable on 1.21.1 NeoForge (D8)";
    public static final String WITCHERY_STATUS = "Blocked: target mod unavailable on 1.21.1 NeoForge (D8)";
    public static final String TINKERS_CONSTRUCT_STATUS = "Blocked: target mod unavailable on 1.21.1 NeoForge (D8)";

    public static void initiate() {
        tt = TinkererCompat.isAvailable();
        kami = TinkererCompat.isKamiAvailable();
        twilight = TwilightCompat.isAvailable();
        viewers = ViewersCompat.isAvailable();
        bloodmagic = BloodMagicCompat.isAvailable();
        botania = BotaniaCompat.isAvailable();
    }

    public static void onCommonSetup(FMLCommonSetupEvent event) {
        initiate();
        event.enqueueWork(() -> {
            TwilightCompat.init();
            ViewersCompat.init();
            BloodMagicCompat.commonSetup();
            BotaniaCompat.commonSetup();
        });
    }

    public static void onEnqueueIMC(InterModEnqueueEvent event) {
        TinkererCompat.onEnqueueIMC(event);
    }

    private Compat() {}
}

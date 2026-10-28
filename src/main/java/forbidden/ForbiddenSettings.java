package forbidden;

import aop.ui.api.SettingsRegistry;
import aop.ui.api.UiTheme;
import forbidden.config.ForbiddenConfig;

/** Exposes the original config through the shared AOP UI without changing its values. */
public final class ForbiddenSettings {
    public static void register() {
        SettingsRegistry.register(ForbiddenMagic.ID, "Forbidden Magic",
                new UiTheme("aop_ui", "forbidden", 0xffeadfc9, 0xffb5a696, 0xffd5aa50), ForbiddenConfig.SPEC);
        for (String category : new String[]{"general", "silly", "compatibility", "powerConverters", "focusUpgrades"}) {
            SettingsRegistry.registerCategory(ForbiddenMagic.ID, category, "forbidden.configuration.category." + category);
        }
    }
    private ForbiddenSettings() { }
}

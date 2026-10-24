package forbidden.rules;

import forbidden.config.ContentGates;

/** Cross-product regression for the release's distinct registration/config gates. No Minecraft runtime. */
public final class FoundationRules {
    private static int checks;
    private static void check(boolean value) { checks++; if (!value) throw new AssertionError("Gate regression #" + checks); }
    public static void main(String[] args) {
        for (int flags = 0; flags < 256; flags++) for (int gluttony = 0; gluttony < 3; gluttony++) {
            boolean lust = (flags & 1) != 0, cage = (flags & 2) != 0, ench = (flags & 4) != 0;
            boolean greedy = (flags & 8) != 0, emerald = (flags & 16) != 0;
            boolean cross = (flags & 32) != 0, wands = (flags & 64) != 0, loaded = (flags & 128) != 0;
            var g = new ContentGates(lust, cage, ench, greedy, emerald, gluttony, cross, wands);
            check(g.registerCage() && g.registerGluttonyItems());
            check(g.registerMobCrystal() == cage && g.registerCrystalTab() == cage);
            check(g.registerCollar() != lust && g.registerLuxuria() != lust);
            check(g.registerEnchantments() == ench);
            check(g.greedyAvailable() == (ench && greedy));
            check(g.gluttonyAvailable() == (gluttony == 0 || gluttony == 2));
            check(g.hardcoreGluttony() == (gluttony == 2));
            check(g.cageResearchAvailable() == cage);
            check(g.shardFourName().equals(lust ? "despair" : "lust"));
            check(g.shardFourAspect().equals(lust ? "vacuos" : "forbidden:luxuria"));
            check(g.conversionWands(loaded, true) == (cross && wands && loaded));
            check(!g.conversionWands(loaded, false));
        }
        System.out.println("FORBIDDEN_FOUNDATION_RULES_PASS checks=" + checks + " profiles=768");
    }
}

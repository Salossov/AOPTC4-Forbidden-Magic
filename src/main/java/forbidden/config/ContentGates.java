package forbidden.config;

/** Pure release gates. Registration, recipe and research availability are distinct. */
public record ContentGates(boolean noLust, boolean wrathCage, boolean enchanting,
                           boolean greedy, boolean emeraldTransmutation, int gluttony,
                           boolean crossMod, boolean crossWand) {
    public ContentGates {
        if (gluttony < 0 || gluttony > 2) gluttony = 0;
    }
    public boolean registerCage() { return true; }
    public boolean registerGluttonyItems() { return true; }
    public boolean registerMobCrystal() { return wrathCage; }
    public boolean registerCrystalTab() { return wrathCage; }
    public boolean registerCollar() { return !noLust; }
    public boolean registerLuxuria() { return !noLust; }
    public boolean registerEnchantments() { return enchanting; }
    // Capitalist disables its recipe/research/effect, not its registry entry.
    public boolean greedyAvailable() { return enchanting && greedy; }
    public boolean gluttonyAvailable() { return gluttony != 1; }
    public boolean hardcoreGluttony() { return gluttony == 2; }
    public boolean cageResearchAvailable() { return wrathCage; }
    public boolean conversionWands(boolean integrationPresent, boolean integrationEnabled) {
        return crossMod && crossWand && integrationPresent && integrationEnabled;
    }
    public String shardFourName() { return noLust ? "despair" : "lust"; }
    public String shardFourAspect() { return noLust ? "vacuos" : "forbidden:luxuria"; }
}

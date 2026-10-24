package forbidden.content;

import forbidden.component.ForbiddenComponents;
import forbidden.config.ForbiddenConfig;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.EssentiaCapabilities;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.aspects.IEssentiaTransport;

/**
 * Wrath Cage block entity.
 * Handles mob crystal binding, sided essentia transport (suction 256),
 * 3-way mode switching (special, wrath, sloth), F01 buffer clamping (max 64),
 * and spawner execution.
 */
public final class WrathCageEntity extends BlockEntity implements IAspectContainer, IEssentiaTransport {
    private final WrathSpawnerLogic spawnLogic;
    private ResourceLocation mob;
    private int wrath;
    private int sloth;
    private int special;
    private byte mode;
    private Aspect aspect = Aspect.getAspect("lucrum");

    public WrathCageEntity(BlockPos pos, BlockState state) {
        super(ForbiddenContent.CAGE_ENTITY.get(), pos, state);
        this.spawnLogic = new WrathSpawnerLogic(this);
    }

    public WrathSpawnerLogic getSpawnerLogic() {
        return spawnLogic;
    }

    public ResourceLocation mob() {
        return mob;
    }

    public byte mode() {
        return mode;
    }

    public int getWrath() {
        return wrath;
    }

    public void setWrath(int wrath) {
        this.wrath = Math.clamp(wrath, 0, 64);
    }

    public int getSloth() {
        return sloth;
    }

    public void setSloth(int sloth) {
        this.sloth = Math.clamp(sloth, 0, 64);
    }

    public int getSpecial() {
        return special;
    }

    public void setSpecial(int special) {
        this.special = Math.clamp(special, 0, 64);
    }

    public Aspect getAspect() {
        if (aspect == null) {
            aspect = spawnLogic.getAspect();
        }
        return aspect;
    }

    public ItemStack crystal() {
        if (mob == null || ForbiddenContent.MOB_CRYSTAL == null) return ItemStack.EMPTY;
        var stack = new ItemStack(ForbiddenContent.MOB_CRYSTAL.get());
        stack.set(ForbiddenComponents.MOB.get(), mob);
        return stack;
    }

    /**
     * Set mob and reset mob-specific buffers (special essentia and spawner fuel)
     * without leaks.
     */
    public void setMob(ResourceLocation replacement) {
        this.mob = replacement;
        this.spawnLogic.setMobID(replacement);
        this.spawnLogic.mobIsSet(replacement != null);
        this.special = 0;
        this.spawnLogic.setFuel(0);
        this.spawnLogic.setSlothful(false);
        this.aspect = this.spawnLogic.getAspect();
    }

    /** Caller replaces the hand exactly once with the returned old crystal. */
    public ItemStack exchange(ResourceLocation replacement) {
        requireServer();
        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(replacement)) {
            throw new IllegalArgumentException("Unknown mob " + replacement);
        }
        var previous = crystal();
        setMob(replacement);
        sync();
        return previous;
    }

    public void checkAspect() {
        if (this.aspect != this.spawnLogic.getAspect()) {
            this.special = 0;
        }
        this.aspect = this.spawnLogic.getAspect();
    }

    public void cycleMode() {
        requireServer();
        mode = (byte) ((mode + 1) % 3);
        sync();
    }

    public void serverTick() {
        if (level == null || level.isClientSide) return;
        if (!level.hasNeighborSignal(worldPosition)) {
            spawnLogic.updateSpawner();
        }
        int wrathCost = ForbiddenConfig.WRATH_COST.get();
        if (wrathCost > 0 && spawnLogic.isMobSet() && (special < 64 || wrath < 64 || sloth < 64)) {
            drawEssentia();
        }
    }

    public void clientTick() {
        if (level != null && level.isClientSide) {
            spawnLogic.updateSpawner();
        }
    }

    public void drawEssentia() {
        if (level == null || level.isClientSide) return;
        int wrathCost = ForbiddenConfig.WRATH_COST.get();
        var ira = Aspect.getAspect("forbidden:ira");
        var desidia = Aspect.getAspect("forbidden:desidia");

        for (Direction current : Direction.values()) {
            BlockPos adjacent = worldPosition.relative(current);
            if (!level.hasChunkAt(adjacent)) continue;

            IEssentiaTransport ic = level.getCapability(EssentiaCapabilities.TRANSPORT, adjacent, current.getOpposite());
            if (ic == null && level.getBlockEntity(adjacent) instanceof IEssentiaTransport transport) {
                ic = transport;
            }

            if (ic != null && ic.canOutputTo(current.getOpposite())) {
                Direction opp = current.getOpposite();

                // 1. Special mob aspect
                if (this.aspect != null && this.special < 64
                        && ic.getEssentiaType(opp) == this.aspect
                        && ic.getEssentiaAmount(opp) > 0
                        && ic.takeEssentia(this.aspect, 1, opp) == 1) {
                    this.special++;
                    sync();
                    return;
                }

                // 2. Wrath aspect (ira)
                if (ira != null && this.wrath < 64
                        && this.special < wrathCost
                        && ic.getEssentiaType(opp) == ira
                        && ic.getEssentiaAmount(opp) > 0
                        && ic.takeEssentia(ira, 1, opp) == 1) {
                    this.wrath++;
                    sync();
                    return;
                }

                // 3. Sloth aspect (desidia)
                if (desidia != null && this.sloth < 64
                        && this.special < wrathCost
                        && this.wrath < wrathCost
                        && ic.getEssentiaType(opp) == desidia
                        && ic.getEssentiaAmount(opp) > 0
                        && ic.takeEssentia(desidia, 1, opp) == 1) {
                    this.sloth++;
                    sync();
                    return;
                }
            }
        }
    }

    // --- IEssentiaTransport ---

    @Override
    public boolean isConnectable(Direction face) {
        return ForbiddenConfig.WRATH_COST.get() > 0;
    }

    @Override
    public boolean canInputFrom(Direction face) {
        return true;
    }

    @Override
    public boolean canOutputTo(Direction face) {
        return false;
    }

    @Override
    public void setSuction(Aspect aspect, int amount) {
    }

    @Override
    public Aspect getSuctionType(Direction face) {
        if (ForbiddenConfig.WRATH_COST.get() <= 0) return null;
        if (this.mode == 0) return this.aspect;
        if (this.mode == 1) return Aspect.getAspect("forbidden:ira");
        return Aspect.getAspect("forbidden:desidia");
    }

    @Override
    public int getSuctionAmount(Direction face) {
        return ForbiddenConfig.WRATH_COST.get() > 0 ? 256 : 0;
    }

    @Override
    public int takeEssentia(Aspect aspect, int amount, Direction face) {
        return 0;
    }

    /**
     * F01: Clamp to 64 without loss.
     * Accepts only up to free capacity and returns the exact accepted amount.
     */
    @Override
    public int addEssentia(Aspect inp, int amount, Direction face) {
        if (amount <= 0 || ForbiddenConfig.WRATH_COST.get() <= 0) return 0;
        int accepted = 0;
        var ira = Aspect.getAspect("forbidden:ira");
        var desidia = Aspect.getAspect("forbidden:desidia");

        if (this.aspect != null && inp == this.aspect) {
            accepted = Math.max(0, Math.min(amount, 64 - this.special));
            this.special += accepted;
        } else if (ira != null && inp == ira) {
            accepted = Math.max(0, Math.min(amount, 64 - this.wrath));
            this.wrath += accepted;
        } else if (desidia != null && inp == desidia) {
            accepted = Math.max(0, Math.min(amount, 64 - this.sloth));
            this.sloth += accepted;
        }

        if (accepted > 0) sync();
        return accepted;
    }

    @Override
    public Aspect getEssentiaType(Direction face) {
        return getSuctionType(face);
    }

    @Override
    public int getEssentiaAmount(Direction face) {
        if (this.mode == 0) return this.special;
        if (this.mode == 1) return this.wrath;
        return this.sloth;
    }

    @Override
    public int getMinimumSuction() {
        return 9000;
    }

    @Override
    public boolean renderExtendedTube() {
        return false;
    }

    // --- IAspectContainer ---

    @Override
    public AspectList getAspects() {
        AspectList list = new AspectList();
        if (ForbiddenConfig.WRATH_COST.get() > 0 && this.spawnLogic.isMobSet()) {
            if (this.mode == 0) {
                if (this.aspect != null) list.add(this.aspect, this.special);
            } else if (this.mode == 1) {
                var ira = Aspect.getAspect("forbidden:ira");
                if (ira != null) list.add(ira, this.wrath);
            } else {
                var desidia = Aspect.getAspect("forbidden:desidia");
                if (desidia != null) list.add(desidia, this.sloth);
            }
        }
        return list;
    }

    @Override
    public void setAspects(AspectList aspects) {
    }

    @Override
    public boolean doesContainerAccept(Aspect tag) {
        if (ForbiddenConfig.WRATH_COST.get() <= 0) return false;
        var ira = Aspect.getAspect("forbidden:ira");
        var desidia = Aspect.getAspect("forbidden:desidia");
        return (this.aspect != null && tag == this.aspect)
                || (ira != null && tag == ira)
                || (desidia != null && tag == desidia);
    }

    /**
     * F01: Clamp to 64 without loss.
     * Accepts only up to free capacity and returns the unaccepted remainder.
     */
    @Override
    public int addToContainer(Aspect tag, int amount) {
        if (amount <= 0) return 0;
        if (ForbiddenConfig.WRATH_COST.get() <= 0) return amount;
        int accepted = 0;
        var ira = Aspect.getAspect("forbidden:ira");
        var desidia = Aspect.getAspect("forbidden:desidia");

        if (this.aspect != null && tag == this.aspect) {
            accepted = Math.max(0, Math.min(amount, 64 - this.special));
            this.special += accepted;
        } else if (ira != null && tag == ira) {
            accepted = Math.max(0, Math.min(amount, 64 - this.wrath));
            this.wrath += accepted;
        } else if (desidia != null && tag == desidia) {
            accepted = Math.max(0, Math.min(amount, 64 - this.sloth));
            this.sloth += accepted;
        } else {
            return amount;
        }

        if (accepted > 0) sync();
        return amount - accepted;
    }

    @Override
    public boolean takeFromContainer(Aspect tag, int amount) {
        return false;
    }

    @Override
    public boolean doesContainerContainAmount(Aspect tag, int amount) {
        var ira = Aspect.getAspect("forbidden:ira");
        var desidia = Aspect.getAspect("forbidden:desidia");
        if (ira != null && tag == ira) return this.wrath >= amount;
        if (desidia != null && tag == desidia) return this.sloth >= amount;
        if (this.aspect != null && tag == this.aspect) return this.special >= amount;
        return false;
    }

    @Override
    public int containerContains(Aspect tag) {
        var ira = Aspect.getAspect("forbidden:ira");
        var desidia = Aspect.getAspect("forbidden:desidia");
        if (ira != null && tag == ira) return this.wrath;
        if (desidia != null && tag == desidia) return this.sloth;
        if (this.aspect != null && tag == this.aspect) return this.special;
        return 0;
    }

    public void requireServer() {
        if (level == null || level.isClientSide || level.getServer() == null || !level.getServer().isSameThread()) {
            throw new IllegalStateException("Server thread required");
        }
    }

    public void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);
        spawnLogic.writeToNBT(tag);
        tag.putShort("Wrath", (short) Math.clamp(wrath, 0, 64));
        tag.putShort("Sloth", (short) Math.clamp(sloth, 0, 64));
        tag.putShort("Special", (short) Math.clamp(special, 0, 64));
        tag.putByte("Mode", mode);
        if (!tag.contains("Delay")) {
            tag.putShort("Delay", (short) 20);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
        spawnLogic.readFromNBT(tag);
        wrath = Math.clamp((int) tag.getShort("Wrath"), 0, 64);
        sloth = Math.clamp((int) tag.getShort("Sloth"), 0, 64);
        special = Math.clamp((int) tag.getShort("Special"), 0, 64);
        mode = tag.getByte("Mode");
        mob = spawnLogic.isMobSet() ? spawnLogic.getMobID() : null;
        aspect = spawnLogic.getAspect();
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider lookup) {
        return saveWithoutMetadata(lookup);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}

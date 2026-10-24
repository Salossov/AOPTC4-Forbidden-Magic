package forbidden.content;

import forbidden.component.ForbiddenComponents;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Persistence and crystal exchange only. P03 adds essentia and spawning. */
public final class WrathCageEntity extends BlockEntity {
    private ResourceLocation mob;
    private CompoundTag spawning = new CompoundTag();
    private int wrath, sloth, special;
    private byte mode;
    public WrathCageEntity(BlockPos pos,BlockState state){super(ForbiddenContent.CAGE_ENTITY.get(),pos,state);}
    public ResourceLocation mob(){return mob;}
    public byte mode(){return mode;}
    public ItemStack crystal(){
        if(mob==null || ForbiddenContent.MOB_CRYSTAL==null)return ItemStack.EMPTY;
        var stack=new ItemStack(ForbiddenContent.MOB_CRYSTAL.get());stack.set(ForbiddenComponents.MOB.get(),mob);return stack;
    }
    /** Caller replaces the hand exactly once with the returned old crystal. */
    public ItemStack exchange(ResourceLocation replacement){
        requireServer();
        if(!BuiltInRegistries.ENTITY_TYPE.containsKey(replacement))throw new IllegalArgumentException("Unknown mob "+replacement);
        var previous=crystal();mob=replacement;sync();return previous;
    }
    public void cycleMode(){requireServer();mode=(byte)((mode+1)%3);sync();}
    private void requireServer(){if(level==null||level.isClientSide||level.getServer()==null||!level.getServer().isSameThread())throw new IllegalStateException("Server thread required");}
    private void sync(){setChanged();level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider lookup){
        super.saveAdditional(tag,lookup);tag.merge(spawning.copy());tag.putBoolean("MobSet",mob!=null);tag.putString("EntityId",mob==null?"minecraft:pig":mob.toString());
        tag.putShort("Wrath",(short)wrath);tag.putShort("Sloth",(short)sloth);tag.putShort("Special",(short)special);tag.putByte("Mode",mode);
        if(!tag.contains("Delay"))tag.putShort("Delay",(short)20);
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider lookup){
        super.loadAdditional(tag,lookup);spawning=new CompoundTag();
        for(var key:new String[]{"Slothful","Fuel","Delay","MaxNearbyEntities","SpawnRange"})if(tag.contains(key))spawning.put(key,tag.get(key).copy());
        mob=tag.getBoolean("MobSet")?ResourceLocation.tryParse(tag.getString("EntityId")):null;
        wrath=tag.getShort("Wrath");sloth=tag.getShort("Sloth");special=tag.getShort("Special");mode=tag.getByte("Mode");
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider lookup){return saveWithoutMetadata(lookup);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}

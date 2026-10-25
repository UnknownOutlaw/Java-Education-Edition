package edu.chemistry.block;

import edu.chemistry.registry.EduBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** The text and lock of a chalkboard; lives in its bottom-left cell. */
public class ChalkboardBlockEntity extends BlockEntity {
    public static final int MAX_LENGTH = 1000;
    private String text = "";
    private boolean locked;
    /** placed by a World Builder: only World Builders can break it */
    private boolean builderPlaced;

    public ChalkboardBlockEntity(BlockPos pos, BlockState state) {
        super(EduBlockEntities.CHALKBOARD, pos, state);
    }

    public String getText() {
        return this.text;
    }

    public boolean isLocked() {
        return this.locked;
    }

    public boolean isBuilderPlaced() {
        return this.builderPlaced;
    }

    public void setBuilderPlaced(boolean builderPlaced) {
        this.builderPlaced = builderPlaced;
        this.setChanged();
    }

    public ChalkboardBlock.Size size() {
        return this.getBlockState().getBlock() instanceof ChalkboardBlock b ? b.size : ChalkboardBlock.Size.SLATE;
    }

    /** Server side: store new text and lock, then send them to everyone watching. */
    public void update(String text, boolean locked) {
        this.text = text.length() > MAX_LENGTH ? text.substring(0, MAX_LENGTH) : text;
        this.locked = locked;
        this.setChanged();
        if (this.level != null) this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.text = input.getStringOr("text", "");
        this.locked = input.getBooleanOr("locked", false);
        // chalkboards saved before this was tracked don't know who placed them, so they count as a World Builder's
        this.builderPlaced = input.getBooleanOr("builder_placed", true);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("text", this.text);
        output.putBoolean("locked", this.locked);
        output.putBoolean("builder_placed", this.builderPlaced);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveCustomOnly(registries);
    }
}

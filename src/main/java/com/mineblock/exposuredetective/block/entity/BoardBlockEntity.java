package com.mineblock.exposuredetective.block.entity;

import com.mineblock.exposuredetective.block.BoardBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import com.mineblock.exposuredetective.init.ExposuredetectiveModBlockEntities;

public class BoardBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer {

    private static final String ROTATION_TAG = "Rotations";
    private static final String CONNECTIONS_TAG = "Connections";
    private static final int SLOT_COUNT = 9;
    private static final int MAX_CONNECTIONS_PER_SLOT = 12;

    private NonNullList<ItemStack> stacks = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final float[] rotations = new float[SLOT_COUNT];
    private final List<PhotoConnection>[] connections = createConnectionLists();

    public BoardBlockEntity(BlockPos position, BlockState state) {
        super(ExposuredetectiveModBlockEntities.BOARD.get(), position, state);
    }

    // region Persistence

    @Override
    public void loadAdditional(CompoundTag compound, HolderLookup.Provider lookupProvider) {
        super.loadAdditional(compound, lookupProvider);

        this.stacks = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);

        if (!this.tryLoadLootTable(compound)) {
            ContainerHelper.loadAllItems(compound, this.stacks, lookupProvider);
        }

        loadRotations(compound);
        loadConnections(compound);
    }

    @Override
    public void saveAdditional(CompoundTag compound, HolderLookup.Provider lookupProvider) {
        super.saveAdditional(compound, lookupProvider);

        if (!this.trySaveLootTable(compound)) {
            ContainerHelper.saveAllItems(compound, this.stacks, lookupProvider);
        }

        saveRotations(compound);
        saveConnections(compound);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider lookupProvider) {
        return this.saveWithFullMetadata(lookupProvider);
    }

    // endregion

    // region Container

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.stacks) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Component getDefaultName() {
        return Component.translatable("block.exposuredetective.board");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return ChestMenu.threeRows(id, inventory);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.exposuredetective.board");
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.stacks;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> stacks) {
        this.stacks = stacks;
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        // Разрешаем только фотографии. Остальное отсекаем.
        return BoardBlock.isPhotograph(stack);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return IntStream.range(0, getContainerSize()).toArray();
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction direction) {
        return canPlaceItem(index, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return true;
    }

    // endregion

    // region Rotations

    public float getRotation(int index) {
        return isValidSlot(index) ? rotations[index] : 0f;
    }

    public void setRotation(int index, float rotation) {
        if (isValidSlot(index)) {
            rotations[index] = rotation;
            setChanged();
        }
    }

    // endregion

    // region Connections

    public List<PhotoConnection> getConnections(int slot) {
        if (!isValidSlot(slot)) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(connections[slot]);
    }

    public boolean hasConnections(int slot) {
        return isValidSlot(slot) && !connections[slot].isEmpty();
    }

    public boolean addConnection(int slot, BlockPos otherPos, int otherSlot) {
        if (!isValidSlot(slot) || !isValidSlot(otherSlot)) {
            return false;
        }
        if (connections[slot].size() >= MAX_CONNECTIONS_PER_SLOT) {
            return false;
        }

        PhotoConnection connection = new PhotoConnection(otherPos.immutable(), otherSlot);
        if (connections[slot].contains(connection)) {
            return false;
        }

        connections[slot].add(connection);
        setChanged();
        return true;
    }

    public boolean removeConnection(int slot, BlockPos otherPos, int otherSlot) {
        if (!isValidSlot(slot)) {
            return false;
        }

        boolean removed = connections[slot].remove(new PhotoConnection(otherPos.immutable(), otherSlot));
        if (removed) {
            setChanged();
        }
        return removed;
    }

    /**
     * Очищает все связи указанного слота и снимает обратные связи на других досках.
     * @return количество удалённых связей
     */
    public int clearConnections(Level level, int slot) {
        if (!isValidSlot(slot) || level == null || level.isClientSide) {
            return 0;
        }

        List<PhotoConnection> toRemove = List.copyOf(connections[slot]);
        int removed = toRemove.size();

        for (PhotoConnection connection : toRemove) {
            removeConnectionFromOther(level, connection.pos(), connection.slot(), slot);
        }

        connections[slot].clear();

        if (removed > 0) {
            setChanged();
        }
        return removed;
    }

    public int removeAllConnections(Level level) {
        if (level == null || level.isClientSide) {
            return 0;
        }

        int total = 0;
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            total += clearConnections(level, slot);
        }
        return total;
    }

    // endregion

    // region Private helpers

    private void loadRotations(CompoundTag tag) {
        Arrays.fill(rotations, 0f);
        ListTag list = tag.getList(ROTATION_TAG, Tag.TAG_FLOAT);
        int size = Math.min(rotations.length, list.size());
        for (int i = 0; i < size; i++) {
            rotations[i] = list.getFloat(i);
        }
    }

    private void saveRotations(CompoundTag tag) {
        ListTag list = new ListTag();
        for (float rotation : rotations) {
            list.add(FloatTag.valueOf(rotation));
        }
        tag.put(ROTATION_TAG, list);
    }

    private void loadConnections(CompoundTag tag) {
        for (List<PhotoConnection> list : connections) {
            list.clear();
        }

        ListTag slotsTag = tag.getList(CONNECTIONS_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < slotsTag.size(); i++) {
            CompoundTag slotTag = slotsTag.getCompound(i);
            int slot = slotTag.getInt("Slot");
            if (!isValidSlot(slot)) {
                continue;
            }

            ListTag linkList = slotTag.getList("Links", Tag.TAG_COMPOUND);
            for (int j = 0; j < linkList.size(); j++) {
                CompoundTag entry = linkList.getCompound(j);
                int otherSlot = entry.getInt("Slot");
                if (!isValidSlot(otherSlot)) {
                    continue;
                }

                BlockPos pos = new BlockPos(
                        entry.getInt("X"),
                        entry.getInt("Y"),
                        entry.getInt("Z")
                );
                connections[slot].add(new PhotoConnection(pos, otherSlot));
            }
        }
    }

    private void saveConnections(CompoundTag tag) {
        ListTag slotsTag = new ListTag();

        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            if (connections[slot].isEmpty()) {
                continue; // не пишем пустые слоты
            }

            CompoundTag slotTag = new CompoundTag();
            slotTag.putInt("Slot", slot);

            ListTag linkList = new ListTag();
            for (PhotoConnection connection : connections[slot]) {
                CompoundTag entry = new CompoundTag();
                entry.putInt("X", connection.pos().getX());
                entry.putInt("Y", connection.pos().getY());
                entry.putInt("Z", connection.pos().getZ());
                entry.putInt("Slot", connection.slot());
                linkList.add(entry);
            }
            slotTag.put("Links", linkList);
            slotsTag.add(slotTag);
        }

        tag.put(CONNECTIONS_TAG, slotsTag);
    }

    private boolean isValidSlot(int slot) {
        return slot >= 0 && slot < SLOT_COUNT;
    }

    @SuppressWarnings("unchecked")
    private static List<PhotoConnection>[] createConnectionLists() {
        List<PhotoConnection>[] lists = new List[SLOT_COUNT];
        for (int i = 0; i < SLOT_COUNT; i++) {
            lists[i] = new ArrayList<>(4);
        }
        return lists;
    }

    private void removeConnectionFromOther(Level level, BlockPos otherPos, int otherSlot, int thisSlot) {
        if (level.getBlockEntity(otherPos) instanceof BoardBlockEntity other) {
            if (other.removeConnection(otherSlot, this.getBlockPos(), thisSlot)) {
                BlockState otherState = level.getBlockState(otherPos);
                level.sendBlockUpdated(otherPos, otherState, otherState, Block.UPDATE_CLIENTS);
            }
        }
    }

    // endregion

    public record PhotoConnection(BlockPos pos, int slot) {}
}
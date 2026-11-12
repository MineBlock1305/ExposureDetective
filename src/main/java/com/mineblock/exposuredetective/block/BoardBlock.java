package com.mineblock.exposuredetective.block;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

import com.mineblock.exposuredetective.block.entity.BoardBlockEntity;
import com.mineblock.exposuredetective.init.ExposuredetectiveModItems;
import com.mineblock.exposuredetective.util.PhotographItems;
import com.mineblock.exposuredetective.item.ColoredStringItem;


public class BoardBlock extends Block implements EntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty BORDER_UP = BooleanProperty.create("border_up");
    public static final BooleanProperty BORDER_DOWN = BooleanProperty.create("border_down");
    public static final BooleanProperty BORDER_LEFT = BooleanProperty.create("border_left");
    public static final BooleanProperty BORDER_RIGHT = BooleanProperty.create("border_right");
    public static final BooleanProperty CORNER_LEFT_UP = BooleanProperty.create("corner_left_up");
    public static final BooleanProperty CORNER_LEFT_DOWN = BooleanProperty.create("corner_left_down");
    public static final BooleanProperty CORNER_RIGHT_UP = BooleanProperty.create("corner_right_up");
    public static final BooleanProperty CORNER_RIGHT_DOWN = BooleanProperty.create("corner_right_down");

    private static final ResourceLocation PHOTOGRAPH_ID = ResourceLocation.fromNamespaceAndPath("exposure", "photograph");
    private static final int GRID_SIZE = 3;
    private static final double MAX_LINK_DISTANCE_SQR = 9.0D; 

    
    private static final Map<UUID, Selection> RED_THREAD_SPOOL_SELECTIONS = new ConcurrentHashMap<>();

    @Nullable
    private static Item cachedPhotographItem;

    public BoardBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(net.minecraft.world.level.material.MapColor.WOOD)
                .sound(SoundType.WOOD)
                .strength(1f, 10f)
                .noOcclusion()
                .isRedstoneConductor((bs, br, bp) -> false)
                .instrument(NoteBlockInstrument.BASS));

        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(BORDER_UP, true)
                .setValue(BORDER_DOWN, true)
                .setValue(BORDER_LEFT, true)
                .setValue(BORDER_RIGHT, true)
                .setValue(CORNER_LEFT_UP, true)
                .setValue(CORNER_LEFT_DOWN, true)
                .setValue(CORNER_RIGHT_UP, true)
                .setValue(CORNER_RIGHT_DOWN, true));
    }

    

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
        return true;
    }

    @Override
    public int getLightBlock(BlockState state, BlockGetter worldIn, BlockPos pos) {
        return 0;
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            default -> box(0, 0, 0, 16, 16, 1);
            case NORTH -> box(0, 0, 15, 16, 16, 16);
            case EAST -> box(0, 0, 0, 1, 16, 16);
            case WEST -> box(15, 0, 0, 16, 16, 16);
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, BORDER_UP, BORDER_DOWN, BORDER_LEFT, BORDER_RIGHT,
                CORNER_LEFT_UP, CORNER_LEFT_DOWN, CORNER_RIGHT_UP, CORNER_RIGHT_DOWN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getClickedFace().getAxis() == Direction.Axis.Y
                ? Direction.NORTH
                : context.getClickedFace();
        BlockState state = defaultBlockState().setValue(FACING, facing);
        return withBorders(context.getLevel(), context.getClickedPos(), state);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return withBorders(level, pos, state);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide) {
            refreshNeighbors(level, pos, state);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
                                   BlockPos fromPos, boolean isMoving) {
        if (!level.isClientSide) {
            BlockState updated = withBorders(level, pos, state);
            if (updated != state) {
                level.setBlock(pos, updated, Block.UPDATE_CLIENTS);
            }
            refreshNeighbors(level, pos, updated);
        }
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
    }

    private void refreshNeighbors(Level level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(FACING);
        Direction left = getLeftDirection(facing);
        Direction right = getRightDirection(facing);
        
        BlockPos[] neighbors = {
                pos.above(), pos.below(), pos.relative(left), pos.relative(right),
                pos.above().relative(left), pos.above().relative(right),
                pos.below().relative(left), pos.below().relative(right)
        };
        for (BlockPos neighborPos : neighbors) {
            BlockState neighborState = level.getBlockState(neighborPos);
            if (neighborState.getBlock() == this) {
                BlockState updatedNeighbor = withBorders(level, neighborPos, neighborState);
                if (updatedNeighbor != neighborState) {
                    level.setBlock(neighborPos, updatedNeighbor, Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    public static BlockState withBorders(LevelAccessor level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof BoardBlock)) {
            return state;
        }

        Direction facing = state.getValue(FACING);
        Direction left = getLeftDirection(facing);
        Direction right = getRightDirection(facing);

        boolean hasUp = hasConnectedBoard(level, pos.above(), facing);
        boolean hasDown = hasConnectedBoard(level, pos.below(), facing);
        boolean hasLeft = hasConnectedBoard(level, pos.relative(left), facing);
        boolean hasRight = hasConnectedBoard(level, pos.relative(right), facing);
        boolean hasLeftUp = hasConnectedBoard(level, pos.above().relative(left), facing);
        boolean hasLeftDown = hasConnectedBoard(level, pos.below().relative(left), facing);
        boolean hasRightUp = hasConnectedBoard(level, pos.above().relative(right), facing);
        boolean hasRightDown = hasConnectedBoard(level, pos.below().relative(right), facing);

        return state
                .setValue(BORDER_UP, !hasUp)
                .setValue(BORDER_DOWN, !hasDown)
                .setValue(BORDER_LEFT, !hasLeft)
                .setValue(BORDER_RIGHT, !hasRight)
                .setValue(CORNER_LEFT_UP, !hasLeft && !hasUp && !hasLeftUp)
                .setValue(CORNER_LEFT_DOWN, !hasLeft && !hasDown && !hasLeftDown)
                .setValue(CORNER_RIGHT_UP, !hasRight && !hasUp && !hasRightUp)
                .setValue(CORNER_RIGHT_DOWN, !hasRight && !hasDown && !hasRightDown);
    }

    private static boolean hasConnectedBoard(LevelAccessor level, BlockPos neighborPos, Direction facing) {
        BlockState neighbor = level.getBlockState(neighborPos);
        return neighbor.getBlock() instanceof BoardBlock && neighbor.getValue(FACING) == facing;
    }

    private static Direction getLeftDirection(Direction facing) {
        return switch (facing) {
            case NORTH -> Direction.WEST;
            case SOUTH -> Direction.EAST;
            case EAST -> Direction.NORTH;
            case WEST -> Direction.SOUTH;
            default -> Direction.WEST;
        };
    }

    private static Direction getRightDirection(Direction facing) {
        return switch (facing) {
            case NORTH -> Direction.EAST;
            case SOUTH -> Direction.WEST;
            case EAST -> Direction.SOUTH;
            case WEST -> Direction.NORTH;
            default -> Direction.EAST;
        };
    }

    private static Direction[] getPlaneDirections(Direction facing) {
        return new Direction[]{Direction.UP, Direction.DOWN, getLeftDirection(facing), getRightDirection(facing)};
    }

    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    public BlockState mirror(BlockState state, Mirror mirrorIn) {
        return state.rotate(mirrorIn.getRotation(state.getValue(FACING)));
    }

    

    

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        BoardAction action = handleInteraction(state, level, pos, player, hand, stack, hit);
        if (action == BoardAction.NONE) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        BoardAction action = handleInteraction(state, level, pos, player, null, ItemStack.EMPTY, hit);
        if (action == BoardAction.NONE) {
            return InteractionResult.PASS;
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static BoardAction handleInteraction(BlockState state, Level level, BlockPos pos, Player player,
                                                 @Nullable InteractionHand hand, ItemStack held, BlockHitResult hit) {
        BlockEntity entity = level.getBlockEntity(pos);
        if (!(entity instanceof BoardBlockEntity board)) {
            return BoardAction.NONE;
        }

        int slot = getSlotFromHit(state, hit);
        if (slot < 0 || slot >= board.getContainerSize()) {
            return BoardAction.NONE;
        }

        boolean holdingPhotograph = isPhotograph(held);
        boolean holdingString = isString(held);
        ItemStack stored = board.getItem(slot);

        
        if (holdingString) {
            return handleRedStringInteraction(state, level, pos, player, hand, board, slot);
        }

        
        if (holdingPhotograph && stored.isEmpty()) {
            if (!level.isClientSide) {
                ItemStack placed = held.copyWithCount(1);
                board.setItem(slot, placed);
                board.setRotation(slot, randomRotation(level));
                board.setChanged();
                sync(level, pos, state);

                if (hand != null && !player.getAbilities().instabuild) {
                    held.shrink(1);
                }
            }
            return BoardAction.PLACED;
        }

        
        if (!stored.isEmpty()) {
            boolean emptyHand = hand == null || held.isEmpty();

            
            if (emptyHand && player.isShiftKeyDown() && board.hasConnections(slot)) {
                if (!level.isClientSide) {
                    dropStrings(level, pos, board.clearConnections(level, slot));
                    sync(level, pos, state);
                }
                return BoardAction.STRING_REMOVED;
            }

            
            if (canRemovePhoto(player, hand, held, board, slot)) {
                if (!level.isClientSide) {
                    ItemStack extracted = stored.copy();
                    if (!player.addItem(extracted)) {
                        player.drop(extracted, false);
                    }
                    board.setItem(slot, ItemStack.EMPTY);
                    board.setRotation(slot, 0f);
                    board.setChanged();
                    sync(level, pos, state);
                }
                return BoardAction.REMOVED;
            }

            
            
            if (level.isClientSide) {
                openPhotographScreenSafe(stored);
            }
            return BoardAction.VIEWED;
        }

        return BoardAction.NONE;
    }

    private static BoardAction handleRedStringInteraction(BlockState state, Level level, BlockPos pos,
                                                          Player player, @Nullable InteractionHand hand,
                                                          BoardBlockEntity board, int slot) {
        ItemStack stored = board.getItem(slot);
        if (stored.isEmpty()) {
            return BoardAction.NONE;
        }

        
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                dropStrings(level, pos, board.clearConnections(level, slot));
                sync(level, pos, state);
            }
            clearSelection(player);
            return BoardAction.STRING_REMOVED;
        }

        
        if (level.isClientSide) {
            return BoardAction.LINKING;
        }

        
        cleanStaleSelections(level);

        Selection selection = RED_THREAD_SPOOL_SELECTIONS.get(player.getUUID());

        if (selection == null) {
            RED_THREAD_SPOOL_SELECTIONS.put(player.getUUID(), new Selection(pos.immutable(), slot));
            return BoardAction.LINKING;
        }

        
        if (selection.pos().equals(pos) && selection.slot() == slot) {
            clearSelection(player);
            return BoardAction.LINKING;
        }

        BlockEntity otherEntity = level.getBlockEntity(selection.pos());
        if (!(otherEntity instanceof BoardBlockEntity other)) {
            clearSelection(player);
            return BoardAction.NONE;
        }

        
        if (board.getItem(slot).isEmpty() || other.getItem(selection.slot()).isEmpty()) {
            clearSelection(player);
            return BoardAction.NONE;
        }

        if (!canLinkBoards(state, pos, selection.pos(), other)) {
            clearSelection(player);
            return BoardAction.NONE;
        }

        int stringColor = getStringColor(player.getItemInHand(hand != null ? hand : InteractionHand.MAIN_HAND));
        boolean linked = board.addConnection(slot, selection.pos(), selection.slot(), stringColor);
        linked |= other.addConnection(selection.slot(), pos, slot, stringColor);

        if (linked) {
            consumeString(player, hand);
            sync(level, pos, state);

            BlockState otherState = level.getBlockState(selection.pos());
            sync(level, selection.pos(), otherState);
        }

        clearSelection(player);
        return linked ? BoardAction.LINKED : BoardAction.NONE;
    }

    private static boolean canLinkBoards(BlockState firstState, BlockPos firstPos,
                                         BlockPos secondPos, BoardBlockEntity second) {
        BlockState secondState = second.getBlockState();
        if (secondState == null || !(secondState.getBlock() instanceof BoardBlock)) {
            return false;
        }

        
        if (firstState.getValue(FACING) != secondState.getValue(FACING)) {
            return false;
        }

        
        return firstPos.distSqr(secondPos) <= MAX_LINK_DISTANCE_SQR;
    }

    

    

    public static boolean isPhotograph(ItemStack stack) {
        return PhotographItems.isPhotograph(stack);
    }

    private static boolean isString(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ColoredStringItem;
    }

    private static int getStringColor(ItemStack stack) {
        if (stack.getItem() instanceof ColoredStringItem colored) {
            return colored.getColorRgb();
        }
        return 0xFF5555;
    }

    @Nullable
    private static Item resolvePhotographItem() {
        if (cachedPhotographItem == null) {
            cachedPhotographItem = BuiltInRegistries.ITEM.getOptional(PHOTOGRAPH_ID).orElse(null);
        }
        return cachedPhotographItem;
    }

    private static int getSlotFromHit(BlockState state, BlockHitResult hit) {
        Vec3 location = hit.getLocation().subtract(
                hit.getBlockPos().getX(),
                hit.getBlockPos().getY(),
                hit.getBlockPos().getZ()
        );

        double horizontal = getHorizontalCoordinate(state.getValue(FACING), location);
        double vertical = 1.0 - clamp01(location.y);

        int column = Mth.clamp((int) (horizontal * GRID_SIZE), 0, GRID_SIZE - 1);
        int row = Mth.clamp((int) (vertical * GRID_SIZE), 0, GRID_SIZE - 1);

        return row * GRID_SIZE + column;
    }

    private static double getHorizontalCoordinate(Direction facing, Vec3 location) {
        return switch (facing) {
            case NORTH -> 1.0 - clamp01(location.x);
            case SOUTH -> clamp01(location.x);
            case EAST -> 1.0 - clamp01(location.z);
            case WEST -> clamp01(location.z);
            default -> clamp01(location.x);
        };
    }

    private static double clamp01(double value) {
        return Mth.clamp(value, 0.0, 0.999);
    }

    
    public static void clearSelection(Player player) {
        RED_THREAD_SPOOL_SELECTIONS.remove(player.getUUID());
    }

    
    private static void cleanStaleSelections(Level level) {
        RED_THREAD_SPOOL_SELECTIONS.entrySet().removeIf(entry -> {
            Selection sel = entry.getValue();
            return !(level.getBlockEntity(sel.pos()) instanceof BoardBlockEntity);
        });
    }

    private static void sync(Level level, BlockPos pos, BlockState state) {
        level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
    }

    private static float randomRotation(Level level) {
        return level.random.nextFloat() * 10.0f - 5.0f;
    }

    private static boolean canRemovePhoto(Player player, @Nullable InteractionHand hand,
                                          ItemStack held, BoardBlockEntity board, int slot) {
        boolean emptyHand = hand == null || held.isEmpty();
        return emptyHand && player.isShiftKeyDown()
                && !board.hasConnections(slot)
                && !board.getItem(slot).isEmpty();
    }

    private static void consumeString(Player player, @Nullable InteractionHand hand) {
        if (hand == null || player.getAbilities().instabuild) {
            return;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (!isString(stack)) {
            return;
        }
        
        stack.hurtAndBreak(1, player, net.minecraft.world.entity.LivingEntity.getSlotForHand(hand));
    }

    
    private static void dropStrings(Level level, BlockPos pos, java.util.List<Integer> colors) {
        
    }

    private static net.minecraft.world.item.Item stringItemFromColor(int rgb) {
        int c = rgb & 0x00FFFFFF;
        for (net.minecraft.world.item.Item item : ExposuredetectiveModItems.threadSpools()) {
            if (item instanceof com.mineblock.exposuredetective.item.ColoredStringItem colored
                    && colored.getColorRgb() == c) {
                return item;
            }
        }
        return ExposuredetectiveModItems.RED_THREAD_SPOOL.get();
    }

    
    private static void openPhotographScreenSafe(ItemStack stack) {
        try {
            Class<?> helper = Class.forName("com.mineblock.exposuredetective.client.BoardClientHelper");
            helper.getMethod("openPhotographScreen", ItemStack.class).invoke(null, stack);
        } catch (ReflectiveOperationException | LinkageError e) {
            
        }
    }

    

    

    @Override
    public MenuProvider getMenuProvider(BlockState state, Level worldIn, BlockPos pos) {
        BlockEntity tileEntity = worldIn.getBlockEntity(pos);
        return tileEntity instanceof MenuProvider menuProvider ? menuProvider : null;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BoardBlockEntity(pos, state);
    }

    @Override
    public boolean triggerEvent(BlockState state, Level world, BlockPos pos, int eventID, int eventParam) {
        super.triggerEvent(state, world, pos, eventID, eventParam);
        BlockEntity blockEntity = world.getBlockEntity(pos);
        return blockEntity != null && blockEntity.triggerEvent(eventID, eventParam);
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof BoardBlockEntity be) {
                dropStrings(world, pos, be.removeAllConnections(world));
                Containers.dropContents(world, pos, be);
                world.updateNeighbourForOutputSignal(pos, this);
            }
            super.onRemove(state, world, pos, newState, isMoving);
        }
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState blockState, Level world, BlockPos pos) {
        BlockEntity tileentity = world.getBlockEntity(pos);
        if (tileentity instanceof BoardBlockEntity be) {
            return AbstractContainerMenu.getRedstoneSignalFromContainer(be);
        }
        return 0;
    }

    

    private enum BoardAction {
        NONE,
        PLACED,
        REMOVED,
        VIEWED,
        LINKING,
        LINKED,
        STRING_REMOVED
    }

    private record Selection(BlockPos pos, int slot) {}
}

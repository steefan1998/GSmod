package nl.gsmod.block;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.transfer.energy.EnergyHandlerUtil;
import nl.gsmod.machine.MachineBlock;
import nl.gsmod.registry.ModBlockEntities;

public class EnergyCellBlock extends BaseEntityBlock {
    public static final MapCodec<EnergyCellBlock> CODEC = simpleCodec(EnergyCellBlock::new);

    public EnergyCellBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(MachineBlock.FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyCellBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level instanceof ServerLevel serverLevel
                ? createTickerHelper(type, ModBlockEntities.ENERGY_CELL.get(), (l, pos, s, cell) -> EnergyCellBlockEntity.tick(serverLevel, pos, s, cell))
                : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof EnergyCellBlockEntity cell) {
            player.sendOverlayMessage(Component.translatable("message.gsmod.energy",
                    String.format("%,d", cell.getEnergy().getEnergy()), String.format("%,d", cell.getEnergy().getCapacity())));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Front (output) faces away from the player, like a hopper pointing where you look.
        return defaultBlockState().setValue(MachineBlock.FACING, context.getHorizontalDirection());
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof EnergyCellBlockEntity cell
                ? EnergyHandlerUtil.getRedstoneSignalFromEnergyHandler(cell.getEnergy())
                : 0;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(MachineBlock.FACING, rotation.rotate(state.getValue(MachineBlock.FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(MachineBlock.FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MachineBlock.FACING);
    }
}

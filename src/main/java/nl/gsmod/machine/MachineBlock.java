package nl.gsmod.machine;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import nl.gsmod.registry.ModBlockEntities;

/** Block for all three machines; {@link MachineType} decides which block entity it gets. */
public class MachineBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public static final MapCodec<MachineBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            propertiesCodec(),
            MachineType.CODEC.fieldOf("machine").forGetter(MachineBlock::getMachineType)
    ).apply(i, MachineBlock::new));

    private final MachineType machineType;

    public MachineBlock(Properties properties, MachineType machineType) {
        super(properties.lightLevel(state -> state.getValue(LIT) ? 13 : 0));
        this.machineType = machineType;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }

    public MachineType getMachineType() {
        return machineType;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return switch (machineType) {
            case GENERATOR -> new GeneratorBlockEntity(pos, state);
            case CRUSHER -> new CrusherBlockEntity(pos, state);
            case ALLOY_SMELTER -> new AlloySmelterBlockEntity(pos, state);
        };
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return null;
        }
        BlockEntityType<? extends MachineBlockEntity> expected = switch (machineType) {
            case GENERATOR -> ModBlockEntities.COMBUSTION_GENERATOR.get();
            case CRUSHER -> ModBlockEntities.CRUSHER.get();
            case ALLOY_SMELTER -> ModBlockEntities.ALLOY_SMELTER.get();
        };
        return createTickerHelper(type, expected, (l, pos, s, machine) -> MachineBlockEntity.tick(serverLevel, pos, s, machine));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MenuProvider provider) {
            player.openMenu(provider);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack itemStack) {
        super.setPlacedBy(level, pos, state, by, itemStack);
        if (by instanceof Player player && level.getBlockEntity(pos) instanceof MachineBlockEntity machine) {
            machine.setOwner(player.getUUID());
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        Containers.updateNeighboursAfterDestroy(state, level, pos);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT) || random.nextDouble() >= 0.3) {
            return;
        }
        Direction facing = state.getValue(FACING);
        double x = pos.getX() + 0.5 + facing.getStepX() * 0.52 + (facing.getAxis() == Direction.Axis.Z ? random.nextDouble() * 0.6 - 0.3 : 0);
        double y = pos.getY() + random.nextDouble() * 6.0 / 16.0;
        double z = pos.getZ() + 0.5 + facing.getStepZ() * 0.52 + (facing.getAxis() == Direction.Axis.X ? random.nextDouble() * 0.6 - 0.3 : 0);
        level.addParticle(machineType == MachineType.CRUSHER ? ParticleTypes.CRIT : ParticleTypes.SMOKE, x, y, z, 0, 0, 0);
        if (machineType != MachineType.CRUSHER) {
            level.addParticle(ParticleTypes.FLAME, x, y, z, 0, 0, 0);
        }
    }
}

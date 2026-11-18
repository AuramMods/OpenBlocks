package art.arcane.openblocks.block;

import art.arcane.openblocks.block.entity.OBTankBlockEntity;
import art.arcane.openblocks.config.OBServerConfig;
import art.arcane.openblocks.item.OBTankItem;
import art.arcane.openblocks.registry.OBBlockEntities;
import art.arcane.openblocks.world.OBLiquidExperience;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.registries.ForgeRegistries;

public final class OBTankBlock extends BaseEntityBlock {

    public static final IntegerProperty LIGHT = BlockStateProperties.LEVEL;
    public OBTankBlock(final Properties properties) {
        super(properties.lightLevel(state -> state.getValue(LIGHT)));
        registerDefaultState(stateDefinition.any().setValue(LIGHT, 0));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIGHT);
    }

    @Override
    public RenderShape getRenderShape(final BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new OBTankBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state, final BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, OBBlockEntities.TANK.get(), OBTankBlockEntity::serverTick);
    }

    @Override
    public void neighborChanged(final BlockState state, final Level level, final BlockPos pos,
            final Block neighbor, final BlockPos neighborPos, final boolean moved) {
        if (level.getBlockEntity(pos) instanceof OBTankBlockEntity tank) tank.scheduleBalance();
    }

    @Override
    public void setPlacedBy(final Level level, final BlockPos pos, final BlockState state,
            @Nullable final LivingEntity placer, final ItemStack stack) {
        final FluidStack fluid = OBTankItem.contents(stack);
        if (!fluid.isEmpty() && level.getBlockEntity(pos) instanceof OBTankBlockEntity tank) tank.restoreContents(fluid);
    }

    @Override
    public ItemStack getCloneItemStack(final BlockState state, final HitResult target, final BlockGetter level,
            final BlockPos pos, final Player player) {
        final ItemStack stack = new ItemStack(this);
        if (level.getBlockEntity(pos) instanceof OBTankBlockEntity tank) {
            final FluidStack fluid = tank.contents();
            if (!fluid.isEmpty()) OBTankItem.setContents(stack, new FluidStack(fluid, OBTankBlockEntity.capacity()));
        }
        return stack;
    }

    @Override
    public boolean hasAnalogOutputSignal(final BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(final BlockState state, final Level level, final BlockPos pos) {
        return level.getBlockEntity(pos) instanceof OBTankBlockEntity tank ? tank.comparatorOutput() : 0;
    }

    @Override
    public InteractionResult use(final BlockState state, final Level level, final BlockPos pos,
            final Player player, final InteractionHand hand, final BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof OBTankBlockEntity tank)) return InteractionResult.PASS;
        final IFluidHandler handler = tank.getCapability(ForgeCapabilities.FLUID_HANDLER, hit.getDirection()).orElse(null);
        if (handler == null) return InteractionResult.PASS;
        final ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty()) {
            if (hand != InteractionHand.MAIN_HAND || !takeExperience(player, tank.contents(), handler)) return InteractionResult.PASS;
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        final var container = FluidUtil.getFluidHandler(held).orElse(null);
        if (container == null) return InteractionResult.PASS;
        final boolean empty = container.drain(FluidType.BUCKET_VOLUME, IFluidHandler.FluidAction.SIMULATE).isEmpty();
        if (empty && !canFill(held)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        return player.getCapability(ForgeCapabilities.ITEM_HANDLER).map(inventory -> {
            final var result = empty
                    ? FluidUtil.tryFillContainerAndStow(held, handler, inventory, FluidType.BUCKET_VOLUME, player, true)
                    : FluidUtil.tryEmptyContainerAndStow(held, handler, inventory, FluidType.BUCKET_VOLUME, player, true);
            if (!result.isSuccess()) return InteractionResult.PASS;
            player.setItemInHand(hand, result.getResult());
            return InteractionResult.CONSUME;
        }).orElse(InteractionResult.PASS);
    }

    private static boolean canFill(final ItemStack stack) {
        if (!OBServerConfig.TANK_BUCKET_DRAIN.get()) return false;
        if (stack.getItem() instanceof BucketItem) return true;
        final var id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null && OBServerConfig.TANK_ADDITIONAL_BUCKETS.get().contains(id.toString());
    }

    private static boolean takeExperience(final Player player, final FluidStack fluid, final IFluidHandler handler) {
        final int ratio = OBLiquidExperience.ratio(fluid);
        if (ratio <= 0) return false;
        final int needed = Math.max(1, player.getXpNeededForNextLevel() - Math.round(player.experienceProgress * player.getXpNeededForNextLevel()));
        final int requested = OBLiquidExperience.millibuckets(needed, ratio);
        final int available = handler.drain(requested, IFluidHandler.FluidAction.SIMULATE).getAmount() / ratio;
        if (available <= 0) return false;
        if (player.level().isClientSide) return true;

        final FluidStack withdrawn = handler.drain(available * ratio, IFluidHandler.FluidAction.EXECUTE);
        final int before = OBLiquidExperience.playerPoints(player);
        player.giveExperiencePoints(withdrawn.getAmount() / ratio);
        final int gained = Math.max(0, OBLiquidExperience.playerPoints(player) - before);
        final int unspent = withdrawn.getAmount() - OBLiquidExperience.millibuckets(gained, ratio);
        if (unspent > 0) handler.fill(new FluidStack(withdrawn, unspent), IFluidHandler.FluidAction.EXECUTE);
        return true;
    }
}

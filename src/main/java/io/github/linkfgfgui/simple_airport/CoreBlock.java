package io.github.linkfgfgui.simple_airport;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import java.lang.SuppressWarnings;

import static io.github.linkfgfgui.simple_airport.SimpleAirport.CONFIG_STICK_ITEM;
import static io.github.linkfgfgui.simple_airport.SimpleAirport.CORE_BLOCK_POS_DATACOM;
import static io.github.linkfgfgui.simple_airport.SimpleAirport.CORE_BLOCK_ENTITY;
import static io.github.linkfgfgui.simple_airport.SimpleAirport.CoreBlockPosRecord;



public class CoreBlock extends Block implements EntityBlock {


    public CoreBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CoreBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player.getMainHandItem().getItem() == Items.AIR && player.isCrouching()) {
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            ItemStack itemStack = CONFIG_STICK_ITEM.toStack(1);
            PatchedDataComponentMap dcp = (PatchedDataComponentMap) itemStack.getComponents();
            dcp.set(CORE_BLOCK_POS_DATACOM.get(), new CoreBlockPosRecord(pos.getX(), pos.getY(), pos.getZ()));
            player.getInventory().add(itemStack);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == CORE_BLOCK_ENTITY.get() ? (BlockEntityTicker<T>) (level1, pos, state1, be) -> CoreBlockEntity.tick(level1, pos, state1, (CoreBlockEntity) be) : null;
    }
}

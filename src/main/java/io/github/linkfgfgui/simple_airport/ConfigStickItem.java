package io.github.linkfgfgui.simple_airport;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;

import javax.annotation.Nullable;

public class ConfigStickItem extends Item {


    public ConfigStickItem(Properties properties) {
        super(properties);
    }


    @Nullable static BlockPos getComp(ItemStack is){
        SimpleAirport.CoreBlockPosRecord cbpr = is.getComponents().get(SimpleAirport.CORE_BLOCK_POS_DATACOM.get());
        if (cbpr == null) return null;
        return new BlockPos(cbpr.x(),cbpr.y(),cbpr.z());
    }

    @Nullable
    private static CoreBlockEntity getEntity(ItemStack is,Player player,BlockPos target,Level level){
        BlockPos pos = getComp(is);
        if (pos == null) return null;
        if (target != null){if (pos.equals(target)) return null;}
        return getEntity(player,pos,level);
    }


    @Nullable
    private static CoreBlockEntity getEntity(Player player,BlockPos pos,Level level){
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof CoreBlockEntity entity)){
            if (player != null) {
                player.displayClientMessage(Component.translatable("simpleairport.actionbar.core_not_found"),true);
            }
            return null;
        }
        return entity;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        boolean isMainhand= context.getHand().equals(InteractionHand.MAIN_HAND);
        BlockPos target=context.getClickedPos();
        BlockPos pos = getComp(context.getItemInHand());
        if (player == null) return InteractionResult.FAIL;
        if (pos==null) return  InteractionResult.FAIL;
        if (pos.equals(target)) return InteractionResult.FAIL;
        if (context.getLevel().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        CoreBlockEntity entity = getEntity(player,pos,context.getLevel());
        if (entity==null) return InteractionResult.FAIL;
        if (isMainhand){
            entity.addApron(target);
            player.displayClientMessage(Component.translatable("simpleairport.actionbar.add_a_apron"),true);
        }else {
            entity.removeApron(target);
            player.displayClientMessage(Component.translatable("simpleairport.actionbar.remove_a_apron"),true);
        }
        return InteractionResult.SUCCESS;
    }


    public static void onItemToss(ItemTossEvent event) {
        ItemEntity entity = event.getEntity();
        ItemStack is=entity.getItem();
        Player player = event.getPlayer();
        CoreBlockEntity cbentity = getEntity(is,player,null,player.level());
        is.setCount(0);
        if (cbentity==null) return;
        BlockPos pos = player.blockPosition().below();
        cbentity.setRWY(pos);
        player.displayClientMessage(Component.translatable("simpleairport.actionbar.set_a_runway",
                pos.getX(),pos.getY(),pos.getZ()),true);

    }
}

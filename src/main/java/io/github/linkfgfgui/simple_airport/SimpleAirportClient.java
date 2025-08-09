package io.github.linkfgfgui.simple_airport;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;


import java.util.ArrayList;
import java.util.List;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = SimpleAirport.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = SimpleAirport.MODID, value = Dist.CLIENT)
public class SimpleAirportClient {
    private static final int HIGHLIGHT_RADIUS = 5;
    private static final List<BlockPos> cachedBlocks = new ArrayList<>();
    private static long lastUpdateTime = 0;
    public SimpleAirportClient(ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
//        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
//        // Some client setup code
//        SimpleAirport.LOGGER.info("HELLO FROM CLIENT SETUP");
//        SimpleAirport.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }

    @SubscribeEvent
    public static void onRenderWorldLast(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        ItemStack handItem = mc.player.getMainHandItem();
        if (handItem.getItem() != SimpleAirport.CONFIG_STICK_ITEM.get()) {
            handItem = mc.player.getOffhandItem();
            if (handItem.getItem() != SimpleAirport.CONFIG_STICK_ITEM.get()) {
                return;
            }
        }
        long now = System.currentTimeMillis();
        if (now - lastUpdateTime > 500) {
            SimpleAirport.CoreBlockPosRecord record= handItem.getComponents().get(SimpleAirport.CORE_BLOCK_POS_DATACOM.get());
            if (record==null) return;
            BlockEntity blockEntity = mc.level.getBlockEntity(new BlockPos(record.x(),record.y(),record.z()));
            if (blockEntity == null) return;
            if (!(blockEntity instanceof CoreBlockEntity entity))return;
            cachedBlocks.clear();

            BlockPos playerPos = mc.player.blockPosition();
            for (int dx = -HIGHLIGHT_RADIUS; dx <= HIGHLIGHT_RADIUS; dx++) {
                for (int dy = -HIGHLIGHT_RADIUS; dy <= HIGHLIGHT_RADIUS; dy++) {
                    for (int dz = -HIGHLIGHT_RADIUS; dz <= HIGHLIGHT_RADIUS; dz++) {
                        BlockPos pos = playerPos.offset(dx, dy, dz);
                        if (entity.aprons.contains(pos)) {
                            cachedBlocks.add(pos);
                        }
                    }
                }
            }
            lastUpdateTime = now;
        }

        PoseStack matrixStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        double camX = mc.gameRenderer.getMainCamera().getPosition().x;
        double camY = mc.gameRenderer.getMainCamera().getPosition().y;
        double camZ = mc.gameRenderer.getMainCamera().getPosition().z;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.lineWidth(5f);
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();

        matrixStack.pushPose();
        matrixStack.translate(-camX,-camY,-camZ);

        for (BlockPos pos : cachedBlocks) {
            AABB box = new AABB(pos);
            LevelRenderer.renderLineBox(matrixStack, bufferSource.getBuffer(RenderType.lines()), box, 0f, 0f, 1f, 0.5f);
        }

        matrixStack.popPose();
        bufferSource.endBatch(RenderType.lines());

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

}

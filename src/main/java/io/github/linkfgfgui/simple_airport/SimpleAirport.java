package io.github.linkfgfgui.simple_airport;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(SimpleAirport.MODID)
public class SimpleAirport {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "simpleairport";
    public static final Logger LOGGER = LogUtils.getLogger();
    public record CoreBlockPosRecord(int x, int y, int z) {}
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredBlock<CoreBlock> CORE_BLOCK = BLOCKS.register("core", () -> new CoreBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .requiresCorrectToolForDrops()
            .strength(5.0F, 6.0F)
            .sound(SoundType.METAL)));
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredItem<BlockItem> CORE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("core", CORE_BLOCK);
    public static final DeferredItem<Item> CONFIG_STICK_ITEM = ITEMS.registerItem("config_stick",ConfigStickItem::new, new Item.Properties().stacksTo(1));
    public static final DeferredRegister.DataComponents REGISTRAR = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MODID);

    public static Codec<CoreBlockPosRecord> BASIC_CODEC = RecordCodecBuilder.create(
            instance ->instance.group(
                            Codec.INT.fieldOf("x").forGetter(CoreBlockPosRecord::x),
                            Codec.INT.fieldOf("y").forGetter(CoreBlockPosRecord::y),
                            Codec.INT.fieldOf("z").forGetter(CoreBlockPosRecord::z))
                    .apply(instance, CoreBlockPosRecord::new));

    public static final StreamCodec<ByteBuf, CoreBlockPosRecord> BASIC_STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, CoreBlockPosRecord::x,
            ByteBufCodecs.INT, CoreBlockPosRecord::y,
            ByteBufCodecs.INT, CoreBlockPosRecord::z,
            CoreBlockPosRecord::new
    );
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CoreBlockPosRecord>> CORE_BLOCK_POS_DATACOM = REGISTRAR.registerComponentType(
            "core_block_pos",
            builder -> builder
                    // The codec to read/write the data to disk
                    .persistent(BASIC_CODEC)
                    // The codec to read/write the data across the network
                    .networkSynchronized(BASIC_STREAM_CODEC)
    );
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final Supplier<BlockEntityType<CoreBlockEntity>> CORE_BLOCK_ENTITY = BLOCK_ENTITIES.register("core_entity", () -> BlockEntityType.Builder.of(CoreBlockEntity::new, CORE_BLOCK.get()).build(null)
    );
    // Creates a new food item with the id "simpleairport:example_id", nutrition 1 and saturation 2


    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "simpleairport" namespace

//    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
//    // Creates a creative tab with the id "simpleairport:example_tab" for the example item, that is placed after the combat tab
//    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("example_tab", () -> CreativeModeTab.builder()
//            .title(Component.translatable("itemGroup.simpleairport")) //The language key for the title of your CreativeModeTab
//            .withTabsBefore(CreativeModeTabs.COMBAT)
//            .icon(() -> CO.get().getDefaultInstance())
//            .displayItems((parameters, output) -> {
//                output.accept(EXAMPLE_ITEM.get()); // Add the example item to the tab. For your own tabs, this method is preferred over the event
//            }).build());

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public SimpleAirport(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);
        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
//        CREATIVE_MODE_TABS.register(modEventBus);
        REGISTRAR.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (SimpleAirport) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.addListener(ConfigStickItem::onItemToss);
        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
//        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {

    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(CORE_BLOCK);
        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
//        LOGGER.info("HELLO from server starting");
    }
}

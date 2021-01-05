package openblocks;

import com.google.common.collect.ImmutableSet;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.data.DataGenerator;
import net.minecraft.fluid.Fluid;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidAttributes;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.GatherDataEvent;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.ObjectHolder;
import openblocks.client.ClientProxy;
import openblocks.common.GuideActionHandler;
import openblocks.common.ServerProxy;
import openblocks.common.block.BlockBuilderGuide;
import openblocks.common.block.BlockGuide;
import openblocks.common.block.BlockLadder;
import openblocks.common.block.BlockVacuumHopper;
import openblocks.common.container.ContainerVacuumHopper;
import openblocks.common.item.ItemGuide;
import openblocks.common.tileentity.TileEntityBuilderGuide;
import openblocks.common.tileentity.TileEntityGuide;
import openblocks.common.tileentity.TileEntityVacuumHopper;
import openblocks.data.OpenBlockRecipes;
import openblocks.data.OpenBlocksLoot;
import openblocks.data.OpenBlocksModels;
import openblocks.events.GuideActionEvent;
import openblocks.rpc.IGuideAnimationTrigger;
import openmods.container.TileEntityContainerFactory;
import openmods.network.event.NetworkEventEntry;
import openmods.network.event.NetworkEventManager;
import openmods.network.rpc.MethodEntry;
import openmods.network.rpc.RpcCallDispatcher;
import openmods.sync.SyncableObjectType;

@Mod(OpenBlocks.MODID)
@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class OpenBlocks {
	public static final String MODID = "openblocks";

	public static ResourceLocation location(String path) {
		return new ResourceLocation(MODID, path);
	}

	public static IOpenBlocksProxy PROXY = DistExecutor.runForDist(() -> ClientProxy::new, () -> ServerProxy::new);

	private static final String BLOCK_LADDER = "ladder";
	private static final String BLOCK_GUIDE = "guide";
	private static final String BLOCK_BUILDER_GUIDE = "builder_guide";
	private static final String BLOCK_VACUUM_HOPPER = "vacuum_hopper";
	private static final String FLUID_XP = "xpjuice";

	public static final ItemGroup OPEN_BLOCKS_TAB = new ItemGroup("openblocks") {
		@Override
		@OnlyIn(Dist.CLIENT)
		public ItemStack createIcon() {
			return new ItemStack(Blocks.ladder);
		}
	};

	@ObjectHolder(MODID)
	public static class Blocks {
		@ObjectHolder(BLOCK_LADDER)
		public static Block ladder;

		@ObjectHolder(BLOCK_GUIDE)
		public static Block guide;

		@ObjectHolder(BLOCK_BUILDER_GUIDE)
		public static Block builderGuide;

		@ObjectHolder(BLOCK_VACUUM_HOPPER)
		public static Block vacuumHopper;
	}

	@ObjectHolder(MODID)
	public static class Items {
		@ObjectHolder(BLOCK_LADDER)
		public static Item ladder;

		@ObjectHolder(BLOCK_GUIDE)
		public static Item guide;

		@ObjectHolder(BLOCK_BUILDER_GUIDE)
		public static Item builderGuide;

		@ObjectHolder(BLOCK_VACUUM_HOPPER)
		public static Item vacuumHopper;
	}

	@ObjectHolder(MODID)
	public static class TileEntities {
		@ObjectHolder(BLOCK_GUIDE)
		public static TileEntityType<TileEntityGuide> guide;

		@ObjectHolder(BLOCK_BUILDER_GUIDE)
		public static TileEntityType<TileEntityBuilderGuide> builderGuide;

		@ObjectHolder(BLOCK_VACUUM_HOPPER)
		public static TileEntityType<TileEntityVacuumHopper> vacuumHopper;
	}

	@ObjectHolder(MODID)
	public static class Sounds {

	}

	@ObjectHolder(MODID)
	public static class Enchantments {
	}

	@ObjectHolder(MODID)
	public static class Fluids {
		@ObjectHolder(FLUID_XP)
		public static Fluid xpJuice;
	}

	@ObjectHolder(MODID)
	public static class Containers {
		@ObjectHolder(BLOCK_VACUUM_HOPPER)
		public static ContainerType<ContainerVacuumHopper> vacuumHopper;
	}

	public OpenBlocks() {
		PROXY.eventInit();
	}

	@SubscribeEvent
	public void registerRegistry(RegistryEvent.NewRegistry e) {
		PROXY.syncInit();
	}

	@SubscribeEvent
	public static void registerSyncTypes(final RegistryEvent.Register<SyncableObjectType> type) {
	}

	@SubscribeEvent
	public static void registerMethodTypes(final RegistryEvent.Register<MethodEntry> evt) {
		RpcCallDispatcher.startMethodRegistration(evt.getRegistry())
				.registerInterface(location("guide_animation"), IGuideAnimationTrigger.class);
	}

	@SubscribeEvent
	public static void registerNetworkEvents(final RegistryEvent.Register<NetworkEventEntry> evt) {
		NetworkEventManager.startRegistration(evt.getRegistry())
				.register(location("guide_action"), GuideActionEvent.class, GuideActionEvent::new);
	}

	@SubscribeEvent
	public static void registerBlocks(final RegistryEvent.Register<Block> evt) {
		final IForgeRegistry<Block> registry = evt.getRegistry();
		registry.register(new BlockLadder(Block.Properties.from(net.minecraft.block.Blocks.OAK_TRAPDOOR)).setRegistryName(BLOCK_LADDER));
		registry.register(new BlockGuide(Block.Properties.create(Material.ROCK).notSolid().setLightLevel(v -> 10)).setTileEntity(TileEntityGuide.class).setRegistryName(BLOCK_GUIDE));
		registry.register(new BlockBuilderGuide(Block.Properties.create(Material.ROCK).notSolid().setLightLevel(v -> 10)).setTileEntity(TileEntityBuilderGuide.class).setRegistryName(BLOCK_BUILDER_GUIDE));
		registry.register(new BlockVacuumHopper(Block.Properties.create(Material.ROCK)).setTileEntity(TileEntityVacuumHopper.class).setRegistryName(BLOCK_VACUUM_HOPPER));
	}

	@SubscribeEvent
	public static void registerItems(final RegistryEvent.Register<Item> evt) {
		final IForgeRegistry<Item> registry = evt.getRegistry();
		registry.register(new BlockItem(Blocks.ladder, new Item.Properties().group(OPEN_BLOCKS_TAB)).setRegistryName(BLOCK_LADDER));
		registry.register(new ItemGuide(Blocks.guide, new Item.Properties().group(OPEN_BLOCKS_TAB)).setRegistryName(BLOCK_GUIDE));
		registry.register(new ItemGuide(Blocks.builderGuide, new Item.Properties().group(OPEN_BLOCKS_TAB)).setRegistryName(BLOCK_BUILDER_GUIDE));
		registry.register(new BlockItem(Blocks.vacuumHopper, new Item.Properties().group(OPEN_BLOCKS_TAB)).setRegistryName(BLOCK_VACUUM_HOPPER));
	}

	@SubscribeEvent
	public static void registerTileEntities(final RegistryEvent.Register<TileEntityType<?>> evt) {
		final IForgeRegistry<TileEntityType<?>> registry = evt.getRegistry();
		registry.register(new TileEntityType<>(TileEntityGuide::new, ImmutableSet.of(Blocks.guide), null).setRegistryName(BLOCK_GUIDE));
		registry.register(new TileEntityType<>(TileEntityBuilderGuide::new, ImmutableSet.of(Blocks.builderGuide), null).setRegistryName(BLOCK_BUILDER_GUIDE));
		registry.register(new TileEntityType<>(TileEntityVacuumHopper::new, ImmutableSet.of(Blocks.vacuumHopper), null).setRegistryName(BLOCK_VACUUM_HOPPER));
	}

	@SubscribeEvent
	public static void registerFluids(final RegistryEvent.Register<Fluid> evt) {
		final IForgeRegistry<Fluid> registry = evt.getRegistry();
		registry.register(new ForgeFlowingFluid.Source(
				new ForgeFlowingFluid.Properties(() -> Fluids.xpJuice, () -> Fluids.xpJuice,
						FluidAttributes.builder(location("block/xp_juice_still"), location("block/xp_juice_flowing"))
								.luminosity(10)
								.density(800)
								.viscosity(1500)
								.translationKey("fluid.openblocks.xp_juice")
								.sound(SoundEvents.ENTITY_PLAYER_LEVELUP, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP)
				)
		).setRegistryName(FLUID_XP));
	}

	@SubscribeEvent
	public static void registerContainers(final RegistryEvent.Register<ContainerType<?>> evt) {
		final IForgeRegistry<ContainerType<?>> registry = evt.getRegistry();
		registry.register(new ContainerType<>(new TileEntityContainerFactory<>(ContainerVacuumHopper::new, TileEntities.vacuumHopper)).setRegistryName(BLOCK_VACUUM_HOPPER));
	}

	@SubscribeEvent
	public static void commonInit(final FMLCommonSetupEvent evt) {
		MinecraftForge.EVENT_BUS.register(new GuideActionHandler());
	}

	@SubscribeEvent
	public static void clientInit(final FMLClientSetupEvent evt) {
		PROXY.clientInit();
	}

	@SubscribeEvent
	public static void registerGenerators(final GatherDataEvent event) {
		final DataGenerator generator = event.getGenerator();
		generator.addProvider(new OpenBlockRecipes(generator));
		generator.addProvider(new OpenBlocksLoot(generator));
		generator.addProvider(new OpenBlocksModels(generator));
	}
}

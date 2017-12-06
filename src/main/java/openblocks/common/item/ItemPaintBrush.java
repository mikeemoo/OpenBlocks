package openblocks.common.item;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.color.IItemColor;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import openblocks.Config;
import openblocks.OpenBlocks;
import openblocks.api.IPaintableBlock;
import openblocks.common.CanvasReplaceBlacklist;
import openblocks.common.block.BlockCanvas;
import openmods.colors.ColorMeta;
import openmods.colors.ColorUtils;
import openmods.colors.RGB;
import openmods.infobook.BookDocumentation;
import openmods.utils.ItemUtils;

@BookDocumentation(customName = "paintbrush", hasVideo = true)
public class ItemPaintBrush extends Item {

	@SideOnly(Side.CLIENT)
	public static class ColorHandler implements IItemColor {
		@Override
		public int colorMultiplier(ItemStack stack, int tintIndex) {
			if (tintIndex == 1) {
				Integer color = getColorFromStack(stack);
				if (color != null) return color;
			}

			return 0xFFFFFF;
		}
	}

	private static final int SINGLE_COLOR_THRESHOLD = 16;

	private static final String TAG_COLOR = "color";

	public static final int MAX_USES = 24;

	public ItemPaintBrush() {
		setMaxStackSize(1);
		setMaxDamage(MAX_USES);
		setNoRepair();
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(@Nonnull ItemStack stack, @Nullable World world, List<String> result, ITooltipFlag flag) {
		Integer color = getColorFromStack(stack);
		if (color != null) result.add(String.format("#%06X", color));
	}

	@Override
	public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> list) {
		list.add(new ItemStack(this));
		for (ColorMeta color : ColorMeta.getAllColors()) {
			list.add(createStackWithColor(color.rgb));
		}
	}

	@Nonnull
	public static ItemStack createStackWithColor(int color) {
		ItemStack stack = new ItemStack(OpenBlocks.Items.paintBrush);
		NBTTagCompound tag = ItemUtils.getItemTag(stack);
		tag.setInteger(TAG_COLOR, color);
		return stack;
	}

	@Override
	public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		if (hand != EnumHand.MAIN_HAND) return EnumActionResult.PASS;

		final ItemStack stack = player.getHeldItem(hand);
		final Integer color = getColorFromStack(stack);
		if (stack.getItemDamage() > MAX_USES || color == null) return EnumActionResult.FAIL;

		if (Config.paintbrushReplacesBlocks && CanvasReplaceBlacklist.instance.isAllowedToReplace(world, pos)) {
			BlockCanvas.replaceBlock(world, pos);
		}

		final boolean changed;

		final int solidColor = 0xFF000000 | color;
		if (player.isSneaking()) changed = tryRecolorBlock(world, pos, solidColor, EnumFacing.VALUES);
		else changed = tryRecolorBlock(world, pos, solidColor, facing);

		if (changed) {
			world.playSound(null, player.getPosition(), SoundEvents.ENTITY_SLIME_SQUISH, SoundCategory.PLAYERS, 0.1F, 0.8F);

			if (!player.capabilities.isCreativeMode) {
				if (stack.attemptDamageItem(1, player.getRNG(), player instanceof EntityPlayerMP? (EntityPlayerMP)player : null)) {
					final NBTTagCompound tag = ItemUtils.getItemTag(stack);
					tag.removeTag(TAG_COLOR);
					stack.setItemDamage(0);
				}
			}

			return EnumActionResult.SUCCESS;
		}

		return EnumActionResult.FAIL;
	}

	private static boolean tryRecolorBlock(World world, BlockPos pos, int rgb, EnumFacing... sides) {
		IBlockState state = world.getBlockState(pos);
		Block block = state.getBlock();

		// first try RGB color...
		if (block instanceof IPaintableBlock) {
			IPaintableBlock paintableBlock = (IPaintableBlock)block;

			boolean result = false;
			for (EnumFacing dir : sides)
				result |= paintableBlock.recolorBlock(world, pos, dir, rgb);

			return result;
		}

		// ...then try finding nearest vanilla one
		final ColorMeta nearest = ColorUtils.findNearestColor(new RGB(rgb), SINGLE_COLOR_THRESHOLD);
		if (nearest != null) {

			boolean result = false;
			for (EnumFacing dir : sides)
				result |= block.recolorBlock(world, pos, dir, nearest.vanillaEnum);

			return result;
		}

		return false;
	}

	@Override
	public boolean onBlockStartBreak(ItemStack itemstack, BlockPos pos, EntityPlayer player) {
		return false;
	}

	public static Integer getColorFromStack(@Nonnull ItemStack stack) {
		if (stack.hasTagCompound()) {
			NBTTagCompound tag = stack.getTagCompound();
			if (tag.hasKey(TAG_COLOR)) { return tag.getInteger(TAG_COLOR); }
		}
		return null;
	}

	public static void setColor(@Nonnull ItemStack stack, int color) {
		NBTTagCompound tag = ItemUtils.getItemTag(stack);
		tag.setInteger(TAG_COLOR, color);
	}
}

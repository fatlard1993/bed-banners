package justfatlard.bed_banners;

import java.util.List;

import com.mojang.math.Transformation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The pattern, lying on the blanket.
 *
 * <p>A bed has nowhere to keep it. Beds lost their block entity on this version and are ordinary
 * model-rendered blocks now, so there is no per-bed anything: no data to hang a pattern on and no
 * renderer to draw one with. What there is instead is a banner, which already knows how to render
 * an arbitrary stack of pattern layers, and an item display, which will render any stack you hand
 * it. Laid flat in its inventory transform, a banner is exactly the picture wanted.
 *
 * <p>So the display entity is both the drawing and the record. Nothing else stores which bed is
 * patterned: the entity sitting on it is the fact.
 */
public final class BedBlanket {
	private BedBlanket() {}

	/** Tagged so a blanket can be told from every other item display in the world. */
	public static final String TAG = "bed_banners_blanket";

	/** Just clear of the mattress, which stands 9/16 of a block tall. */
	private static final double LIFT = 0.5625 + 0.01;

	/** A bed is one block by two. The sprite is square, so it is stretched to match. */
	private static final float ACROSS = 0.86F;
	private static final float ALONG = 1.72F;

	/** Close enough to see from the doorway, not so far that a bedroom is visible from a hill. */
	private static final float VIEW_RANGE = 0.5F;

	/** Put the pattern on a bed that has just been placed, or replace the one already there. */
	public static void lay(ServerLevel level, BlockPos pos, BlockState state, ItemStack bed) {
		if (!BedBanners.isPatterned(bed)) return;

		BlockPos foot = footOf(state, pos);
		if (foot == null) return;

		strip(level, foot, state);

		Display.ItemDisplay blanket = new Display.ItemDisplay(EntityTypes.ITEM_DISPLAY, level);
		blanket.setPos(middleOf(foot, state));
		blanket.getSlot(0).set(bannerFor(bed));
		blanket.setItemTransform(ItemDisplayContext.GUI);
		blanket.setBillboardConstraints(Display.BillboardConstraints.FIXED);
		blanket.setViewRange(VIEW_RANGE);
		blanket.setTransformation(lyingFlat(state));
		blanket.addTag(TAG);

		level.addFreshEntity(blanket);
	}

	/** Take the pattern off, for a bed being broken or repatterned. */
	public static void strip(ServerLevel level, BlockPos foot, BlockState state) {
		for (Display.ItemDisplay blanket : blanketsOn(level, foot, state)) {
			blanket.discard();
		}
	}

	/**
	 * The bed item this blanket came from, for handing back when the bed is broken.
	 *
	 * <p>Read off the entity rather than a table of our own, which is the point of storing the
	 * pattern in the thing that draws it: there is no second copy to disagree with.
	 */
	public static ItemStack pickUp(ServerLevel level, BlockPos foot, BlockState state, ItemStack dropped) {
		List<Display.ItemDisplay> blankets = blanketsOn(level, foot, state);
		if (blankets.isEmpty()) return dropped;

		ItemStack banner = blankets.get(0).getSlot(0).get();
		var layers = banner.get(DataComponents.BANNER_PATTERNS);
		if (layers != null) dropped.set(DataComponents.BANNER_PATTERNS, layers);

		return dropped;
	}

	/** The blanket entities sitting on this bed. */
	private static List<Display.ItemDisplay> blanketsOn(ServerLevel level, BlockPos foot, BlockState state) {
		Vec3 middle = middleOf(foot, state);

		return level.getEntitiesOfClass(Display.ItemDisplay.class,
			new AABB(middle, middle).inflate(1.1, 0.6, 1.1),
			blanket -> blanket.entityTags().contains(TAG));
	}

	/** The foot end, whichever half was touched: one bed, one blanket, one place to look for it. */
	public static BlockPos footOf(BlockState state, BlockPos pos) {
		if (!(state.getBlock() instanceof BedBlock)) return null;

		return state.getValue(BedBlock.PART) == BedPart.FOOT
			? pos
			: pos.relative(state.getValue(BedBlock.FACING).getOpposite());
	}

	/** Halfway between the two blocks, just above the mattress. */
	private static Vec3 middleOf(BlockPos foot, BlockState state) {
		Direction towardsHead = state.getValue(BedBlock.FACING);

		return new Vec3(
			foot.getX() + 0.5 + towardsHead.getStepX() * 0.5,
			foot.getY() + LIFT,
			foot.getZ() + 0.5 + towardsHead.getStepZ() * 0.5);
	}

	/** Face up, turned to lie along the bed, stretched from square to the bed's two-by-one. */
	private static Transformation lyingFlat(BlockState state) {
		org.joml.Quaternionf facingUp = new org.joml.Quaternionf()
			.rotateY((float) Math.toRadians(-state.getValue(BedBlock.FACING).toYRot()))
			.rotateX((float) Math.toRadians(90));

		return new Transformation(
			new org.joml.Vector3f(0, 0, 0),
			facingUp,
			new org.joml.Vector3f(ACROSS, ALONG, 1F),
			null);
	}

	private static ItemStack bannerFor(ItemStack bed) {
		ItemStack banner = new ItemStack(BedBanners.bannerFor(bed.getItem()));
		var layers = bed.get(DataComponents.BANNER_PATTERNS);
		if (layers != null) banner.set(DataComponents.BANNER_PATTERNS, layers);

		return banner;
	}
}

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
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.server.level.ServerPlayer;
import justfatlard.pandorical.api.BannerDecalApi;
import justfatlard.pandorical.api.PandoricalApi;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The pattern, lying on the blanket.
 *
 * <p>A bed has nowhere to keep it. Beds lost their block entity on this version and are ordinary
 * model-rendered blocks now, so there is no per-bed anything: no data to hang a pattern on and no
 * renderer to draw one with. What there is instead is a banner, which already knows how to render
 * an arbitrary stack of pattern layers, and an item display, which will render any stack you hand
 * it. Laid flat, a banner's flag is exactly the picture wanted.
 *
 * <p>The banner is shown with no display context at all, so what the entity's transform gets is
 * the banner item in its raw item space: the whole standing banner, pole and all, base at the
 * bottom of the item cube, flag hung on the pole's far side. Any other context puts the
 * inventory or hand pose in front of the transform - the first version used the GUI one, and its
 * thirty-degree tilt turned "lying flat" into a flag standing crooked on the mattress.
 *
 * <p>So the display entity is the record, and the drawing for a vanilla client. Nothing else
 * stores which bed is patterned: the entity sitting on it is the fact.
 *
 * <p>A Pandorical client gets something better. A banner lying on a bed is a banner lying on a
 * bed: its own base colour over the blanket, its own pixel size, a foreign object however flat
 * it lies. So the pattern layers alone are laid on the bed as a decal through Pandorical, drawn
 * over the bed's own blanket texture with no base colour, at the blanket's size - and the item
 * display is marked so a Pandorical client leaves it undrawn.
 */
public final class BedBlanket {
	private BedBlanket() {}

	/** Tagged so a blanket can be told from every other item display in the world. */
	public static final String TAG = "bed_banners_blanket";

	/** Just clear of the mattress, which stands 9/16 of a block tall. */
	private static final double LIFT = 0.5625 + 0.01;

	/**
	 * A bed is one block by two; the flag is stretched across it and stopped a little short
	 * along its length. The pole runs on past the flag's foot, inside the mattress, and it has to
	 * end before the bed does or its tip shows through the footboard: it did, as a stick.
	 */
	private static final float ACROSS = 0.86F;
	private static final float ALONG = 1.6F;

	/**
	 * The flag in the banner item's own space, as the item display draws it: twenty by forty
	 * pixels at the two-thirds scale the item definition applies, a sixteenth of a block thick,
	 * its centre half a block up the cube, hung on the -z side of the pole. Minus z, not plus:
	 * the item display renderer turns every item half a turn about y before the entity's own
	 * transform, and the first version worked out the geometry without that turn. It put the
	 * flag inside the mattress and the pole on top.
	 */
	private static final float FLAG_WIDE = 20 / 16F * 2 / 3F;
	private static final float FLAG_LONG = 40 / 16F * 2 / 3F;
	private static final Vector3f FLAG_CENTRE = new Vector3f(0, 0.5F, -1 / 16F);

	/**
	 * The blanket on a vanilla bed: the mattress top is nine pixels up, the pillow takes the
	 * first six pixels of the head block, and the blanket runs the remaining twenty-six to the
	 * foot, full width.
	 */
	private static final float MATTRESS_TOP = 9 / 16F;
	private static final float PILLOW = 6 / 16F;
	private static final float BLANKET_LONG = 26 / 16F;
	private static final float BLANKET_WIDE = 1F;

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
		blanket.setBillboardConstraints(Display.BillboardConstraints.FIXED);
		blanket.setViewRange(VIEW_RANGE);
		pose(blanket, foot, state);
		blanket.addTag(TAG);

		level.addFreshEntity(blanket);
		show(level, foot, state, bed.get(DataComponents.BANNER_PATTERNS));
	}

	/** The decal for this bed, to everyone in its level. */
	private static void show(ServerLevel level, BlockPos foot, BlockState state, BannerPatternLayers layers) {
		if (layers == null || layers.layers().isEmpty()) return;
		BannerDecalApi.Decal decal = decalFor(foot, state, layers);
		for (ServerPlayer player : level.players()) {
			PandoricalApi.bannerDecals().send(player, decal);
		}
	}

	/** Every blanket loaded in this level, to one player who has just arrived in it. */
	public static void showAll(ServerLevel level, ServerPlayer player) {
		List<BannerDecalApi.Decal> decals = new java.util.ArrayList<>();
		for (Display.ItemDisplay blanket : level.getEntities(
				net.minecraft.world.level.entity.EntityTypeTest.forClass(Display.ItemDisplay.class),
				entity -> entity.entityTags().contains(TAG))) {
			BlockPos at = BlockPos.containing(blanket.position());
			BlockState state = level.getBlockState(at);
			BlockPos foot = footOf(state, at);
			if (foot == null) continue;
			BannerPatternLayers layers = blanket.getSlot(0).get().get(DataComponents.BANNER_PATTERNS);
			if (layers == null || layers.layers().isEmpty()) continue;
			decals.add(decalFor(foot, state, layers));
		}
		PandoricalApi.bannerDecals().send(player, decals);
	}

	/** Anchored on the head block, the pattern's top at the pillow's edge, running to the foot. */
	private static BannerDecalApi.Decal decalFor(BlockPos foot, BlockState state, BannerPatternLayers layers) {
		Direction toHead = state.getValue(BedBlock.FACING);
		return new BannerDecalApi.Decal(foot.relative(toHead), toHead, MATTRESS_TOP, PILLOW,
			BLANKET_LONG, BLANKET_WIDE, layers);
	}

	/**
	 * Put a blanket that has just loaded back into today's pose.
	 *
	 * <p>The pose is stored on the entity, so a blanket laid by an older build keeps the older
	 * build's pose until something rewrites it. This is that something: every blanket is re-posed
	 * from the bed under it as it loads, which costs a block lookup per blanket per chunk load and
	 * means a fix to the pose reaches every bed already in the world.
	 */
	public static void refresh(ServerLevel level, Display.ItemDisplay blanket) {
		if (!blanket.entityTags().contains(TAG)) return;

		BlockPos at = BlockPos.containing(blanket.position());
		BlockState state = level.getBlockState(at);
		BlockPos foot = footOf(state, at);
		if (foot == null) return;

		pose(blanket, foot, state);
		show(level, foot, state, blanket.getSlot(0).get().get(DataComponents.BANNER_PATTERNS));
	}

	private static void pose(Display.ItemDisplay blanket, BlockPos foot, BlockState state) {
		blanket.setPos(middleOf(foot, state));
		blanket.setItemTransform(ItemDisplayContext.NONE);
		blanket.setTransformation(lyingFlat(state));
	}

	/** Take the pattern off, for a bed being broken or repatterned. */
	public static void strip(ServerLevel level, BlockPos foot, BlockState state) {
		for (Display.ItemDisplay blanket : blanketsOn(level, foot, state)) {
			blanket.discard();
		}
		if (state.hasProperty(BedBlock.FACING)) {
			BlockPos head = foot.relative(state.getValue(BedBlock.FACING));
			for (ServerPlayer player : level.players()) {
				PandoricalApi.bannerDecals().clear(player, head);
			}
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

	/**
	 * The blanket entities sitting on this bed, and only this bed.
	 *
	 * <p>Every blanket sits exactly on its bed's seam, so the box is tight. It used to be a
	 * block wide in every direction, and two beds side by side are one block apart: laying the
	 * second took the first's blanket with it, pattern and all.
	 */
	private static List<Display.ItemDisplay> blanketsOn(ServerLevel level, BlockPos foot, BlockState state) {
		Vec3 middle = middleOf(foot, state);

		return level.getEntitiesOfClass(Display.ItemDisplay.class,
			new AABB(middle, middle).inflate(0.1),
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

	/**
	 * Face up, its top at the pillow, stretched to the bed's two-by-one.
	 *
	 * <p>Built around the flag rather than the item: the flag is centred on the entity, laid on
	 * its back so the pole ends up underneath it, and only then turned to the bed. The pole and
	 * its bar go with it, into the mattress, where nothing sees them.
	 */
	private static Transformation lyingFlat(BlockState state) {
		// A quarter turn forward on x lays the flag face up, its top to +z; the y turn then
		// carries +z round to the head of the bed. The flag's outer face, the one that reads the
		// right way round, was away from the pole and is now up.
		float toHead = -state.getValue(BedBlock.FACING).toYRot();
		Quaternionf turn = new Quaternionf()
			.rotateY((float) Math.toRadians(toHead))
			.rotateX((float) Math.toRadians(90));
		Vector3f stretch = new Vector3f(ACROSS / FLAG_WIDE, ALONG / FLAG_LONG, 1F);

		// The transform scales, then turns, then moves. The flag's centre goes through the first
		// two and the move is whatever brings it back to the entity.
		Vector3f offset = new Vector3f(FLAG_CENTRE).mul(stretch).rotate(turn).negate();

		return new Transformation(offset, turn, stretch, null);
	}

	private static ItemStack bannerFor(ItemStack bed) {
		ItemStack banner = new ItemStack(BedBanners.bannerFor(bed.getItem()));
		var layers = bed.get(DataComponents.BANNER_PATTERNS);
		if (layers != null) banner.set(DataComponents.BANNER_PATTERNS, layers);
		// A Pandorical client draws the decal instead and leaves this display undrawn.
		CustomData.update(DataComponents.CUSTOM_DATA, banner,
			tag -> tag.putBoolean(BannerDecalApi.HIDDEN_ITEM_KEY, true));

		return banner;
	}
}

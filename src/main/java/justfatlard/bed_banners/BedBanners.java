package justfatlard.bed_banners;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BedBlock;

/** What counts as a beddable bed, and which banner stands in for it. */
public final class BedBanners {
	private BedBanners() {}

	private static Map<Item, Item> bannerForBed;

	/**
	 * Whether the loom should take this.
	 *
	 * <p>Dyed beds only. A straw bed has no colour for a banner to stand in for, and the whole
	 * visual rests on there being a banner of the same colour underneath the pattern.
	 */
	public static boolean isPatternable(ItemStack stack) {
		return bannerFor(stack.getItem()) != null;
	}

	/** Whether a placed bed is carrying a pattern worth drawing. */
	public static boolean isPatterned(ItemStack stack) {
		return isPatternable(stack)
			&& !stack.getOrDefault(DataComponents.BANNER_PATTERNS,
				net.minecraft.world.level.block.entity.BannerPatternLayers.EMPTY).layers().isEmpty();
	}

	/** The banner that wears this bed's colour, or null when the item is not a dyed bed. */
	public static Item bannerFor(Item bed) {
		if (bannerForBed == null) bannerForBed = buildTable();

		return bannerForBed.get(bed);
	}

	/**
	 * Bed item to banner item, paired by colour name.
	 *
	 * <p>Built from the registry rather than written out, so the sixteen stay in step with each
	 * other and a seventeenth dye would need no edit here. A bed block that is not a
	 * {@code BedBlock} - the straw one - is skipped on the way past.
	 */
	private static Map<Item, Item> buildTable() {
		Map<Item, Item> table = new HashMap<>();

		for (DyeColor colour : DyeColor.values()) {
			Item bed = BuiltInRegistries.ITEM.getValue(
				Identifier.withDefaultNamespace(colour.getSerializedName() + "_bed"));
			Item banner = BuiltInRegistries.ITEM.getValue(
				Identifier.withDefaultNamespace(colour.getSerializedName() + "_banner"));

			if (bed instanceof BlockItem placed && placed.getBlock() instanceof BedBlock
					&& banner != null && banner != Items.AIR) {
				table.put(bed, banner);
			}
		}
		return table;
	}
}

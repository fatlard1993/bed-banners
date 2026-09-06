package justfatlard.bed_banners.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import justfatlard.bed_banners.BedBanners;
import net.minecraft.client.gui.screens.inventory.LoomScreen;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Lets the loom screen draw a bed's preview.
 *
 * <p>The screen asks the banner slot for its item and casts it to {@code BannerItem} to learn the
 * base colour of the flag it previews. A bed is a {@code BlockItem}, so the cast threw and the
 * client went down the moment a pattern was clicked. Handing the screen the banner of the bed's
 * colour instead answers the only question it asks - which colour - and the preview then shows
 * the pattern on a flag of that colour, which is what the crafted bed's blanket becomes.
 */
@Mixin(LoomScreen.class)
public abstract class LoomScreenBedMixin {

	@Redirect(method = "extractBackground",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getItem()Lnet/minecraft/world/item/Item;"))
	private Item bedBanners$bannerOfBed(ItemStack stack) {
		Item banner = BedBanners.bannerFor(stack.getItem());
		return banner != null ? banner : stack.getItem();
	}
}

package justfatlard.bed_banners.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import justfatlard.bed_banners.BedBanners;
import net.minecraft.world.item.ItemStack;

/**
 * Lets a bed into the loom's banner slot.
 *
 * <p>This is the whole of the crafting half, because vanilla's loom never actually cared that it
 * was working on a banner. Its result is built by copying whatever is in that slot and appending
 * a layer to the stack's {@code banner_patterns} component, and the pattern list it offers is
 * read off the same component. Both are generic already. Only the slot said no.
 *
 * <p><b>Targeted by inner class number, which is the fragile part.</b> {@code LoomMenu$3} is the
 * banner slot on this version, confirmed by it being the only one of the four whose
 * {@code mayPlace} references {@code BannerItem}. If the loom's constructor is ever reordered
 * this attaches to the wrong slot, so the mixin is required: a miss is a refusal to start rather
 * than a dye slot that quietly accepts beds. Re-check with javap on a version bump.
 */
@Mixin(targets = "net.minecraft.world.inventory.LoomMenu$3")
public class LoomBannerSlotMixin {

	@Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
	private void bedBanners$alsoBeds(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		if (BedBanners.isPatternable(stack)) cir.setReturnValue(true);
	}
}

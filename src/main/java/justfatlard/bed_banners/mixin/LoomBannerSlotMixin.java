package justfatlard.bed_banners.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import justfatlard.bed_banners.BedBanners;
import justfatlard.bed_banners.access.LoomBannerSlotOwner;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Lets a bed into the loom's banner slot, for a player whose client can draw it there.
 *
 * <p>This is the whole of the crafting half, because vanilla's loom never actually cared that it
 * was working on a banner. Its result is built by copying whatever is in that slot and appending
 * a layer to the stack's {@code banner_patterns} component, and the pattern list it offers is
 * read off the same component. Both are generic already. Only the slot said no.
 *
 * <p>The loom screen did care: it casts the slot's item to a banner for its preview and throws on
 * a bed the moment a pattern is picked. Pandorical's client half guards that, so the slot only
 * opens for a player who completed the Pandorical handshake ({@link BedBanners#canPatternBeds}).
 * For anyone else it keeps saying no, which is what their client already predicts: the bed stays
 * on the cursor, with no ghost item and nothing for the screen to choke on. Every way into the
 * slot - a click, a drag, a number-key swap - asks {@code mayPlace}, so this is the one gate. A
 * shift-click never gets here with a bed: vanilla's {@code quickMoveStack} only sends a
 * {@code BannerItem} to this slot.
 *
 * <p><b>Targeted by inner class number, which is the fragile part.</b> {@code LoomMenu$3} is the
 * banner slot on this version, confirmed by it being the only one of the four whose
 * {@code mayPlace} references {@code BannerItem}. If the loom's constructor is ever reordered
 * this attaches to the wrong slot, so the mixin is required: a miss is a refusal to start rather
 * than a dye slot that quietly accepts beds. Re-check with javap on a version bump.
 */
@Mixin(targets = "net.minecraft.world.inventory.LoomMenu$3")
public class LoomBannerSlotMixin implements LoomBannerSlotOwner {

	/** Set by {@link LoomMenuMixin} as the menu is built; the slot class holds no menu of its own. */
	@Unique
	private Player bedBanners$owner;

	@Override
	public void bedBanners$setOwner(Player player) {
		this.bedBanners$owner = player;
	}

	@Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
	private void bedBanners$alsoBeds(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		if (BedBanners.isPatternable(stack) && BedBanners.canPatternBeds(bedBanners$owner)) {
			cir.setReturnValue(true);
		}
	}
}

package justfatlard.bed_banners.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import justfatlard.bed_banners.access.LoomBannerSlotOwner;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.inventory.Slot;

/**
 * Tells the loom's banner slot whose loom it is.
 *
 * <p>On the three-argument constructor, which every loom goes through: the two-argument one the
 * client opens with calls it.
 */
@Mixin(LoomMenu.class)
public class LoomMenuMixin {

	@Shadow @Final private Slot bannerSlot;

	@Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",
		at = @At("TAIL"))
	private void bedBanners$ownTheBannerSlot(int containerId, Inventory inventory, ContainerLevelAccess access,
			CallbackInfo ci) {
		if (bannerSlot instanceof LoomBannerSlotOwner owned) owned.bedBanners$setOwner(inventory.player);
	}
}

package justfatlard.bed_banners.access;

import net.minecraft.world.entity.player.Player;

/**
 * The player whose loom a banner slot belongs to.
 *
 * <p>A slot only ever sees the stack it is offered, and this one's class keeps no reference to
 * the menu it sits in, so the menu hands it its player as it is built. Kept out of the mixin
 * package: Mixin refuses to load any other class from a package a mixin config owns.
 */
public interface LoomBannerSlotOwner {
	void bedBanners$setOwner(Player player);
}

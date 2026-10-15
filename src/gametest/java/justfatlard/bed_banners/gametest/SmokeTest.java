package justfatlard.bed_banners.gametest;

import justfatlard.pandorical.api.PandoricalApi;
import justfatlard.pandorical.gametest.Smoke;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * This mod loads, its mixins bind, and a world comes up with it installed - and the loom takes a
 * bed only from a player whose client can draw one there.
 *
 * <p>Getting into the world proves most of it: a mixin that no longer applies refuses the mod
 * outright, a registration that throws takes the client down, and a datapack that will not parse
 * fails the world. The loom is checked on the server's own menus, cheap enough for every run.
 */
public final class SmokeTest implements FabricClientGameTest {

	@Override
	public void runTest(ClientGameTestContext context) {
		Smoke.run(context, "bed-banners-justfatlard", session -> session.onServer(server -> {
			ServerPlayer pandorical = session.player();
			ServerPlayer vanilla = FakePlayer.get(pandorical.level());
			if (!PandoricalApi.isAvailable(pandorical)) throw new AssertionError("the test player has no Pandorical");
			if (PandoricalApi.isAvailable(vanilla)) throw new AssertionError("the stand-in vanilla player has Pandorical");

			// A Pandorical client's loom takes a dyed bed, and a banner as ever; never the straw bed.
			LoomMenu theirs = new LoomMenu(0, pandorical.getInventory(), ContainerLevelAccess.NULL);
			expect(theirs.getBannerSlot().mayPlace(new ItemStack(Items.BED.red())), "a Pandorical client's loom refused a red bed");
			expect(theirs.getBannerSlot().mayPlace(new ItemStack(Items.BANNER.red())), "a Pandorical client's loom refused a banner");
			expect(!theirs.getBannerSlot().mayPlace(new ItemStack(Items.STRAW_BED)), "a loom took the straw bed, which has no colour");
			expect(!theirs.getDyeSlot().mayPlace(new ItemStack(Items.BED.red())), "the dye slot took a bed: LoomMenu$3 is no longer the banner slot");

			// A vanilla client's loom refuses the bed, which its screen would crash on, and still takes a banner.
			LoomMenu vanillas = new LoomMenu(0, vanilla.getInventory(), ContainerLevelAccess.NULL);
			expect(!vanillas.getBannerSlot().mayPlace(new ItemStack(Items.BED.red())), "a vanilla client's loom took a bed");
			expect(vanillas.getBannerSlot().mayPlace(new ItemStack(Items.BANNER.red())), "a vanilla client's loom refused a banner");

			// Shift-clicking a bed out of the hotbar does not land it in the banner slot either.
			// Hotbar slot 0 is menu slot 31: four loom slots, then 27 of inventory.
			vanilla.getInventory().setItem(0, new ItemStack(Items.BED.red()));
			vanillas.quickMoveStack(vanilla, 31);
			expect(!vanillas.getBannerSlot().hasItem(), "shift-click put a bed in a vanilla client's loom");
			vanilla.getInventory().clearContent();
		}));
	}

	private static void expect(boolean held, String otherwise) {
		if (!held) throw new AssertionError(otherwise);
	}
}

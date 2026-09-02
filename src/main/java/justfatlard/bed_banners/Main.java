package justfatlard.bed_banners;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;

public class Main implements ModInitializer {
	public static final String MOD_ID = "bed-banners-justfatlard";

	@Override
	public void onInitialize() {
		// BEFORE, not AFTER: the blanket has to be read while the bed is still standing, because
		// where to look for it is worked out from the bed's own facing and which half it is.
		PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
			if (!(level instanceof ServerLevel serverLevel)) return true;
			if (!(state.getBlock() instanceof BedBlock)) return true;

			BlockPos foot = BedBlanket.footOf(state, pos);
			if (foot == null) return true;

			// Hand the pattern back with the bed. Dropped here rather than through the loot table
			// because the table has no idea a blanket was ever on it, and a pattern that survives
			// one move but not the next is worse than one that never moved at all.
			if (!player.isCreative()) {
				ItemStack bed = BedBlanket.pickUp(serverLevel, foot, state,
					new ItemStack(state.getBlock()));
				if (BedBanners.isPatterned(bed)) {
					net.minecraft.world.level.block.Block.popResource(serverLevel, pos, bed);
					dropNothingElse(serverLevel, pos, state);
				}
			}

			BedBlanket.strip(serverLevel, foot, state);
			return true;
		});

		System.out.println("[" + MOD_ID + "] Loaded");
	}

	/**
	 * Take the bed out of the world ourselves, so vanilla's own drop does not add a plain one
	 * beside the patterned one we just handed over.
	 */
	private static void dropNothingElse(ServerLevel level, BlockPos pos, BlockState state) {
		BlockPos other = state.getValue(BedBlock.PART) == net.minecraft.world.level.block.state.properties.BedPart.FOOT
			? pos.relative(state.getValue(BedBlock.FACING))
			: pos.relative(state.getValue(BedBlock.FACING).getOpposite());

		level.setBlock(other, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),
			net.minecraft.world.level.block.Block.UPDATE_ALL);
		level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),
			net.minecraft.world.level.block.Block.UPDATE_ALL);
	}
}

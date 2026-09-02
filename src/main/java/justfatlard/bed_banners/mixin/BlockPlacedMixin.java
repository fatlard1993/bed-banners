package justfatlard.bed_banners.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import justfatlard.bed_banners.BedBlanket;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Lays the blanket when a patterned bed is placed.
 *
 * <p>On {@code Block} rather than on the bed, because neither {@code BedBlock} nor
 * {@code AbstractBedBlock} declares {@code setPlacedBy} and a mixin cannot inject into a method a
 * class does not have. The guard is one instanceof on a path that runs once per block placed, and
 * this is the only hook that arrives holding the stack the pattern is written on.
 */
@Mixin(Block.class)
public class BlockPlacedMixin {

	@Inject(method = "setPlacedBy", at = @At("TAIL"))
	private void bedBanners$layTheBlanket(Level level, BlockPos pos, BlockState state,
			LivingEntity placer, ItemStack stack, CallbackInfo ci) {
		if (!(level instanceof ServerLevel serverLevel)) return;
		if (!(state.getBlock() instanceof BedBlock)) return;

		BedBlanket.lay(serverLevel, pos, state, stack);
	}
}

package org.embeddedt.modernfix.common.mixin.perf.mojang_registry_size;

import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;
import org.embeddedt.modernfix.annotation.RequiresMod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

/**
 * Minor mixin to avoid duplicate empty neighbor tables, used when FerriteCore is not present. Won't be enabled in 99% of
 * modded environments but is useful for testing in dev without dragging in Fabric API.
 */
@Mixin(StateHolder.class)
@RequiresMod("!ferritecore")
public class StateHolderMixin {
    private static final Logger LOGGER = LoggerFactory.getLogger("ModernFix/StateHolderMixin");
    private static final Reference2ObjectArrayMap<Property<?>, ?> EMPTY_NEIGHBOURS = new Reference2ObjectArrayMap<>();
    private static boolean hasLoggedFerriteCore = false;

    @Shadow private Map<Property<?>, ?> neighbours;

    /* optimize the case where block has no properties */
    @Inject(method = "populateNeighbours", at = @At("RETURN"), require = 0)
    private void replaceEmptyTable(CallbackInfo ci) {
        // Check if the neighbors map is the actual implementation, not FerriteCore's wrapper
        // FerriteCore replaces this with a crash-on-access map, so we need to detect and skip that
        try {
            if (this.neighbours.isEmpty()) {
                this.neighbours = EMPTY_NEIGHBOURS;
            }
        } catch (UnsupportedOperationException e) {
            // FerriteCore is active and using its own optimization, skip our optimization
            if (!hasLoggedFerriteCore) {
                LOGGER.debug("Disabling ModernFix neighbor table optimization (reason: FerriteCore is present)");
                hasLoggedFerriteCore = true;
            }
        }
    }
}

package org.embeddedt.modernfix.common.mixin.feature.branding;

import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import net.minecraft.resources.ResourceLocation;
import org.embeddedt.modernfix.annotation.ClientOnlyMixin;
import org.embeddedt.modernfix.screen.ModernFixDebugScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ClientOnlyMixin
@Mixin(DebugScreenEntries.class)
public class DebugScreenEntriesMixin {
    @ModifyVariable(method = "<clinit>", at = @At(value = "STORE"), ordinal = 0)
    private static Map<ResourceLocation, DebugScreenEntryStatus> insertIntoDefaultProfile(Map<ResourceLocation, DebugScreenEntryStatus> map) {
        // Map is immutable, so we need to build a new one with the added entry
        var stream = map.entrySet().stream();
        var new_map = Map.of(ModernFixDebugScreen.MODERNFIX_ENTRY, DebugScreenEntryStatus.IN_F3).entrySet().stream();

        return Stream.concat(stream, new_map).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}

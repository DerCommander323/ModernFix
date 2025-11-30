package org.embeddedt.modernfix.common.mixin.feature.branding;

import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.resources.ResourceLocation;
import org.embeddedt.modernfix.annotation.ClientOnlyMixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Map;

@ClientOnlyMixin
@Mixin(DebugScreenEntries.class)
public interface DebugScreenEntriesInvoker {
    @Invoker("register")
    static ResourceLocation mfix$register(ResourceLocation name, DebugScreenEntry entry) {
        throw new RuntimeException("Invoker mixin didn't work?");
    };
}

package org.embeddedt.modernfix.common.mixin.perf.dynamic_resources;

import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.resources.model.ModelManager;
import org.embeddedt.modernfix.annotation.ClientOnlyMixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@ClientOnlyMixin
@Mixin(ModelManager.class)
public interface ModelManagerAccessor {
    @Accessor("playerSkinRenderCache")
    PlayerSkinRenderCache mfix$getPlayerSkinRenderCache();
}

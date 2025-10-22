package org.embeddedt.modernfix.neoforge.mixin.perf.dynamic_resources;

import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.world.item.Item;
import net.neoforged.fml.ModLoader;
import org.embeddedt.modernfix.ModernFix;
import org.embeddedt.modernfix.annotation.ClientOnlyMixin;
import org.embeddedt.modernfix.dynamicresources.DynamicModelProvider;
import org.embeddedt.modernfix.neoforge.dynresources.ModelBakeEventHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collections;
import java.util.Iterator;

@Mixin(ModelManager.class)
@ClientOnlyMixin
public class ModelManagerMixinNeo {
    /*
    Disable dynamic baking result modification for NeoForge due to compatibility issues with 1.21.10.
    Commenting out the code instead of deleting for potential future reference.
    Method "lambda$loadModels$18" is not available in NeoForge 1.21.10, causing mixin application failures.
    "lambda$loadModels$18" changed to "lambda$loadModels$14" in NeoForge 1.21.10, but it also changed the signature and parameters.
    */
    @ModifyArg(
        method = "lambda$loadModels$14",
        at = @At(
            value = "INVOKE",
            target = "Lnet/neoforged/neoforge/client/ClientHooks;onModifyBakingResult(Lnet/minecraft/client/resources/model/ModelBakery$BakingResult;Ljava/util/Map;Lnet/minecraft/client/resources/model/ModelBakery;)V"
        ),
        remap = false,
        index = 0 // still the first arg: BakingResult
    )
    private static ModelBakery.BakingResult useDynamicBakingResult(ModelBakery.BakingResult bakingResult) {
        var currentReloadingProvider = DynamicModelProvider.currentReloadingModelProvider.get();
        if (ModLoader.hasErrors() || currentReloadingProvider == null) {
            ModernFix.LOGGER.error("Errors encountered - not using dynamic model BakingResult");
            return bakingResult;
        }

        return new ModelBakeEventHelper(currentReloadingProvider).createDynamicResult();
    }

    /**
     * @author DerCommander323
     * @reason stop NeoForge from iterating over registered items to warn about missing models, as it always fails
     *  with dynamic resources enabled
     */
    @Redirect(method = "apply", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/DefaultedRegistry;iterator()Ljava/util/Iterator;"), require = 0)
    private static Iterator<Item> iterateItemRegistry(DefaultedRegistry<Item> registry) {
        return Collections.emptyIterator();
    }
}

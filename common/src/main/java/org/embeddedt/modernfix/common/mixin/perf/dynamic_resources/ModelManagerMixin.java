package org.embeddedt.modernfix.common.mixin.perf.dynamic_resources;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.resources.model.AtlasManager;
import net.minecraft.client.resources.model.BlockStateModelLoader;
import net.minecraft.client.resources.model.ClientItemInfoLoader;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.ArrayUtils;
import org.embeddedt.modernfix.annotation.ClientOnlyMixin;
import org.embeddedt.modernfix.duck.IModelHoldingBlockState;
import org.embeddedt.modernfix.dynamicresources.DynamicModelProvider;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(ModelManager.class)
@ClientOnlyMixin
public class ModelManagerMixin implements DynamicModelProvider.ModelManagerExtension {
    @Shadow private Map<ResourceLocation, ItemModel> bakedItemStackModels;
    @Shadow private Map<ResourceLocation, ClientItem.Properties> itemProperties;

    @Shadow
    @Final
    private AtlasManager atlasManager;
    @Unique
    private DynamicModelProvider mfix$modelProvider;

    @Redirect(method = "reload", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/model/ModelManager;loadBlockModels(Lnet/minecraft/server/packs/resources/ResourceManager;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;"))
    private CompletableFuture<Map<ResourceLocation, BlockModel>> deferBlockModelLoad(ResourceManager manager, Executor executor) {
        return CompletableFuture.completedFuture(Map.of());
    }

    @Redirect(method = "reload", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/model/BlockStateModelLoader;loadBlockStates(Lnet/minecraft/server/packs/resources/ResourceManager;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;"))
    private CompletableFuture<BlockStateModelLoader.LoadedModels> deferBlockStateLoad(ResourceManager resourceManager, Executor executor) {
        return CompletableFuture.completedFuture(new BlockStateModelLoader.LoadedModels(Map.of()));
    }

    /**
     * @author embeddedt
     * @reason disable map creation, use dynamic dispatch
     */
    @Overwrite
    private static Map<BlockState, BlockStateModel> createBlockStateToModelDispatch(Map<BlockState, BlockStateModel> map, BlockStateModel missingModel) {
        var dynamicProvider = Objects.requireNonNull(DynamicModelProvider.currentReloadingModelProvider.get());

        return dynamicProvider.getFastTopLevelEmulatedRegistry();
    }

    @Redirect(method = "reload", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/model/ClientItemInfoLoader;scheduleLoad(Lnet/minecraft/server/packs/resources/ResourceManager;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;"))
    private CompletableFuture<ClientItemInfoLoader.LoadedClientInfos> disableClientItemEarlyLoad(ResourceManager resourceManager, Executor executor) {
        return CompletableFuture.completedFuture(new ClientItemInfoLoader.LoadedClientInfos(Map.of()));
    }

    @ModifyExpressionValue(method = "reload", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/model/AtlasManager$PendingStitchResults;get(Lnet/minecraft/resources/ResourceLocation;)Ljava/util/concurrent/CompletableFuture;"))
    private CompletableFuture<SpriteLoader.Preparations> storePreparationsFuture(
            CompletableFuture<SpriteLoader.Preparations> original,
            @Share("preparationsFuture") LocalRef<CompletableFuture<SpriteLoader.Preparations>> sharedPreparationsFuture
    ) {
        sharedPreparationsFuture.set(original);
        return original;
    }

    @ModifyArg(method = "reload", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/CompletableFuture;allOf([Ljava/util/concurrent/CompletableFuture;)Ljava/util/concurrent/CompletableFuture;", ordinal = 1))
    public CompletableFuture<?>[] createModelProviderCommon(
            CompletableFuture<?>[] cfs,
            @Local(ordinal = 0) CompletableFuture<EntityModelSet> entityModelFuture,
            @Share("preparationsFuture") LocalRef<CompletableFuture<SpriteLoader.Preparations>> sharedPreparationsFuture
    ) {
        CompletableFuture<Void> makeModelProviderFuture = entityModelFuture
                .thenAcceptBoth(sharedPreparationsFuture.get(), (entityModelSet, preparations) -> {
                    this.mfix$modelProvider = new DynamicModelProvider(
                            Minecraft.getInstance().getResourceManager(),
                            entityModelSet,
                            preparations,
                            ((ModelManagerAccessor) this).mfix$getPlayerSkinRenderCache(),
                            this.atlasManager
                    );
                    DynamicModelProvider.currentReloadingModelProvider = new WeakReference<>(this.mfix$modelProvider);
                });
        return ArrayUtils.add(cfs, makeModelProviderFuture);
    }

    @Inject(method = "apply", at = @At("RETURN"))
    private void setModelRegistries(CallbackInfo ci) {
        this.bakedItemStackModels = this.mfix$modelProvider.getItemModelEmulatedRegistry();
        this.itemProperties = this.mfix$modelProvider.getItemPropertiesEmulatedRegistry();
        for(Block block : BuiltInRegistries.BLOCK) {
            for(BlockState state : block.getStateDefinition().getPossibleStates()) {
                if(state instanceof IModelHoldingBlockState modelHolder) {
                    modelHolder.mfix$setModel(null);
                }
            }
        }
    }

    @Override
    public DynamicModelProvider mfix$getModelProvider() {
        return this.mfix$modelProvider;
    }
}

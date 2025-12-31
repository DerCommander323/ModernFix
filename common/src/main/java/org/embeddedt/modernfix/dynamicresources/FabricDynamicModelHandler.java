package org.embeddedt.modernfix.dynamicresources;

import net.fabricmc.fabric.api.client.model.loading.v1.*;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.embeddedt.modernfix.ModernFix;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class FabricDynamicModelHandler implements DynamicModelProvider.DynamicModelPlugin {
    // Borrowed from Fabric API, this dispatching logic is extremely trivial

    private static final Identifier[] MODEL_MODIFIER_PHASES = new Identifier[] { ModelModifier.OVERRIDE_PHASE, ModelModifier.DEFAULT_PHASE, ModelModifier.WRAP_PHASE, ModelModifier.WRAP_LAST_PHASE };

    private final Event<ModelModifier.OnLoad> onLoadModifiers = EventFactory.createWithPhases(ModelModifier.OnLoad.class, modifiers -> (model, context) -> {
        for (ModelModifier.OnLoad modifier : modifiers) {
            try {
                model = modifier.modifyModelOnLoad(model, context);
            } catch (Exception exception) {
                ModernFix.LOGGER.error("Failed to modify unbaked model on load", exception);
            }
        }

        return model;
    }, MODEL_MODIFIER_PHASES);

    private final Event<ModelModifier.OnLoadBlock> onLoadBlockModifiers = EventFactory.createWithPhases(ModelModifier.OnLoadBlock.class, modifiers -> (model, context) -> {
        for (ModelModifier.OnLoadBlock modifier : modifiers) {
            try {
                model = modifier.modifyModelOnLoad(model, context);
            } catch (Exception exception) {
                ModernFix.LOGGER.error("Failed to modify unbaked block model on load", exception);
            }
        }

        return model;
    }, MODEL_MODIFIER_PHASES);

    // Block modifiers
    private final Event<ModelModifier.BeforeBakeBlock> beforeBakeBlockModifiers = EventFactory.createWithPhases(ModelModifier.BeforeBakeBlock.class, modifiers -> (model, context) -> {
        for (ModelModifier.BeforeBakeBlock modifier : modifiers) {
            try {
                model = modifier.modifyModelBeforeBake(model, context);
            } catch (Exception exception) {
                ModernFix.LOGGER.error("Failed to modify unbaked block model before bake", exception);
            }
        }

        return model;
    }, MODEL_MODIFIER_PHASES);
    private final Event<ModelModifier.AfterBakeBlock> afterBakeBlockModifiers = EventFactory.createWithPhases(ModelModifier.AfterBakeBlock.class, modifiers -> (model, context) -> {
        for (ModelModifier.AfterBakeBlock modifier : modifiers) {
            try {
                model = modifier.modifyModelAfterBake(model, context);
            } catch (Exception exception) {
                ModernFix.LOGGER.error("Failed to modify baked block model after bake", exception);
            }
        }

        return model;
    }, MODEL_MODIFIER_PHASES);

    // Item modifiers
    private final Event<ModelModifier.BeforeBakeItem> beforeBakeItemModifiers = EventFactory.createWithPhases(ModelModifier.BeforeBakeItem.class, modifiers -> (model, context) -> {
        for (ModelModifier.BeforeBakeItem modifier : modifiers) {
            try {
                model = modifier.modifyModelBeforeBake(model, context);
            } catch (Exception exception) {
                ModernFix.LOGGER.error("Failed to modify unbaked item model before bake", exception);
            }
        }

        return model;
    }, MODEL_MODIFIER_PHASES);
    private final Event<ModelModifier.AfterBakeItem> afterBakeItemModifiers = EventFactory.createWithPhases(ModelModifier.AfterBakeItem.class, modifiers -> (model, context) -> {
        for (ModelModifier.AfterBakeItem modifier : modifiers) {
            try {
                model = modifier.modifyModelAfterBake(model, context);
            } catch (Exception exception) {
                ModernFix.LOGGER.error("Failed to modify baked item model after bake", exception);
            }
        }

        return model;
    }, MODEL_MODIFIER_PHASES);


    record PreparablePluginData<T>(CompletableFuture<T> data, PreparableModelLoadingPlugin.Holder<T> plugin) {
        void initialize(ModelLoadingPlugin.Context context) {
            plugin.plugin().initialize(data.join(), context);
        }
    }

    private <T> PreparablePluginData<T> makeDataRecord(PreparableReloadListener.SharedState state, PreparableModelLoadingPlugin.Holder<T> holder) {
        return new PreparablePluginData<>(holder.loader().load(state, ModernFix.resourceReloadExecutor()), holder);
    }

    public FabricDynamicModelHandler(DynamicModelProvider provider, PreparableReloadListener.SharedState sharedState) {
        List<ModelLoadingPlugin> pluginList = new ArrayList<>(ModelLoadingPlugin.getAll());
        var preparablePluginData = new ArrayList<PreparablePluginData<?>>();
        for (var holder : PreparableModelLoadingPlugin.getAll()) {
            preparablePluginData.add(makeDataRecord(sharedState, holder));
        }
        // Wait for all the preparable plugins to finish loading
        CompletableFuture.allOf(preparablePluginData.stream().map(PreparablePluginData::data).toArray(CompletableFuture[]::new)).join();
        var context = new PluginContext(provider);
        for (var plugin : pluginList) {
            plugin.initialize(context);
        }
        for (var data : preparablePluginData) {
            data.initialize(context);
        }
        context.fireResolvers();
    }

    @Override
    public Optional<UnbakedModel> modifyModelOnLoad(Optional<UnbakedModel> modelOpt, Identifier id) {
        return modelOpt.map(model -> this.onLoadModifiers.invoker().modifyModelOnLoad(model, () -> id));
    }

    @Override
    public BlockStateModel.UnbakedRoot modifyBlockModelOnLoad(BlockStateModel.UnbakedRoot model, BlockState state) {
        return this.onLoadBlockModifiers.invoker().modifyModelOnLoad(model, new ModelModifier.OnLoadBlock.Context() {
            @Override
            public BlockState state() {
                return state;
            }
        });
    }

    // Block modifiers
    @Override
    public BlockStateModel.UnbakedRoot modifyBlockModelBeforeBake(BlockStateModel.UnbakedRoot model, BlockState state, ModelBaker baker) {
        return beforeBakeBlockModifiers.invoker().modifyModelBeforeBake(model, new ModelModifier.BeforeBakeBlock.Context() {
            @Override
            public BlockState state() {
                return state;
            }

            @Override
            public ModelBaker baker() {
                return baker;
            }
        });
    }

    @Override
    public BlockStateModel modifyBlockModelAfterBake(BlockStateModel bakedModel, BlockStateModel.UnbakedRoot model, BlockState state, ModelBaker baker) {
        return afterBakeBlockModifiers.invoker().modifyModelAfterBake(bakedModel, new ModelModifier.AfterBakeBlock.Context() {
            @Override
            public BlockStateModel.UnbakedRoot sourceModel() {
                return model;
            }

            @Override
            public BlockState state() {
                return state;
            }

            @Override
            public ModelBaker baker() {
                return baker;
            }
        });
    }


    // Item modifiers
    @Override
    public ItemModel.Unbaked modifyItemModelBeforeBake(ItemModel.Unbaked model, Identifier id, ItemModel.BakingContext bakingContext) {
        return beforeBakeItemModifiers.invoker().modifyModelBeforeBake(model, new ModelModifier.BeforeBakeItem.Context() {
            @Override
            public Identifier itemId() {
                return id;
            }

            @Override
            public ItemModel.BakingContext bakeContext() {
                return bakingContext;
            }
        });
    }

    @Override
    public ItemModel modifyItemModelAfterBake(ItemModel itemModel, Identifier identifier, ItemModel.Unbaked unbaked, ItemModel.BakingContext bakingContext) {
        return afterBakeItemModifiers.invoker().modifyModelAfterBake(itemModel, new ModelModifier.AfterBakeItem.Context() {
            @Override
            public Identifier itemId() {
                return identifier;
            }

            @Override
            public ItemModel.Unbaked sourceModel() {
                return unbaked;
            }

            @Override
            public ItemModel.BakingContext bakeContext() {
                return bakingContext;
            }
        });
    }



    private class PluginContext implements ModelLoadingPlugin.Context {
        private final DynamicModelProvider provider;
        private final Map<Block, BlockStateResolver> resolvers = new HashMap<>();

        private PluginContext(DynamicModelProvider provider) {
            this.provider = provider;
        }

        @Override
        public void registerBlockStateResolver(Block block, BlockStateResolver resolver) {
            resolvers.put(block, resolver);
        }

        @Override
        public <T> void addModel(ExtraModelKey<T> extraModelKey, UnbakedExtraModel<T> unbakedExtraModel) {
            /* no-op on dynamic model loader */
        }

        public void fireResolvers() {
            resolvers.forEach((block, resolver) -> {
                resolver.resolveBlockStates(new BlockStateResolver.Context() {
                    @Override
                    public Block block() {
                        return block;
                    }

                    @Override
                    public void setModel(BlockState blockState, BlockStateModel.UnbakedRoot unbakedRoot) {
                        // provider.addUnbakedBlockStateOverride(BlockModelShaper.stateToModelIdentifier(state), model);
                    }

                });
            });
        }

        @Override
        public Event<ModelModifier.OnLoad> modifyModelOnLoad() {
            return onLoadModifiers;
        }

        @Override
        public Event<ModelModifier.OnLoadBlock> modifyBlockModelOnLoad() {
            return onLoadBlockModifiers;
        }

        @Override
        public Event<ModelModifier.BeforeBakeBlock> modifyBlockModelBeforeBake() {
            return beforeBakeBlockModifiers;
        }

        @Override
        public Event<ModelModifier.AfterBakeBlock> modifyBlockModelAfterBake() {
            return afterBakeBlockModifiers;
        }

        @Override
        public Event<ModelModifier.BeforeBakeItem> modifyItemModelBeforeBake() {
            return beforeBakeItemModifiers;
        }

        @Override
        public Event<ModelModifier.AfterBakeItem> modifyItemModelAfterBake() {
            return afterBakeItemModifiers;
        }
    }
}
package org.embeddedt.modernfix.common.mixin.bugfix.chunk_deadlock;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Entity.class)
public class EntityMixin {
    /**
     * @author embeddedt
     * @reason When an entity is added to the world via the worldgen load path (ChunkMap#postLoadProtoChunk calling
     * ServerLevel#addWorldGenChunkEntities), attempts to add a passenger result in a deadlock when the sculk event
     * tries to raytrace blocks. To fix this, we skip firing the sculk event if the chunk the entity is within is not
     * loaded.
     * 
     * NOTE: Disabled for MC 1.21.9 - the target method call pattern has changed and may no longer be needed
     */
    // TODO: Re-evaluate this mixin for MC 1.21.9 - method signature or call pattern may have changed
    /*
    @WrapWithCondition(method = "addPassenger", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;gameEvent(Lnet/minecraft/core/Holder;Lnet/minecraft/world/entity/Entity;)V"))
    private boolean onlyAddIfSelfChunkLoaded(Entity instance, Holder<GameEvent> gameEvent, Entity entity) {
        var chunkPos = instance.chunkPosition();
        if (instance.level() instanceof ServerLevel serverLevel && serverLevel.getChunkSource().getChunkNow(chunkPos.x, chunkPos.z) == null) {
            ModernFix.LOGGER.warn("Skipped emitting ENTITY_MOUNT game event for entity {} as it would cause deadlock", instance.toString());
            return false;
        } else {
            return true;
        }
    }
    */
}

package com.celik.kingdoms.mixin;
import com.celik.kingdoms.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Block.class)
public abstract class ArtifactBlockMixin {
 @Inject(method="getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemInstance;)Ljava/util/List;",at=@At("RETURN"),cancellable=true)
 private static void drops(BlockState state,ServerLevel level,BlockPos pos,BlockEntity entity,Entity breaker,ItemInstance tool,CallbackInfoReturnable<List<ItemStack>> cir){if(KingdomsMod.service!=null)cir.setReturnValue(KingdomsMod.service.artifacts.drops(level,pos,cir.getReturnValue()));}
}

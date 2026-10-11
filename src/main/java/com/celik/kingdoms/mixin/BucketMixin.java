package com.celik.kingdoms.mixin;
import com.celik.kingdoms.*;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.*;
import net.minecraft.world.level.material.*;
import net.minecraft.world.*;
import net.minecraft.world.phys.*;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(BucketItem.class)
public abstract class BucketMixin {
 @Shadow @Final protected Fluid content;
 @Inject(method="use",at=@At("HEAD"),cancellable=true)
 private void pickup(Level level,Player player,InteractionHand hand,CallbackInfoReturnable<InteractionResult> cir){
  if(content!=Fluids.EMPTY)return;
  Vec3 start=player.getEyePosition(),end=start.add(player.getViewVector(1).scale(player.blockInteractionRange()));
  BlockHitResult hit=level.clip(new ClipContext(start,end,ClipContext.Block.OUTLINE,ClipContext.Fluid.SOURCE_ONLY,player));
  if(hit.getType()==HitResult.Type.BLOCK&&(!Protection.permitted(level,hit.getBlockPos(),player.getUUID(),Rules.Permission.BREAK)||!Protection.flag(level,hit.getBlockPos(),Rules.Flag.FLUIDS)))cir.setReturnValue(InteractionResult.FAIL);
 }
 @Inject(method="emptyContents",at=@At("HEAD"),cancellable=true)
 private void empty(LivingEntity user,Level level,BlockPos pos,BlockHitResult hit,CallbackInfoReturnable<Boolean> cir){
  var context=Protection.current();var actor=Protection.owner(user);if(actor==null&&context!=null)actor=context.actor();
  if(!Protection.flag(level,pos,Rules.Flag.FLUIDS)||(actor!=null&&!Protection.permitted(level,pos,actor,Rules.Permission.PLACE))||(actor==null&&context!=null&&!Protection.environmental(level,context.origin(),pos,Rules.Flag.FLUIDS)))cir.setReturnValue(false);
 }
}

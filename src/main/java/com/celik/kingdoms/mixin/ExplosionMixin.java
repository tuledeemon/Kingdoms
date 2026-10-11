package com.celik.kingdoms.mixin;
import com.celik.kingdoms.*;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(ServerExplosion.class)
public abstract class ExplosionMixin {
 @Shadow @Final private ServerLevel level;
 @Shadow @Final private Entity source;
 @Shadow @Final private Vec3 center;
 @WrapMethod(method="explode")
 private int explode(Operation<Integer> original){
  var existing=Protection.current(); UUID actor=Protection.owner(source);
  if(actor==null&&existing!=null)actor=existing.actor();
  try(var scope=Protection.scope(level,BlockPos.containing(center),actor,source,Protection.Cause.EXPLOSION)){return original.call();}
 }
 @Inject(method="calculateExplodedPositions",at=@At("RETURN"),cancellable=true)
 private void positions(CallbackInfoReturnable<List<BlockPos>> cir){
  var context=Protection.current();UUID actor=context==null?Protection.owner(source):context.actor();
  List<BlockPos> filtered=new ArrayList<>(cir.getReturnValue());
  var service=Protection.service(level);
  filtered.removeIf(pos->service!=null&&service.cores.at(level,pos)!=null||!Protection.explosion(level,BlockPos.containing(center),pos,actor));
  cir.setReturnValue(filtered);
 }
}

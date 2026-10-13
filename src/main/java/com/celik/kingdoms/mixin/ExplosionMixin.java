package com.celik.kingdoms.mixin;
import com.celik.kingdoms.*;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import java.util.UUID;
import java.util.function.BiConsumer;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class ExplosionMixin {
 // Leave ray tracing, blast resistance and other mods' candidate lists untouched.
 // Guard the real block callback before it creates drops, primes TNT or mutates terrain.
 @WrapMethod(method="onExplosionHit")
 private void kingdoms$blockExplosion(ServerLevel level,BlockPos pos,Explosion explosion,BiConsumer<ItemStack,BlockPos> drops,Operation<Void> original){
  var source=explosion.getDirectSourceEntity();
  var existing=Protection.current();UUID actor=Protection.owner(source);
  if(actor==null)actor=Protection.owner(explosion.getIndirectSourceEntity());
  if(actor==null&&existing!=null&&existing.level()==level)actor=existing.actor();
  BlockPos origin=BlockPos.containing(explosion.center());
  if(!Protection.explosion(level,origin,pos,actor))return;
  try(var scope=Protection.scope(level,origin,actor,source,Protection.Cause.EXPLOSION)){
   original.call(level,pos,explosion,drops);
  }
 }
}

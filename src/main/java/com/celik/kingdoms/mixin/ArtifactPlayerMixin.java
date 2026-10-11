package com.celik.kingdoms.mixin;
import com.celik.kingdoms.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.*;
@Mixin(Player.class)
public abstract class ArtifactPlayerMixin {
 @Shadow private boolean canCriticalAttack(Entity target){throw new AssertionError();}
 @WrapMethod(method="attack")
 private void attack(Entity target,Operation<Void> original){Player self=(Player)(Object)this;
  if(KingdomsMod.service==null||!(self instanceof ServerPlayer p)){original.call(target);return;}
  KingdomsMod.service.artifacts.attack(p,target,p.getAttackStrengthScale(.5f)>=1&&canCriticalAttack(target),()->original.call(target));
 }
}

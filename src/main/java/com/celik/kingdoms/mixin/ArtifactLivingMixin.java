package com.celik.kingdoms.mixin;
import com.celik.kingdoms.*;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(LivingEntity.class)
public abstract class ArtifactLivingMixin {
 @WrapMethod(method="hurtServer")
 private boolean hurt(ServerLevel level,DamageSource source,float damage,Operation<Boolean> original){return KingdomsMod.service==null?original.call(level,source,damage):KingdomsMod.service.artifacts.hurt((LivingEntity)(Object)this,level,source,damage,n->original.call(level,source,n));}
 @WrapMethod(method="applyItemBlocking")
 private float block(ServerLevel level,DamageSource source,float incoming,Operation<Float> original){return KingdomsMod.service==null?original.call(level,source,incoming):KingdomsMod.service.artifacts.block((LivingEntity)(Object)this,source,incoming,()->original.call(level,source,incoming));}
}

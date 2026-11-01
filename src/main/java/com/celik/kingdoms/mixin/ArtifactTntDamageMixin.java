package com.celik.kingdoms.mixin;

import com.celik.kingdoms.ArtifactTnt;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ServerExplosion;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerExplosion.class)
public abstract class ArtifactTntDamageMixin {
    @Shadow @Final private Entity source;
    @ModifyExpressionValue(method="hurtEntities",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/ExplosionDamageCalculator;getEntityDamageAmount(Lnet/minecraft/world/level/Explosion;Lnet/minecraft/world/entity/Entity;F)F"))
    private float damage(float vanilla){return source instanceof ArtifactTnt tnt?tnt.kingdoms$damage(vanilla):vanilla;}
}

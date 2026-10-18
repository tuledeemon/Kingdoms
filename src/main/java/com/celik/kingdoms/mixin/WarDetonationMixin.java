package com.celik.kingdoms.mixin;
import com.celik.kingdoms.WarRestrictions;
import net.minecraft.core.*;
import net.minecraft.core.particles.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class WarDetonationMixin {
    @Inject(method="explode",at=@At("HEAD"),cancellable=true)
    private void noExplosion(Entity source,DamageSource damage,ExplosionDamageCalculator calculator,double x,double y,double z,float radius,boolean fire,Level.ExplosionInteraction interaction,ParticleOptions small,ParticleOptions large,WeightedList<ExplosionParticleInfo> particles,Holder<SoundEvent> sound,CallbackInfo ci){
        if(WarRestrictions.preventDetonation((ServerLevel)(Object)this,BlockPos.containing(x,y,z),source))ci.cancel();
    }
}

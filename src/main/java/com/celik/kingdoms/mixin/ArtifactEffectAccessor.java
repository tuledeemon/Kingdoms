package com.celik.kingdoms.mixin;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(MobEffectInstance.class)
public interface ArtifactEffectAccessor {
    @Accessor("hiddenEffect") MobEffectInstance kingdoms$hiddenEffect();
    @Accessor("hiddenEffect") void kingdoms$hiddenEffect(MobEffectInstance value);
}

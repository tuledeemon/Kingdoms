package com.celik.kingdoms.mixin;

import com.celik.kingdoms.ArtifactTnt;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.storage.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PrimedTnt.class)
public abstract class ArtifactTntMixin implements ArtifactTnt {
    @Unique private double kingdoms$multiplier=1,kingdoms$maximum=-1;
    @Override public void kingdoms$policy(double multiplier,double maximum){if(!Double.isFinite(multiplier)||multiplier<0||!Double.isFinite(maximum)||maximum<0)throw new IllegalArgumentException("Invalid TNT policy");kingdoms$multiplier=multiplier;kingdoms$maximum=maximum;}
    @Override public float kingdoms$damage(float vanilla){return kingdoms$maximum<0?vanilla:ArtifactTnt.damage(vanilla,kingdoms$multiplier,kingdoms$maximum);}
    @Inject(method="addAdditionalSaveData",at=@At("TAIL"))
    private void save(ValueOutput out,CallbackInfo ci){if(kingdoms$maximum>=0){out.putDouble("kingdoms_tnt_multiplier",kingdoms$multiplier);out.putDouble("kingdoms_tnt_maximum",kingdoms$maximum);}}
    @Inject(method="readAdditionalSaveData",at=@At("TAIL"))
    private void load(ValueInput in,CallbackInfo ci){double maximum=in.getDoubleOr("kingdoms_tnt_maximum",-1),multiplier=in.getDoubleOr("kingdoms_tnt_multiplier",1);if(Double.isFinite(maximum)&&maximum>=0&&Double.isFinite(multiplier)&&multiplier>=0)kingdoms$policy(multiplier,maximum);}
}

package com.celik.kingdoms.mixin;
import com.celik.kingdoms.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Entity.class)
public abstract class GuardianWebMixin implements GuardianMovement {
 @Shadow protected Vec3 stuckSpeedMultiplier;
 @Unique private boolean kingdoms$lastWeb;
 public void kingdoms$clearWebSlowdown(){if(kingdoms$lastWeb)stuckSpeedMultiplier=Vec3.ZERO;kingdoms$lastWeb=false;}
 @Inject(method="makeStuckInBlock",at=@At("HEAD"),cancellable=true)
 private void web(BlockState state,Vec3 multiplier,CallbackInfo ci){boolean web=state.is(Blocks.COBWEB);if(web&&KingdomsMod.service!=null&&(Object)this instanceof ServerPlayer p&&KingdomsMod.service.artifacts.webImmune(p)){kingdoms$clearWebSlowdown();ci.cancel();return;}kingdoms$lastWeb=web;}
}

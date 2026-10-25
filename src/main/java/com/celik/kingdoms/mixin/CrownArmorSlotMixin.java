package com.celik.kingdoms.mixin;
import com.celik.kingdoms.KingdomsMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ArmorSlot;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArmorSlot.class)
public abstract class CrownArmorSlotMixin {
    @Shadow @Final private LivingEntity owner;
    @Shadow @Final private EquipmentSlot slot;
    @Inject(method="mayPickup",at=@At("HEAD"),cancellable=true)
    private void bound(Player actor,CallbackInfoReturnable<Boolean> ci){if(slot==EquipmentSlot.HEAD&&owner instanceof ServerPlayer p&&KingdomsMod.service!=null&&KingdomsMod.service.artifacts.crown.locked(p))ci.setReturnValue(false);}
}

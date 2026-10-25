package com.celik.kingdoms.mixin;
import com.celik.kingdoms.KingdomsMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Equippable.class)
public abstract class CrownEquippableMixin {
    @Inject(method="swapWithEquipmentSlot",at=@At("HEAD"),cancellable=true)
    private void bound(ItemStack held,Player player,CallbackInfoReturnable<InteractionResult> ci){if(((Equippable)(Object)this).slot()==EquipmentSlot.HEAD&&player instanceof ServerPlayer p&&KingdomsMod.service!=null&&KingdomsMod.service.artifacts.crown.locked(p))ci.setReturnValue(InteractionResult.FAIL);}
}

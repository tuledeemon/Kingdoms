package com.celik.kingdoms.mixin;
import com.celik.kingdoms.*;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.entity.player.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.util.List;
import org.spongepowered.asm.mixin.*;
@Mixin(Inventory.class)
public abstract class ArtifactInventoryMixin {
 @Shadow @Final public Player player;
 @WrapMethod(method="dropAll")
 private void death(Operation<Void> original){if(KingdomsMod.service==null||!(player instanceof ServerPlayer p)){original.call();return;}List<ItemStack> kept=KingdomsMod.service.artifacts.keepOnDeath(p);try{original.call();}finally{KingdomsMod.service.artifacts.returnAfterDeath(p,kept);}}
}

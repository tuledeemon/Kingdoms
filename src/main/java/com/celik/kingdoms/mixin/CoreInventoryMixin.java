package com.celik.kingdoms.mixin;

import com.celik.kingdoms.KingdomCores;
import net.minecraft.world.inventory.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public abstract class CoreInventoryMixin {
    @Inject(method="clicked",at=@At("HEAD"),cancellable=true)
    private void noStorage(int slotIndex,int button,ContainerInput type,Player player,CallbackInfo ci){
        AbstractContainerMenu menu=(AbstractContainerMenu)(Object)this;
        boolean hasStorage=menu.slots.stream().anyMatch(s->s.container!=player.getInventory());
        if(!hasStorage)return;
        Slot slot=slotIndex>=0&&slotIndex<menu.slots.size()?menu.slots.get(slotIndex):null;
        boolean storage=slot!=null&&slot.container!=player.getInventory();
        boolean blocked=storage&&KingdomCores.isToken(menu.getCarried())
            ||type==ContainerInput.QUICK_CRAFT&&KingdomCores.isToken(menu.getCarried())
            ||type==ContainerInput.QUICK_MOVE&&slot!=null&&slot.container==player.getInventory()&&KingdomCores.isToken(slot.getItem())
            ||storage&&type==ContainerInput.SWAP&&button>=0&&button<player.getInventory().getContainerSize()&&KingdomCores.isToken(player.getInventory().getItem(button));
        if(blocked){ci.cancel();menu.broadcastFullState();}
    }
}

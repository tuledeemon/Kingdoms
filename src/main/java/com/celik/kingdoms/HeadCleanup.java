package com.celik.kingdoms;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Small inventory-only joke; no world, entity or container scans. */
final class HeadCleanup {
    static boolean matches(ItemStack stack){
        if(!stack.is(Items.PLAYER_HEAD))return false;
        var profile=stack.get(DataComponents.PROFILE);
        return profile!=null&&profile.name().filter("_Umut"::equalsIgnoreCase).isPresent();
    }
    static void clear(ServerPlayer player){
        boolean changed=false;
        var inventory=player.getInventory();
        for(int slot=0;slot<inventory.getContainerSize();slot++)if(matches(inventory.getItem(slot))){inventory.setItem(slot,ItemStack.EMPTY);changed=true;}
        if(matches(player.containerMenu.getCarried())){player.containerMenu.setCarried(ItemStack.EMPTY);changed=true;}
        if(changed){inventory.setChanged();player.containerMenu.broadcastChanges();}
    }
    private HeadCleanup(){}
}

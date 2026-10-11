package com.celik.kingdoms;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import java.util.*;
import static com.celik.kingdoms.AirDropRules.*;

/** Chest-sized shared contents with exact-once whole-stack transfers. Every click rechecks reach/access. */
public final class AirDropChestMenu extends ChestMenu {
    private final ServerPlayer viewer;
    final UUID drop;
    private final Map<Integer,UUID> rewards=new HashMap<>();
    private int page,ticks;
    private static AirDrops drops(){return KingdomsMod.service.airDrops;}
    public static void open(ServerPlayer player,UUID id){Drop d=drops().drop(id);drops().service.require(drops().chests.canUse(player,d),"Only winners standing at the AirDrop chest can collect its loot.");if(drops().service.needsWarning(player)){Ui.warning(player);return;}player.openMenu(new SimpleMenuProvider((containerId,inventory,p)->new AirDropChestMenu(containerId,inventory,player,id),Ui.text("AirDrop chest • "+d.variantName)));drops().chests.opened(player,id);}
    private AirDropChestMenu(int id,Inventory inventory,ServerPlayer player,UUID drop){super(MenuType.GENERIC_9x3,id,inventory,new SimpleContainer(27),3);viewer=player;this.drop=drop;
        for(int i=0;i<27;i++){Slot old=slots.get(i);Slot locked=new Slot(getContainer(),i,old.x,old.y){@Override public boolean mayPlace(ItemStack stack){return false;}@Override public boolean mayPickup(Player p){return false;}};locked.index=i;slots.set(i,locked);}refresh();
    }
    private void label(int slot,Item item,String label){ItemStack stack=new ItemStack(item);stack.set(DataComponents.CUSTOM_NAME,Ui.text(label));getContainer().setItem(slot,stack);}
    private void refresh(){getContainer().clearContent();rewards.clear();Drop d=drops().drop(drop);boolean paged=d.loot.size()>27;int size=paged?24:27;page=Math.min(page,Math.max(0,(d.loot.size()-1)/size));List<Integer> layout=AirDropRules.chestSlots(d,page,size);for(int i=page*size;i<Math.min(d.loot.size(),(page+1)*size);i++){Reward r=d.loot.get(i);if(r.status!=LootStatus.AVAILABLE&&r.status!=LootStatus.PENDING)continue;int slot=layout.get(i-page*size);ItemStack stack=AirDropLoot.decode(drops().service.server,r.itemJson);stack.set(DataComponents.CUSTOM_NAME,Ui.text((r.money>0?"Cash reward: "+drops().service.money(r.money):stack.getHoverName().getString())+(r.status==LootStatus.PENDING?" • reserved":"")));getContainer().setItem(slot,stack);rewards.put(slot,r.id);}if(paged){if(page>0)label(24,Items.ARROW,"Previous");label(25,Items.CLOCK,"Page "+(page+1)+" • expires "+Ui.clock(d.expiresAt-drops().service.now()));if((page+1)*size<d.loot.size())label(26,Items.ARROW,"Next");}}
    @Override public boolean stillValid(Player player){return player==viewer&&drops().chests.canUse(viewer,AirDrops.find(drops().data,drop));}
    @Override public void clicked(int slot,int button,ContainerInput input,Player player){if(!stillValid(player)){viewer.closeContainer();return;}if(drops().service.needsWarning(viewer)){Ui.warning(viewer);return;}if(input==ContainerInput.CLONE)return;if(slot>=0&&slot<27){if(input!=ContainerInput.PICKUP&&input!=ContainerInput.QUICK_MOVE)return;Ui.run(viewer,()->{UUID reward=rewards.get(slot);if(reward!=null)drops().collect(viewer,drop,reward);else if(slot==24&&page>0)page--;else if(slot==26&&!getContainer().getItem(26).isEmpty())page++;refresh();});broadcastFullState();return;}super.clicked(slot,button,input,player);}
    @Override public ItemStack quickMoveStack(Player player,int slot){if(slot<27){clicked(slot,0,ContainerInput.QUICK_MOVE,player);return ItemStack.EMPTY;}return super.quickMoveStack(player,slot);}
    @Override public boolean canTakeItemForPickAll(ItemStack stack,Slot slot){return slot.index>=27;}
    @Override public void broadcastChanges(){if(!stillValid(viewer)){viewer.closeContainer();return;}if(++ticks%10==0)refresh();super.broadcastChanges();}
    @Override public void removed(Player player){super.removed(player);drops().chests.closed(viewer,drop);}
}

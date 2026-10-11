package com.celik.kingdoms;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import java.util.*;
import static com.celik.kingdoms.AirDropRules.*;

/** Vanilla inventory menus. Loot is shared, server-authorized and never transferable from display slots. */
public final class AirDropUi {
    private static AirDrops drops(){return KingdomsMod.service.airDrops;}
    static void homeButton(ServerPlayer p,Ui.Menu m){
        int slot=drops().config().menuSlot;
        if(!m.getContainer().getItem(slot).isEmpty())for(int i=44;i>=0;i--)if(m.getContainer().getItem(i).isEmpty()){slot=i;break;}
        long active=drops().data.drops.stream().filter(Drop::active).count();
        long won=drops().data.drops.stream().filter(d->d.phase==Phase.CLAIMED&&d.recipients.contains(p.getUUID())&&d.loot.stream().anyMatch(r->r.status==LootStatus.AVAILABLE||r.status==LootStatus.PENDING)).count();
        m.button(slot,Items.CHEST,"AirDrops",List.of("Active: "+active,"Won with uncollected loot: "+won,"Loot day: "+drops().day(),"Compete with your kingdom or capture alone."),()->list(p,0));
    }
    public static void list(ServerPlayer p,int page){Ui.open(p,"AirDrops",m->{
        List<Drop> all=drops().data.drops.stream().filter(d->d.active()||d.phase==Phase.CLAIMED&&d.recipients.contains(p.getUUID())).sorted(Comparator.comparingLong((Drop d)->d.createdAt).reversed()).toList();
        for(int i=page*36;i<Math.min(all.size(),page*36+36);i++){Drop d=all.get(i);List<String> info=new ArrayList<>(List.of(AirDrops.coordinates(d),"Status: "+status(d),"Tier: "+d.tierName));if(d.phase==Phase.CLAIMED){info.add("Winner: "+d.winnerName);info.add("Loot disappears in: "+Ui.clock(d.expiresAt-drops().service.now()));}else info.add("Loot remains sealed until capture.");m.button(i-page*36,Items.CHEST,d.variantName,info,()->detail(p,d.id,0));}
        if(all.isEmpty())m.button(22,Items.CLOCK,"No active AirDrops",List.of("Events require at least "+drops().config().minimumPlayers+" online players.","Default interval: "+drops().config().intervalMinutes+" minutes plus random delay.","Loot age advances in real-world days."),null);
        m.pages(p,page,all.size(),q->list(p,q));m.back(p);
    },false);}
    private static String status(Drop d){return switch(d.phase){case DESCENDING->"Arriving in "+Ui.clock(d.landsAt-drops().service.now());case CAPTURABLE->"Ready to capture • expires in "+Ui.clock(d.expiresAt-drops().service.now());case CLAIMED->"Secured • loot expires in "+Ui.clock(d.expiresAt-drops().service.now());case CANCELLED->"Cancelled";case EXPIRED->"Expired";};}
    public static void detail(ServerPlayer p,UUID id,int page){Ui.open(p,"AirDrop • information",m->{
        Drop d=drops().drop(id);boolean winner=d.phase==Phase.CLAIMED&&d.recipients.contains(p.getUUID());
        List<String> info=new ArrayList<>(List.of(AirDrops.coordinates(d),"Status: "+status(d),"Tier: "+d.tierName));
        if(d.active()){info.add("Capture radius: "+d.captureRadius+" blocks / height ±"+d.verticalRadius);info.add("Build protection: "+d.protectionRadius+" blocks");info.add("Solo control strength: "+d.soloControlStrength);info.add("Only one side captures; "+(d.largerGroupTakeover?"a stronger side takes over after "+d.takeoverMillis/1000+" seconds.":"control changes after a reset."));info.add("Kingdom: "+d.requiredMembers+" members for "+Ui.clock(d.captureMillis));info.add("Solo: "+Ui.clock((long)d.required(AirDropRules.solo(p.getUUID()))));info.add("Rivals slow progress by "+d.interferenceMultiplier+"x.");info.add("Grace period: "+Ui.clock(d.graceMillis));if(d.takeoverSide!=null)info.add("Challenger: "+drops().sideName(d.takeoverSide)+" • takeover in "+Ui.clock(d.takeoverMillis-(drops().service.now()-d.takeoverSince)));Progress progress=d.capturingSide==null?null:d.progress.get(d.capturingSide);if(progress!=null)info.add("Capturing: "+drops().sideName(d.capturingSide)+" • "+Math.round(progress.millis/d.required(d.capturingSide)*100)+"%"+(progress.missingSince!=0?" • paused":progress.contested?" • contested":""));}
        else if(d.winnerName!=null){info.add("Winner: "+d.winnerName);info.add("Loot disappears in: "+Ui.clock(d.expiresAt-drops().service.now()));}
        m.button(40,Items.MAP,d.variantName,info,null);
        if(winner){boolean remaining=d.loot.stream().anyMatch(r->r.status==LootStatus.AVAILABLE||r.status==LootStatus.PENDING);m.button(22,remaining?Items.CHEST:Items.EMERALD,remaining?"Visit the AirDrop chest":"All rewards collected",List.of(remaining?"Right-click the chest at the coordinates above.":"Your side has collected this crate's loot.","Only the winning roster can open its shared contents.","Keep an empty inventory slot to collect each stack."),null);
        }else m.button(22,Items.BARRIER,d.active()?"Loot is sealed":"Winners only",List.of(d.active()?"Control the area after landing to unlock the chest.":"Only the roster recorded when capture finished can open the chest."),null);
        m.button(49,Items.ARROW,"Back",List.of(),()->list(p,0));
    },false);}
    private AirDropUi(){}
}

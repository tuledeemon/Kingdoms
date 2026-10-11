package com.celik.kingdoms;
import java.util.*;
import java.util.function.BiConsumer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import static com.celik.kingdoms.AirDropRules.*;

final class SettingsMoneyIntegration {
    static void run(KingdomService s,ServerPlayer mayor,ServerPlayer enemy,BiConsumer<Boolean,String> check)throws Exception {
        Config saved=s.config;var state=s.store.copy(s.state);var ledger=Store.JSON.fromJson(Store.JSON.toJson(s.airDrops.data),Data.class);
        ItemStack[] inventory=new ItemStack[mayor.getInventory().getContainerSize()];for(int i=0;i<inventory.length;i++)inventory[i]=mayor.getInventory().getItem(i).copy();
        try {
            var commands=s.server.getCommands().getDispatcher();var console=s.server.createCommandSourceStack();
            check.accept(commands.execute("kingdomadmin airdrop set takeoverSeconds 15",console)==1&&s.config.airDrops.takeoverSeconds==15,"AirDrop scalar command validates and saves settings");
            check.accept(commands.execute("kingdomadmin airdrop money 7 100k",console)==1&&s.config.airDrops.money(7)==100000,"cash settings accept compact money aliases");
            check.accept(commands.execute("kingdomadmin airdrop threshold 50 3",console)==1&&s.config.airDrops.dropsFor(50)==3,"player threshold command edits future event counts");
            check.accept(commands.execute("kingdomadmin artifact chance 3 0.15%",console)==1&&s.config.artifacts.chance(3)==.0015,"artifact chance command accepts percentages");
            check.accept(commands.execute("kingdomadmin artifact type thunder_axe cooldownSeconds 35",console)==1&&s.config.artifacts.types.get("thunder_axe").cooldownSeconds==35,"per-type artifact settings are configurable by command");
            check.accept(commands.execute("kingdomadmin artifact variant vampire_sword 1 chance 6%",console)==1&&s.config.artifacts.types.get("vampire_sword").variants.getFirst().chance==.06,"artifact variant settings use one-based variant indices");
            String before=Store.JSON.toJson(s.config);check.accept(commands.execute("kingdomadmin airdrop set captureRadius 0",console)==0&&Store.JSON.toJson(s.config).equals(before),"invalid config command never mutates running configuration");
            check.accept(commands.execute("kingdomadmin artifact chance 1 101%",console)==0&&Store.JSON.toJson(s.config).equals(before),"invalid artifact probabilities leave configuration unchanged");
            var disk=KingdomsMod.configStore.read("config.json",Config.class,new Config());check.accept(disk.airDrops.money(7)==100000&&disk.artifacts.types.get("thunder_axe").cooldownSeconds==35,"command settings persist in the actual config file");
            var kingdom=s.own(mayor.getUUID());var other=s.own(enemy.getUUID());s.edit(kingdom.id,k->{k.core.tributeTo=null;k.treasury=1000;});
            UUID reward=UUID.randomUUID();s.airDropMoney(reward,kingdom.id,mayor.getUUID(),1000);s.airDropMoney(reward,kingdom.id,enemy.getUUID(),1000);
            check.accept(s.state.kingdoms.get(kingdom.id).treasury==2000,"kingdom cash reward credits its treasury once across different collectors");
            long wallet=s.api().balances().getBalance(mayor.getUUID());UUID cash=UUID.randomUUID();s.airDropMoney(cash,null,mayor.getUUID(),1000);s.airDropMoney(cash,null,mayor.getUUID(),1000);
            check.accept(s.api().balances().getBalance(mayor.getUUID())==wallet+1000,"solo cash uses the economy journal and pays the wallet exactly once");
            var homeCore=Store.JSON.fromJson(Store.JSON.toJson(s.state.kingdoms.get(kingdom.id).core),com.celik.kingdoms.Rules.Core.class);long captor=s.state.kingdoms.get(other.id).treasury;s.edit(kingdom.id,k->{k.core.tributeTo=other.id;k.core.dimension=other.core.dimension;k.core.x=other.core.x;k.core.y=other.core.y+1;k.core.z=other.core.z;});long treasury=s.state.kingdoms.get(kingdom.id).treasury;
            s.airDropMoney(UUID.randomUUID(),kingdom.id,mayor.getUUID(),1000);
            check.accept(s.state.kingdoms.get(kingdom.id).treasury==treasury+800&&s.state.kingdoms.get(other.id).treasury==captor+200,"captured kingdom cash routes twenty-percent tribute once");
            s.edit(kingdom.id,k->k.core=homeCore);
            s.config.airDrops=new AirDropConfig();s.config.artifacts.enabled=false;s.config.spawnClaimProtectionChunks=0;
            var level=s.server.overworld();var pos=s.airDrops.landingAt(level,6200,1600);check.accept(pos!=null,"cash test uses an accessible wilderness landing site");
            s.airDrops.spawnTest(level,pos.getX(),pos.getZ(),"mythic",true);Drop d=s.airDrops.data.drops.getLast();
            check.accept(d.loot.stream().filter(r->r.money>0).count()==1,"normal Mythic crate gets exactly one phase-appropriate cash reward");
            d.phase=Phase.CLAIMED;d.winner=kingdom(kingdom.id);d.winnerName=kingdom.name;d.recipients.add(mayor.getUUID());s.store.write("airdrops.json",s.airDrops.data);s.airDrops.chests.sync(s.now());
            mayor.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);for(int slot=0;slot<36;slot++)mayor.getInventory().setItem(slot,new ItemStack(Items.STONE,64));
            Reward money=d.loot.stream().filter(r->r.money>0).findFirst().orElseThrow();long balance=s.state.kingdoms.get(kingdom.id).treasury;
            AirDropChestMenu.open(mayor,d.id);var menu=(AirDropChestMenu)mayor.containerMenu;int cashIndex=d.loot.indexOf(money),cashSlot=chestSlots(d,0,d.loot.size()>27?24:27).get(cashIndex);
            check.accept(menu.getContainer().getItem(cashSlot).getHoverName().getString().contains(Ui.caps("Cash reward")),"real chest renders cash in its saved random slot");
            menu.clicked(cashSlot,0,net.minecraft.world.inventory.ContainerInput.PICKUP,mayor);check.accept(s.state.kingdoms.get(kingdom.id).treasury==balance+money.money&&s.airDrops.drop(d.id).loot.stream().filter(r->r.id.equals(money.id)).findFirst().orElseThrow().status==LootStatus.COLLECTED,"full-inventory winner collects cash directly without an item token");
            mayor.closeContainer();AirDropChestMenu.open(mayor,d.id);menu=(AirDropChestMenu)mayor.containerMenu;int rewardSlot=chestSlots(d,0,d.loot.size()>27?24:27).get(0);check.accept(menu.getContainer().getItem(cashSlot).isEmpty()&&menu.getContainer().getItem(rewardSlot).is(AirDropLoot.decode(s.server,d.loot.getFirst().itemJson).getItem()),"reopening collected chest preserves its empty cash slot and other reward positions");mayor.closeContainer();
            Reward pending=s.airDrops.drop(d.id).loot.stream().filter(r->r.id.equals(money.id)).findFirst().orElseThrow();pending.status=LootStatus.PENDING;s.store.write("airdrops.json",s.airDrops.data);s.airDrops.recoverStorage();
            s.airDrops.recover(mayor);check.accept(s.state.kingdoms.get(kingdom.id).treasury==balance+money.money&&s.airDrops.drop(d.id).loot.stream().filter(r->r.id.equals(money.id)).findFirst().orElseThrow().status==LootStatus.COLLECTED,"crash after treasury commit completes reserved reward without paying twice");
            var reloaded=s.store.read("kingdoms.json",com.celik.kingdoms.Rules.State.class,new com.celik.kingdoms.Rules.State());check.accept(reloaded.airDropMoneyReceipts.contains(money.id),"cash receipt is durably saved with kingdom balances");
            s.config.artifacts=new ArtifactConfig();s.config.artifacts.chanceByDay=new TreeMap<>(Map.of(1,1d));s.config.artifacts.fixerChance=0;
            var artifactPos=s.airDrops.landingAt(level,6800,1600);s.airDrops.spawnTest(level,artifactPos.getX(),artifactPos.getZ(),"standard",true);var artifactDrop=s.airDrops.data.drops.getLast();
            check.accept(artifactDrop.loot.size()==1&&artifactDrop.loot.getFirst().rareKey.startsWith("artifact:")&&artifactDrop.loot.getFirst().money==0,"artifact roll replaces ordinary rewards and never includes cash by default");
        } finally {
            mayor.closeContainer();for(int i=0;i<inventory.length;i++)mayor.getInventory().setItem(i,inventory[i]);s.config=saved;KingdomsMod.configStore.write("config.json",saved);s.commit(state);
            for(Drop d:s.airDrops.data.drops)if(ledger.drops.stream().noneMatch(old->old.id.equals(d.id))){d.phase=Phase.CANCELLED;d.closedAt=s.now();d.loot.forEach(r->{if(r.status!=LootStatus.COLLECTED)r.status=LootStatus.EXPIRED;});}s.airDrops.chests.sync(s.now());s.airDrops.data=ledger;s.store.write("airdrops.json",ledger);
        }
    }
    private SettingsMoneyIntegration() {}
}

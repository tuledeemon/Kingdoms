package com.celik.kingdoms;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.server.network.*;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.*;
import java.util.*;
import java.util.function.BiConsumer;

final class CrownPowerIntegration {
    static void run(KingdomService s,ServerPlayer reference,ArtifactConfig cfg,BiConsumer<Boolean,String> check){
        ServerPlayer wearer=create(s,"CrownWearer",UUID.randomUUID(),reference);wearer.setPos(17120.5,240,17120.5);wearer.level().getChunk(wearer.blockPosition());
        var record=ArtifactIntegration.issue(s.artifacts,cfg,"golden_crown");ItemStack crown=s.artifacts.create(record);
        List<ServerPlayer> fixtures=new ArrayList<>();fixtures.add(wearer);s.server.getPlayerList().getPlayers().add(wearer);
        try{
            check.accept(record.rarity.equals("unique")&&crown.get(DataComponents.CUSTOM_NAME).getStyle().getColor().getValue()==0xFF5555,"crown has Unique rarity and red name");
            record.rarity="mythic";s.artifacts.refresh(crown,record);
            check.accept(record.rarity.equals("unique")&&crown.get(DataComponents.LORE).lines().getFirst().getString().equals(Ui.caps("unique")),"existing crown refresh migrates rarity and lore");
            var head=new ItemStack(Items.PLAYER_HEAD,8);head.set(DataComponents.PROFILE,net.minecraft.world.item.component.ResolvableProfile.createUnresolved("_Umut"));
            var other=new ItemStack(Items.PLAYER_HEAD);other.set(DataComponents.PROFILE,net.minecraft.world.item.component.ResolvableProfile.createUnresolved("OtherPlayer"));
            wearer.getInventory().setItem(10,head);wearer.getInventory().setItem(11,other);wearer.containerMenu.setCarried(head.copy());
            var stored=head.copy();wearer.getEnderChestInventory().setItem(0,stored);HeadCleanup.clear(wearer);
            check.accept(wearer.getInventory().getItem(10).isEmpty()&&wearer.containerMenu.getCarried().isEmpty(),"target heads are cleared from player inventory and cursor");
            check.accept(wearer.getInventory().getItem(11)==other&&wearer.getEnderChestInventory().getItem(0)==stored,"other heads and stored container heads are untouched");
            wearer.getInventory().setItem(11,ItemStack.EMPTY);wearer.getEnderChestInventory().setItem(0,ItemStack.EMPTY);
            wearer.setItemSlot(EquipmentSlot.HEAD,crown);s.artifacts.crown.sync(wearer);
            check.accept(wearer.getMaxHealth()==20&&wearer.hasEffect(MobEffects.GLOWING)&&!wearer.hasEffect(MobEffects.STRENGTH),"crown starts at ten hearts, glows while worn and grants no Strength");
            check.accept(crown.has(DataComponents.UNBREAKABLE),"Golden Crown uses native infinite durability");
            crown.hurtAndBreak(100000,wearer.level(),wearer,item->{});
            check.accept(!crown.isEmpty()&&crown.getDamageValue()==0&&!record.retired,"native durability damage cannot break or wear out the crown");
            check.accept(!wearer.inventoryMenu.getSlot(5).mayPickup(wearer),"survival armor slot refuses crown removal");
            wearer.inventoryMenu.clicked(5,0,ContainerInput.PICKUP,wearer);
            check.accept(wearer.inventoryMenu.getCarried().isEmpty()&&wearer.getItemBySlot(EquipmentSlot.HEAD)==crown,"ordinary click cannot pick up the equipped crown");
            wearer.inventoryMenu.clicked(5,0,ContainerInput.QUICK_MOVE,wearer);wearer.inventoryMenu.clicked(5,1,ContainerInput.THROW,wearer);
            check.accept(wearer.getItemBySlot(EquipmentSlot.HEAD)==crown,"shift-click and drop cannot remove the crown");
            wearer.getInventory().setItem(0,new ItemStack(Items.IRON_HELMET));wearer.inventoryMenu.clicked(5,0,ContainerInput.SWAP,wearer);
            check.accept(wearer.getItemBySlot(EquipmentSlot.HEAD)==crown&&wearer.getInventory().getItem(0).is(Items.IRON_HELMET),"hotbar helmet swap is blocked without moving either item");
            check.accept(wearer.getInventory().removeItem(39,1).isEmpty()&&wearer.getInventory().removeItemNoUpdate(39).isEmpty(),"native inventory removal paths cannot take the bound crown");
            wearer.getInventory().setItem(39,ItemStack.EMPTY);wearer.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);
            check.accept(wearer.getItemBySlot(EquipmentSlot.HEAD)==crown,"direct inventory and equipment replacement cannot remove a live wearer's crown");
            ItemStack helmet=new ItemStack(Items.IRON_HELMET);var equipment=helmet.get(DataComponents.EQUIPPABLE);
            check.accept(equipment.swapWithEquipmentSlot(helmet,wearer)==net.minecraft.world.InteractionResult.FAIL&&wearer.getItemBySlot(EquipmentSlot.HEAD)==crown,"right-click helmet replacement fails before copying or consuming items");
            wearer.gameMode.changeGameModeForPlayer(GameType.CREATIVE);
            check.accept(!wearer.inventoryMenu.getSlot(5).mayPickup(wearer)&&equipment.swapWithEquipmentSlot(helmet,wearer)==net.minecraft.world.InteractionResult.FAIL,"creative mode cannot bypass the crown lock");
            wearer.connection.handleSetCreativeModeSlot(new net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket(5,ItemStack.EMPTY));
            check.accept(wearer.getItemBySlot(EquipmentSlot.HEAD)==crown,"creative inventory packets cannot clear the crown slot");wearer.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
            UUID first=UUID.randomUUID();
            for(int kill=1;kill<=5;kill++){
                UUID id=kill==1?first:UUID.randomUUID();ServerPlayer victim=create(s,"CrownVictim"+kill,id,wearer);fixtures.add(victim);victim.setHealth(0);victim.die(victim.damageSources().playerAttack(wearer));
                check.accept(wearer.getMaxHealth()==20+4*kill&&wearer.getHealth()==20+4*kill,"native unique player death grants two full hearts: kill "+kill);
                if(kill==1){ServerPlayer repeated=create(s,"CrownRepeat",id,wearer);fixtures.add(repeated);repeated.setHealth(0);repeated.die(repeated.damageSources().playerAttack(wearer));check.accept(wearer.getMaxHealth()==24&&s.artifacts.data.crownLives.get(wearer.getUUID()).victims.size()==1,"repeat kills of the same player give no additional hearts");}
            }
            s.artifacts.crown.killed(wearer,UUID.randomUUID());s.artifacts.crown.killed(wearer,wearer.getUUID());
            check.accept(wearer.getMaxHealth()==40&&s.artifacts.data.crownLives.get(wearer.getUUID()).victims.size()==5,"five unique kills cap the crown at forty HP and self kills do not count");
            var saved=s.store.read("artifacts.json",ArtifactRules.Data.class,new ArtifactRules.Data());check.accept(saved.crownLives.get(wearer.getUUID()).victims.size()==5,"unique victims are saved immediately for the wearer's life");
            wearer.removeEffect(MobEffects.GLOWING);s.artifacts.crown.sync(wearer);check.accept(wearer.hasEffect(MobEffects.GLOWING),"removing the glowing effect cannot permanently hide a crown wearer");
            ((com.celik.kingdoms.mixin.AirDropPlayerSaveInvoker)s.server.getPlayerList()).kingdoms$saveAirDropPlayer(wearer);
            var tag=NbtIo.readCompressed(s.server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(wearer.getStringUUID()+".dat"),NbtAccounter.unlimitedHeap());
            s.artifacts.leave(wearer);s.server.getPlayerList().getPlayers().remove(wearer);
            ServerPlayer rejoined=create(s,"CrownWearer",wearer.getUUID(),reference);rejoined.load(TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,rejoined.level().registryAccess(),tag));fixtures.add(rejoined);s.server.getPlayerList().getPlayers().add(rejoined);s.artifacts.crown.sync(rejoined);
            check.accept(rejoined.getMaxHealth()==40&&rejoined.getHealth()==40&&s.artifacts.crown.locked(rejoined),"native player-save reload preserves crown hearts, current health and binding across reconnect");
            rejoined.setHealth(0);rejoined.die(rejoined.damageSources().genericKill());
            check.accept(rejoined.getMaxHealth()==20&&!s.artifacts.data.crownLives.containsKey(rejoined.getUUID())&&rejoined.getItemBySlot(EquipmentSlot.HEAD).isEmpty(),"native wearer death clears all bonus hearts and permits the crown to drop");
            saved=s.store.read("artifacts.json",ArtifactRules.Data.class,new ArtifactRules.Data());check.accept(!saved.crownLives.containsKey(rejoined.getUUID()),"death reset is durable in the artifact save");
            ServerPlayer next=create(s,"CrownNext",UUID.randomUUID(),reference);fixtures.add(next);next.setItemSlot(EquipmentSlot.HEAD,s.artifacts.create(ArtifactIntegration.issue(s.artifacts,cfg,"golden_crown")));s.artifacts.crown.sync(next);
            check.accept(next.getMaxHealth()==20&&s.artifacts.data.crownLives.get(next.getUUID()).victims.isEmpty(),"another wearer begins with their own empty kill progress");next.setHealth(0);next.die(next.damageSources().genericKill());
        }catch(Exception e){throw new AssertionError("Crown powers fixture failed",e);}
        finally{for(ServerPlayer player:fixtures){player.setHealth(0);s.artifacts.crown.died(player);s.server.getPlayerList().getPlayers().remove(player);s.artifacts.leave(player);}}
    }
    private static ServerPlayer create(KingdomService s,String name,UUID id,ServerPlayer reference){var p=new ServerPlayer(s.server,reference.level(),new GameProfile(id,name),ClientInformation.createDefault());p.setPos(reference.position());p.setHealth(20);new ServerGamePacketListenerImpl(s.server,new ArtifactIntegration.RecordingConnection(),p,CommonListenerCookie.createInitial(p.getGameProfile(),false));return p;}
    private CrownPowerIntegration(){}
}

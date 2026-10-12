package com.celik.kingdoms;
import java.util.*;
import java.util.function.BiConsumer;
import net.minecraft.server.level.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.inventory.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.enchantment.*;
import static com.celik.kingdoms.ArtifactRules.*;
import com.celik.kingdoms.ArtifactRules.Record;

final class ArtifactIntegration {
 static void run(KingdomService service,ServerPlayer player,ServerPlayer other,BiConsumer<Boolean,String> check){
  Artifacts a=service.artifacts;ArtifactConfig original=service.config.artifacts;Data saved=Store.JSON.fromJson(Store.JSON.toJson(a.data),Data.class);
  ItemStack[] inventory=new ItemStack[player.getInventory().getContainerSize()];for(int i=0;i<inventory.length;i++){inventory[i]=player.getInventory().getItem(i).copy();player.getInventory().setItem(i,ItemStack.EMPTY);}
  var oldPosition=player.position();var oldOther=other.position();float oldHealth=player.getHealth();boolean oldGround=player.onGround();double oldFall=player.fallDistance;
  ServerLevel level=player.level();BlockPos origin=new BlockPos(9008,200,7008);level.getChunk(origin);level.getChunk(origin.east(16));level.getChunk(origin.west(16));player.setPos(origin.getX()+.5,202,origin.getZ()+3);other.setPos(origin.getX()+2,202,origin.getZ()+3);
  player.closeContainer();
  var captured=new RecordingConnection();new net.minecraft.server.network.ServerGamePacketListenerImpl(service.server,captured,player,net.minecraft.server.network.CommonListenerCookie.createInitial(player.getGameProfile(),false));
  try{
   ArtifactConfig cfg=new ArtifactConfig();service.config.artifacts=cfg;a.validateConfiguration(cfg);cfg.chanceByDay=new TreeMap<>(Map.of(1,1d));cfg.fixerChance=1;
   var rewards=a.roll(UUID.randomUUID(),1,new Random(11));check.accept(rewards.size()==3&&rewards.stream().filter(r->r.rareKey.startsWith("artifact:")).count()==1,"one uncapped artifact and exactly two optional fixers");
   Record unique=issue(a,cfg,"vampire_sword");ItemStack authentic=a.create(unique);player.setItemInHand(InteractionHand.MAIN_HAND,authentic);a.observe(player,authentic);ItemStack duplicate=authentic.copy();other.setItemInHand(InteractionHand.MAIN_HAND,duplicate);a.observe(other,duplicate);check.accept(!Artifacts.marked(duplicate)&&a.record(authentic)==unique,"a physically duplicated identity cannot retain artifact abilities");other.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
   cfg.fixerChance=0;check.accept(a.roll(UUID.randomUUID(),7,new Random(14)).size()==1,"artifact drops remain free of cash and ordinary loot");
   for(String key:ArtifactConfig.KEYS){Record r=issue(a,cfg,key);ItemStack s=a.create(r);check.accept(a.record(s)==r&&s.getMaxStackSize()==1,"authentic new artifact: "+key);check.accept(!s.has(DataComponents.UNBREAKABLE)&&s.getOrDefault(DataComponents.LORE,net.minecraft.world.item.component.ItemLore.EMPTY).lines().size()<=3,"short lore and native durability: "+key);}
   Record thunder=issue(a,cfg,"thunder_axe");ItemStack axe=a.create(thunder);player.setItemInHand(InteractionHand.MAIN_HAND,axe);
   check.accept(axe.getMaxDamage()==512,"Thunder Axe starts with 512 vanilla durability");axe.hurtAndBreak(1,level,player,x->{});a.refresh(axe,thunder);check.accept(axe.getDamageValue()==1,"refresh preserves vanilla damage");
   var enchant=service.server.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
   EnchantmentHelper.updateEnchantments(axe,m->{m.set(enchant.getOrThrow(Enchantments.MENDING),1);m.set(enchant.getOrThrow(Enchantments.UNBREAKING),3);});a.refresh(axe,thunder);
   check.accept(EnchantmentHelper.getItemEnchantmentLevel(enchant.getOrThrow(Enchantments.MENDING),axe)==1&&EnchantmentHelper.getItemEnchantmentLevel(enchant.getOrThrow(Enchantments.UNBREAKING),axe)==3,"Mending and Unbreaking are retained");
   ItemStack decoded=AirDropLoot.decode(service.server,AirDropLoot.encode(service.server,axe));check.accept(decoded.getDamageValue()==1&&decoded.getMaxDamage()==512,"vanilla durability persists in saved item components");
   player.getInventory().setItem(9,new ItemStack(Items.NETHERITE_INGOT,3));a.upgrade(player);axe=player.getMainHandItem();thunder=a.data.records.get(thunder.id);
   check.accept(axe.is(Items.NETHERITE_AXE)&&axe.getMaxDamage()==768&&axe.getDamageValue()==2&&player.getInventory().getItem(9).isEmpty(),"Thunder upgrade consumes three ingots, increases durability and preserves wear percentage");
   check.accept(EnchantmentHelper.getItemEnchantmentLevel(enchant.getOrThrow(Enchantments.MENDING),axe)==1,"Netherite upgrade preserves enchantments");
   service.server.setWeatherParameters(0,1000,true,true);level.setRainLevel(1);level.setThunderLevel(1);int damage=axe.getDamageValue();axe.hurtAndBreak(100,level,player,x->{});check.accept(axe.getDamageValue()==damage,"upgraded Thunder Axe spends no durability in thunderstorms");
   thunder.readyAt=service.now()+60000;check.accept(a.freeDurability(axe,level),"weather durability protection remains active during configured ability cooldowns");thunder.readyAt=0;
   service.server.setWeatherParameters(1000,0,false,false);level.setRainLevel(0);level.setThunderLevel(0);check.accept(!a.freeDurability(axe,level),"Thunder Axe resumes normal durability outside thunderstorms");
   Record scythe=issue(a,cfg,"void_scythe");ItemStack fragile=a.create(scythe);player.setItemInHand(InteractionHand.MAIN_HAND,fragile);fragile.hurtAndBreak(10,level,player,x->{});check.accept(fragile.isEmpty()&&a.data.records.get(scythe.id).retired,"Void Scythe breaks normally after its ten durability is exhausted");
   Zombie target=new Zombie(EntityTypes.ZOMBIE,level);target.setPos(player.getX(),player.getY(),player.getZ()-2);target.setHealth(20);
   Record vampire=issue(a,cfg,"vampire_sword");ItemStack sword=a.create(vampire);player.setItemInHand(InteractionHand.MAIN_HAND,sword);player.setHealth(8);
   target.invulnerableTime=0;float before=target.getHealth();target.hurtServer(level,player.damageSources().playerAttack(player),8);float dealt=before-target.getHealth();check.accept(dealt>0&&Math.abs(player.getHealth()-(8+dealt*.25))<.001,"Vampire heals exactly one quarter of actual post-armor damage");
   vampire.upgraded=true;target.setHealth(20);target.invulnerableTime=0;player.setHealth(8);before=target.getHealth();target.hurtServer(level,player.damageSources().playerAttack(player),8);dealt=before-target.getHealth();check.accept(Math.abs(player.getHealth()-(8+dealt*.5))<.001,"upgraded Vampire heals half of actual damage");
   target.setHealth(1);target.invulnerableTime=0;player.setHealth(8);target.hurtServer(level,player.damageSources().playerAttack(player),100);check.accept(player.getHealth()<=8.5f,"Vampire does not heal from overkill damage");
   target=new Zombie(EntityTypes.ZOMBIE,level);target.setPos(player.getX(),player.getY(),player.getZ()-2);
   Record frost=issue(a,cfg,"frostbite_sword");player.setItemInHand(InteractionHand.MAIN_HAND,a.create(frost));target.hurtServer(level,player.damageSources().playerAttack(player),2);check.accept(target.isFullyFrozen()&&!target.hasEffect(net.minecraft.world.effect.MobEffects.SLOWNESS),"Frostbite applies real freezing rather than a Slowness substitute");
   frost.upgraded=true;check.accept(frost.upgradedDurationSeconds==1.5,"Netherite Frostbite adds one and a half seconds per hit");
   // Exercise locking with the real damage callback while granting enemy PvP permission in wilderness.
   Record voidRecord=issue(a,cfg,"void_scythe");player.setItemInHand(InteractionHand.MAIN_HAND,a.create(voidRecord));other.setHealth(20);other.invulnerableTime=0;other.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.SHIELD));other.startUsingItem(InteractionHand.OFF_HAND);
   a.hurt(other,level,player.damageSources().playerAttack(player),2,n->{other.setHealth(other.getHealth()-n);return true;});
   check.accept(a.itemDisabled(other)&&a.preventsMining(other)&&a.preventsAttack(other)&&!other.isUsingItem(),"Void disables weapons, tools and an already-raised shield");
   check.accept(other.gameMode.useItem(other,level,new ItemStack(Items.ENDER_PEARL),InteractionHand.MAIN_HAND)==InteractionResult.FAIL,"Void blocks right-click item use in the actual game-mode path");
   final boolean[] mined={false};a.mine(other,origin,()->{mined[0]=true;return true;});check.accept(!mined[0],"Void prevents block mining without stopping inventory access or movement");a.leave(other);
   Record thorns=issue(a,cfg,"thorns_shield");player.setItemInHand(InteractionHand.MAIN_HAND,a.create(thorns));player.startUsingItem(InteractionHand.MAIN_HAND);target.invulnerableTime=0;target.setHealth(20);
   float reflected=a.shield(player,target.damageSources().mobAttack(target),7,7);check.accept(reflected==7&&target.getHealth()<20,"Thorns reflects fully blocked damage even when nothing gets through");player.stopUsingItem();
   Record spear=issue(a,cfg,"wind_burst_spear");ItemStack weapon=a.create(spear);player.setItemInHand(InteractionHand.MAIN_HAND,weapon);check.accept(weapon.is(Items.DIAMOND_SPEAR)&&EnchantmentHelper.getItemEnchantmentLevel(enchant.getOrThrow(Enchantments.WIND_BURST),weapon)==2,"Diamond Spear carries actual Wind Burst II");player.getInventory().setItem(9,new ItemStack(Items.NETHERITE_INGOT,3));a.upgrade(player);check.accept(player.getMainHandItem().is(Items.NETHERITE_SPEAR)&&EnchantmentHelper.getItemEnchantmentLevel(enchant.getOrThrow(Enchantments.WIND_BURST),player.getMainHandItem())==2,"spear upgrades to Netherite while keeping Wind Burst II");
   ItemStack upgradedSpear=player.getMainHandItem();GrindstoneMenu grindstone=new GrindstoneMenu(300,player.getInventory());grindstone.getSlot(0).set(upgradedSpear);check.accept(grindstone.getSlot(2).hasItem()&&Artifacts.marked(grindstone.getSlot(2).getItem())&&EnchantmentHelper.getItemEnchantmentLevel(enchant.getOrThrow(Enchantments.WIND_BURST),grindstone.getSlot(2).getItem())==2,"grindstone keeps the spear's built-in Wind Burst enchantment");
   var experience=grindstone.getSlot(2).getClass().getDeclaredMethod("getExperienceFromItem",ItemStack.class);experience.setAccessible(true);check.accept((int)experience.invoke(grindstone.getSlot(2),upgradedSpear)==0,"built-in Wind Burst cannot be farmed for grindstone XP");
   spearTargets(a,cfg,player,other,check);
   Record lightning=issue(a,cfg,"thunder_axe");player.setItemInHand(InteractionHand.MAIN_HAND,a.create(lightning));target=new Zombie(EntityTypes.ZOMBIE,level);target.setPos(player.getX(),player.getY(),player.getZ()-2);Zombie victim=target;
   long bolts=countBolts(level);a.attack(player,target,false,()->victim.hurtServer(level,player.damageSources().playerAttack(player),2));check.accept(countBolts(level)==bolts,"non-critical Thunder hits cannot spawn lightning");target.invulnerableTime=0;
   a.attack(player,target,true,()->victim.hurtServer(level,player.damageSources().playerAttack(player),2));check.accept(countBolts(level)==bolts+1,"eligible critical hit spawns a real lightning entity");for(Entity e:level.getAllEntities())if(e instanceof LightningBolt bolt){check.accept(player.getUUID().equals(Provenance.entityOwner(bolt.getUUID())),"artifact lightning retains its player attribution");bolt.discard();}
   // Player.attack must snapshot critical state before Minecraft resets the attack timer.
   var ticker=LivingEntity.class.getDeclaredField("attackStrengthTicker");ticker.setAccessible(true);ticker.setInt(player,100);player.setOnGround(false);player.fallDistance=1;player.setSprinting(false);target.invulnerableTime=0;target.setHealth(20);bolts=countBolts(level);player.attack(target);check.accept(countBolts(level)==bolts+1,"real fully charged vanilla critical attack triggers Thunder Axe");for(Entity e:level.getAllEntities())if(e instanceof LightningBolt) e.discard();player.setOnGround(true);player.fallDistance=0;
   Record pick=issue(a,cfg,"multi_pickaxe");ItemStack tool=a.create(pick);player.setItemInHand(InteractionHand.MAIN_HAND,tool);player.setPos(origin.getX()+.5,origin.getY()-1,origin.getZ()+3);a.minedFace(player,origin,Direction.SOUTH);
   List<BlockPos> square=ArtifactMining.area(origin,Direction.SOUTH,Direction.NORTH,false);check.accept(square.equals(List.of(origin.east(),origin.below(),origin.east().below())),"2x2 mining treats the struck block as top-left from the viewed face");
   level.setBlockAndUpdate(origin,Blocks.STONE.defaultBlockState());for(BlockPos pos:square)level.setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());int wear=tool.getDamageValue();check.accept(player.gameMode.destroyBlock(origin),"real player mining starts a Multi Pickaxe operation");check.accept(square.stream().allMatch(pos->level.getBlockState(pos).isAir())&&tool.getDamageValue()==wear+4,"2x2 delegates all four blocks to normal player mining and durability");
   level.setBlockAndUpdate(origin,Blocks.STONE.defaultBlockState());for(BlockPos pos:square)level.setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());level.setBlockAndUpdate(square.getFirst(),Blocks.BEDROCK.defaultBlockState());player.gameMode.destroyBlock(origin);check.accept(level.getBlockState(square.getFirst()).is(Blocks.BEDROCK),"Multi Pickaxe cannot break bedrock");
   pick.upgraded=true;List<BlockPos> big=ArtifactMining.area(origin,Direction.SOUTH,Direction.NORTH,true);level.setBlockAndUpdate(origin,Blocks.STONE.defaultBlockState());for(BlockPos pos:big)level.setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());player.gameMode.destroyBlock(origin);check.accept(big.size()==8&&big.stream().allMatch(pos->level.getBlockState(pos).isAir()),"Netherite mines the full centered 3x3 area");
   miningProtection(service,other,check);
   for(Direction face:Direction.values()){var area=ArtifactMining.area(origin,face,Direction.NORTH,true);check.accept(area.size()==8&&new HashSet<>(area).size()==8&&area.stream().allMatch(pos->face.getAxis()==Direction.Axis.X?pos.getX()==origin.getX():face.getAxis()==Direction.Axis.Y?pos.getY()==origin.getY():pos.getZ()==origin.getZ()),"mining plane follows face "+face);}
   Record angel=issue(a,cfg,"guardian_angel");ItemStack heart=a.create(angel);player.setItemInHand(InteractionHand.MAIN_HAND,heart);BlockPos web=player.blockPosition();level.setBlockAndUpdate(web,Blocks.COBWEB.defaultBlockState());
   check.accept(player.gameMode.useItem(player,level,heart,InteractionHand.MAIN_HAND)==InteractionResult.SUCCESS&&a.webImmune(player)&&angel.uses==99,"Guardian right-click grants three-second web immunity and spends one charge");
   check.accept(a.guardian.masks(player,web)&&level.getBlockState(web).is(Blocks.COBWEB),"Guardian masks only the recipient's view and keeps real cobweb blocks intact");
   check.accept(captured.packets.stream().anyMatch(packet->packet instanceof net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket b&&b.getPos().equals(web)&&b.getBlockState().is(Blocks.GLOW_LICHEN)),"Guardian sends the harmless web replacement through real vanilla packets");
   captured.packets.clear();player.connection.send(new net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket(web,Blocks.COBWEB.defaultBlockState()));check.accept(captured.packets.getLast() instanceof net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket b&&b.getBlockState().is(Blocks.GLOW_LICHEN),"real block updates cannot reapply client-side web slowing during immunity");
   captured.packets.clear();var webChunk=level.getChunkSource().getChunkNow(web.getX()>>4,web.getZ()>>4);player.connection.send(new net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket(webChunk,level.getChunkSource().getLightEngine(),null,null));check.accept(captured.packets.getLast() instanceof net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket b&&b.getBlockState().is(Blocks.GLOW_LICHEN),"chunk resends cannot reapply client-side web slowing during immunity");
   var multiplier=Entity.class.getDeclaredField("stuckSpeedMultiplier");multiplier.setAccessible(true);multiplier.set(player,net.minecraft.world.phys.Vec3.ZERO);player.makeStuckInBlock(Blocks.COBWEB.defaultBlockState(),new net.minecraft.world.phys.Vec3(.25,.05,.25));check.accept(multiplier.get(player).equals(net.minecraft.world.phys.Vec3.ZERO),"Guardian also cancels server-side cobweb physics");
   a.use(player,InteractionHand.MAIN_HAND);check.accept(angel.uses==99,"holding right-click cannot drain charges during the active effect");a.guardian.clear(player);check.accept(!a.webImmune(player)&&level.getBlockState(web).is(Blocks.COBWEB),"Guardian expiry restores ordinary cobweb behavior");check.accept(captured.packets.getLast() instanceof net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket b&&b.getBlockState().is(Blocks.COBWEB),"expiry sends the actual cobweb back to the client");
   player.makeStuckInBlock(Blocks.COBWEB.defaultBlockState(),new net.minecraft.world.phys.Vec3(.25,.05,.25));a.guardian.start(player,3000);check.accept(multiplier.get(player).equals(net.minecraft.world.phys.Vec3.ZERO),"activation clears an existing cobweb slowdown without clearing other block effects");a.guardian.clear(player);
   a.guardian.start(player,0);a.guardian.tick();check.accept(!a.webImmune(player),"expired sessions are restored immediately");angel.uses=1;a.use(player,InteractionHand.MAIN_HAND);check.accept(angel.uses==0&&a.webImmune(player),"the final Guardian charge still grants its full effect");a.guardian.clear(player);
   boolean noUpgrade=false;try{a.upgrade(player);}catch(IllegalArgumentException e){noUpgrade=true;}check.accept(noUpgrade,"Guardian Angel has no invented Netherite upgrade");
   ItemStack fixer=AirDropLoot.decode(service.server,rewards.get(1).itemJson);ItemStack preview=a.repairPreview(heart,fixer);check.accept(!preview.isEmpty()&&angel.uses==0,"fixer preview restores Guardian charges without spending them early");
   Record repair=issue(a,cfg,"multi_pickaxe");ItemStack repairTool=a.create(repair);repairTool.setDamageValue(900);player.getInventory().clearContent();player.experienceLevel=30;
   AnvilMenu anvil=new AnvilMenu(301,player.getInventory());player.containerMenu=anvil;anvil.getSlot(0).set(repairTool);anvil.getSlot(1).set(fixer);check.accept(anvil.getSlot(2).hasItem()&&anvil.getCost()==cfg.repairLevels&&anvil.getSlot(2).getItem().getDamageValue()==900-(int)Math.ceil(repairTool.getMaxDamage()*.25),"real anvil fixer repairs native damage at the configured price");
   anvil.clicked(2,0,ContainerInput.PICKUP,player);check.accept(player.experienceLevel==25&&anvil.getSlot(0).getItem().isEmpty()&&anvil.getSlot(1).getItem().isEmpty(),"real anvil spends one fixer and saves the repair once");player.closeContainer();
   ItemStack repaired=player.getInventory().getItem(0);anvil=new AnvilMenu(302,player.getInventory());player.containerMenu=anvil;anvil.getSlot(0).set(repaired);anvil.getSlot(1).set(new ItemStack(Items.DIAMOND));check.accept(anvil.getSlot(2).hasItem()&&anvil.getSlot(2).getItem().getDamageValue()<repaired.getDamageValue(),"normal diamond repairs work on artifacts");
   for(String ench:List.of("mending","unbreaking","efficiency","silk_touch")){var book=new AirDropConfig.Loot("minecraft:enchanted_book",1,1,1);book.enchantments.put("minecraft:"+ench,1);anvil.getSlot(1).set(AirDropLoot.template(service.server,book));check.accept(anvil.getSlot(2).hasItem()&&Artifacts.marked(anvil.getSlot(2).getItem()),"compatible anvil enchantment preserves artifact identity: "+ench);}player.closeContainer();
   Record old=issue(a,cfg,"multi_pickaxe");old.type="smelter_pickaxe";old.maxUses=old.uses=2000;var legacy=com.google.gson.JsonParser.parseString(Store.JSON.toJson(old)).getAsJsonObject();legacy.addProperty("abilityBalanceVersion",1);Record retired=Store.JSON.fromJson(legacy,Record.class);a.data.records.put(retired.id,retired);check.accept(retired.retired&&a.record(a.create(retired))==null,"removed old artifact types safely retire");
   cfg.types.values().forEach(t->t.enabled=false);cfg.types.get("vampire_sword").enabled=true;player.getInventory().clearContent();for(int i=0;i<4;i++)a.give(player,"vampire_sword");check.accept(java.util.stream.IntStream.range(0,player.getInventory().getContainerSize()).filter(i->Artifacts.marked(player.getInventory().getItem(i))).count()==4,"admin artifact give remains uncapped");
   a.retire(vampire.id);Artifacts loaded=new Artifacts(service);check.accept(loaded.data.records.get(vampire.id).retired&&loaded.data.records.get(angel.id).uses==0,"charges and retired identities survive real disk reload");
  }catch(ReflectiveOperationException e){throw new AssertionError(e);}
  finally{a.guardian.clear(player);a.leave(player);a.leave(other);player.closeContainer();player.stopUsingItem();service.config.artifacts=original;a.data=saved;service.store.write("artifacts.json",saved);for(int i=0;i<inventory.length;i++)player.getInventory().setItem(i,inventory[i]);player.setPos(oldPosition);other.setPos(oldOther);player.setHealth(oldHealth);player.setOnGround(oldGround);player.fallDistance=oldFall;service.server.setWeatherParameters(1000,0,false,false);level.setRainLevel(0);level.setThunderLevel(0);}
 }
 static Record issue(Artifacts artifacts,ArtifactConfig cfg,String key){Record r=ArtifactRules.generate(key,cfg.types.get(key),new Random(1));r.delivered=true;artifacts.data.records.put(r.id,r);return r;}
 private static long countBolts(ServerLevel level){long n=0;for(Entity e:level.getAllEntities())if(e instanceof LightningBolt&&!e.isRemoved())n++;return n;}
 private static void spearTargets(Artifacts artifacts,ArtifactConfig cfg,ServerPlayer player,ServerPlayer other,BiConsumer<Boolean,String> check){
  var level=player.level();ItemStack oldHand=player.getMainHandItem(),oldOff=player.getOffhandItem();var position=player.position();var movement=player.getDeltaMovement();float yaw=player.getYRot(),headYaw=player.getYHeadRot(),pitch=player.getXRot();double fall=player.fallDistance;
  var props=List.of(EntityTypes.ARMOR_STAND,EntityTypes.MANNEQUIN,EntityTypes.MINECART,EntityTypes.CHEST_MINECART,EntityTypes.OAK_BOAT,EntityTypes.OAK_CHEST_BOAT,EntityTypes.ITEM_FRAME,EntityTypes.GLOW_ITEM_FRAME,EntityTypes.PAINTING,EntityTypes.ITEM,EntityTypes.FALLING_BLOCK,EntityTypes.END_CRYSTAL,EntityTypes.INTERACTION,EntityTypes.BLOCK_DISPLAY,EntityTypes.ARROW,EntityTypes.FIREBALL);
  try{
   Record spear=issue(artifacts,cfg,"wind_burst_spear");
   for(boolean upgraded:List.of(false,true)){
    spear.upgraded=upgraded;ItemStack weapon=artifacts.create(spear);player.setItemInHand(InteractionHand.MAIN_HAND,weapon);player.fallDistance=2;
    for(var type:props){Entity prop=Objects.requireNonNull(type.create(level,EntitySpawnReason.COMMAND));prop.setPos(position.add(0,0,-1));int damage=weapon.getDamageValue();var propMovement=prop.getDeltaMovement();float health=prop instanceof LivingEntity living?living.getHealth():0;
     check.accept(!net.minecraft.world.item.component.PiercingWeapon.canHitEntity(player,prop),"spear target scan skips "+type+" upgraded="+upgraded);
     player.attack(prop);boolean jab=player.stabAttack(EquipmentSlot.MAINHAND,prop,100,true,true,true);
     check.accept(!jab&&!prop.isRemoved()&&weapon.getDamageValue()==damage&&prop.getDeltaMovement().equals(propMovement)&&player.getDeltaMovement().equals(movement)&&(!(prop instanceof LivingEntity living)||living.getHealth()==health),"direct spear attacks cannot damage, launch or consume durability on "+type+" upgraded="+upgraded);
     player.setItemInHand(InteractionHand.OFF_HAND,weapon);player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
     check.accept(!player.stabAttack(EquipmentSlot.OFFHAND,prop,100,true,true,true),"offhand charged spear also refuses "+type+" upgraded="+upgraded);player.setItemInHand(InteractionHand.MAIN_HAND,weapon);player.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);prop.discard();
    }
    Zombie zombie=new Zombie(EntityTypes.ZOMBIE,level);zombie.setPos(position.add(0,0,-2));check.accept(artifacts.allowsAttack(weapon,zombie)&&artifacts.allowsAttack(weapon,other),"spear still targets living mobs and players upgraded="+upgraded);
    zombie.setHealth(20);player.fallDistance=0;check.accept(player.stabAttack(EquipmentSlot.MAINHAND,zombie,5,true,false,false)&&zombie.getHealth()<20,"native spear jab still damages a mob upgraded="+upgraded);zombie.discard();
   }
   var stand=Objects.requireNonNull(EntityTypes.ARMOR_STAND.create(level,EntitySpawnReason.COMMAND));stand.setPos(position.add(0,0,-1));player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND_SPEAR));check.accept(net.minecraft.world.item.component.PiercingWeapon.canHitEntity(player,stand),"ordinary spears keep vanilla nonliving target behavior");
   var dragon=new net.minecraft.world.entity.boss.enderdragon.EnderDragon(EntityTypes.ENDER_DRAGON,level);ItemStack weapon=artifacts.create(spear);check.accept(artifacts.allowsAttack(weapon,dragon.getSubEntities()[0]),"living dragon hitboxes remain eligible targets");dragon.discard();
   player.setItemInHand(InteractionHand.MAIN_HAND,weapon);player.fallDistance=0;player.setYRot(180);player.setYHeadRot(180);player.setXRot(0);var zombie=new Zombie(EntityTypes.ZOMBIE,level);zombie.setPos(position.add(0,0,-3));zombie.setHealth(20);
   check.accept(level.addFreshEntity(stand)&&level.addFreshEntity(zombie),"mixed spear ray fixture adds armor stand and living target");
   try{
    float standHealth=stand.getHealth();weapon.get(DataComponents.PIERCING_WEAPON).attack(player,EquipmentSlot.MAINHAND);check.accept(stand.getHealth()==standHealth&&zombie.getHealth()<20,"real piercing jab skips the armor stand and still hits the mob behind it");
    var kinetic=weapon.get(DataComponents.KINETIC_WEAPON);player.startUsingItem(InteractionHand.MAIN_HAND);kinetic.damageEntities(weapon,weapon.getUseDuration(player)-kinetic.delayTicks()-1,player,EquipmentSlot.MAINHAND);
    check.accept(player.stabbedEntities(e->e==stand)==0&&player.stabbedEntities(e->e==zombie)>0,"actual charged contact skips armor stands before cooldown and contact bookkeeping");player.stopUsingItem();
    zombie.setItemSlot(EquipmentSlot.MAINHAND,weapon);check.accept(!zombie.stabAttack(EquipmentSlot.MAINHAND,stand,100,true,true,true)&&stand.getHealth()==standHealth,"mob-held artifact spear also refuses armor stands");
   }finally{player.stopUsingItem();stand.discard();zombie.discard();}
  }finally{player.setItemInHand(InteractionHand.MAIN_HAND,oldHand);player.setItemInHand(InteractionHand.OFF_HAND,oldOff);player.setPos(position);player.setDeltaMovement(movement);player.setYRot(yaw);player.setYHeadRot(headYaw);player.setXRot(pitch);player.fallDistance=fall;}
 }
 private static final class RecordingConnection extends net.minecraft.network.Connection {
  final List<net.minecraft.network.protocol.Packet<?>> packets=new ArrayList<>();
  RecordingConnection(){super(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);}
  @Override public void send(net.minecraft.network.protocol.Packet<?> packet,io.netty.channel.ChannelFutureListener listener,boolean flush){packets.add(packet);}
 }
 private static void miningProtection(KingdomService s,ServerPlayer p,BiConsumer<Boolean,String> check){
  var saved=s.store.copy(s.state);var position=p.position();ItemStack hand=p.getMainHandItem();BlockPos origin=new BlockPos(9023,200,7008);var level=p.level();
  try{
   Rules.Kingdom foreign=new Rules.Kingdom();foreign.name="Mining Guard";UUID owner=UUID.randomUUID();foreign.members.put(owner,"Guard");foreign.mayors.add(owner);foreign.claims.add(KingdomService.key(level,origin.east()));foreign.nextUpkeep=s.now()+86400000;
   var next=s.store.copy(s.state);next.kingdoms.put(foreign.id,foreign);s.commit(next);p.setPos(origin.getX()+.5,199,origin.getZ()+3);
   Record r=issue(s.artifacts,s.config.artifacts,"multi_pickaxe");ItemStack pick=s.artifacts.create(r);p.setItemInHand(InteractionHand.MAIN_HAND,pick);s.artifacts.minedFace(p,origin,Direction.SOUTH);
   for(BlockPos pos:List.of(origin,origin.below(),origin.east(),origin.east().below()))try(var scope=Protection.scope(level,pos,owner,null,Protection.Cause.PLAYER)){level.setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());}
   check.accept(p.gameMode.destroyBlock(origin)&&level.getBlockState(origin.below()).isAir()&&level.getBlockState(origin.east()).is(Blocks.STONE)&&level.getBlockState(origin.east().below()).is(Blocks.STONE),"Multi Pickaxe stops at another kingdom's protected claim boundary");
   check.accept(pick.getDamageValue()==2,"denied neighboring blocks consume no durability and produce no loot");
  }finally{s.commit(saved);p.setPos(position);p.setItemInHand(InteractionHand.MAIN_HAND,hand);}
 }
}

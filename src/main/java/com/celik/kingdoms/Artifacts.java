package com.celik.kingdoms;

import com.celik.kingdoms.mixin.AirDropPlayerSaveInvoker;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.damagesource.DamageSource;
import java.util.*;
import java.util.function.Supplier;
import static com.celik.kingdoms.ArtifactRules.*;
import com.celik.kingdoms.ArtifactRules.Record;

/** Persistent identities authenticate artifacts in inventories and storage without quantity limits. */
public final class Artifacts {
    private final KingdomService service;
    Data data;
    private boolean healthy=true,dirty;
    private int ticks;
    private final Random random=new Random();
    private final Map<UUID,ItemStack> leases=new HashMap<>();
    private final Map<UUID,ServerPlayer> observedPlayers=new HashMap<>();
    private final Map<UUID,Long> disabled=new HashMap<>();
    private final Map<UUID,Freeze> frozen=new HashMap<>();
    final GuardianAngel guardian;
    final ArtifactArmor armor=new ArtifactArmor(this);
    public final CrownPowers crown;
    private final ThreadLocal<Boolean> reflected=ThreadLocal.withInitial(()->false);
    private final ThreadLocal<Attack> attack=new ThreadLocal<>();
    private final Map<UUID,Face> faces=new HashMap<>();
    private record Face(BlockPos pos,net.minecraft.core.Direction direction){}
    private record Freeze(LivingEntity entity,long until){}
    private record Attack(ServerPlayer player,Entity target,boolean critical){}
    private final ThreadLocal<Mining> mining=new ThreadLocal<>();
    public record Mining(ServerPlayer player,BlockPos pos,BlockState state,ItemStack stack,boolean placed){}
    public static final class Journal {public UUID player,receipt;public Record next;public UUID fixer;}
    Artifacts(KingdomService service){this.service=service;guardian=new GuardianAngel(service);crown=new CrownPowers(this,service);data=service.store.read("artifacts.json",Data.class,new Data());ArtifactRules.validate(data);validateConfiguration(config());healthy=service.store.read("artifact-transaction.json",Journal.class,null)==null;if(healthy)save();}
    ArtifactConfig config(){return service.config.artifacts;}
    public void validateConfiguration(ArtifactConfig cfg){cfg.validate();for(var e:cfg.types.entrySet()){
        ItemStack base=new ItemStack(item(e.getValue().baseItem));
        if(!e.getKey().equals("guardian_angel")&&!base.isDamageableItem())throw new IllegalArgumentException("Artifact tools require a damageable base item: "+e.getKey());
        if(e.getKey().equals("guardian_angel")&&!base.is(Items.HEART_OF_THE_SEA))throw new IllegalArgumentException("Guardian Angel must use Heart of the Sea");
    }item(cfg.fixerItem);}
    private static Item item(String name){var id=Identifier.parse(name);if(BuiltInRegistries.ITEM.get(id).isEmpty())throw new IllegalArgumentException("Unknown artifact item: "+name);return BuiltInRegistries.ITEM.getValue(id);}
    private void check(){if(!healthy)throw new IllegalStateException("Artifact storage requires administrator recovery.");}
    void saveCrown(){save();}
    private void save(){check();try{ArtifactRules.validate(data);service.store.write("artifacts.json",data);dirty=false;}catch(RuntimeException e){healthy=false;throw e;}}
    public static boolean marked(ItemStack stack){return tag(stack,"kingdoms_artifact")!=null;}
    public static boolean fixerMarked(ItemStack stack){return tag(stack,"kingdoms_fixer")!=null;}
    private static UUID tag(ItemStack stack,String key){try{return UUID.fromString(stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getStringOr(key,""));}catch(Exception e){return null;}}
    Record record(ItemStack stack){UUID id=tag(stack,"kingdoms_artifact");Record r=data.records.get(id);if(r==null||r.retired||stack.getCount()!=1||!r.nonce.equals(tag(stack,"kingdoms_nonce")))return null;migrateScytheDurability(stack,r);return r;}
    private void migrateScytheDurability(ItemStack stack,Record r){
        int maximum=r.upgraded&&r.upgradedMaxUses>0?r.upgradedMaxUses:r.maxUses;
        if(r.type.equals("void_scythe")&&maximum==1000&&stack.getMaxDamage()==10&&marked(stack)){
            int damage=stack.getDamageValue();stack.set(DataComponents.MAX_DAMAGE,1000);stack.setDamageValue(Math.min(999,damage*100));
        }
    }
    private boolean lease(ItemStack stack,Record r){ItemStack old=leases.get(r.id);if(old==null||old==stack||old.isEmpty()||!physical(old)){leases.put(r.id,stack);return true;}return false;}
    private boolean physical(ItemStack stack){Set<ServerPlayer> players=new HashSet<>(service.server.getPlayerList().getPlayers());players.addAll(observedPlayers.values());for(ServerPlayer p:players){for(int i=0;i<p.getInventory().getContainerSize();i++)if(p.getInventory().getItem(i)==stack)return true;for(var slot:p.containerMenu.slots)if(slot.getItem()==stack)return true;if(p.containerMenu.getCarried()==stack)return true;}for(ServerLevel l:service.server.getAllLevels())for(Entity e:l.getAllEntities())if(e instanceof ItemEntity item&&!item.isRemoved()&&item.getItem()==stack)return true;return false;}
    public Record usable(ItemStack stack){Record r=record(stack);return healthy&&config().enabled&&r!=null&&config().types.containsKey(r.type)&&config().types.get(r.type).enabled&&lease(stack,r)&&ArtifactRules.active(r,service.now())?r:null;}
    public boolean depleted(ItemStack stack){Record r=record(stack);return r==null||r.type.equals("guardian_angel")&&r.uses<=0;}
    private Record cloneRecord(Record r){return Store.JSON.fromJson(Store.JSON.toJson(r),Record.class);}
    public ItemStack create(Record r){ItemStack stack=new ItemStack(item(r.upgraded?r.baseItem.replace("diamond_","netherite_"):r.baseItem));refresh(stack,r);return stack;}
    private static ChatFormatting color(String rarity){return switch(rarity){case "mythic"->ChatFormatting.LIGHT_PURPLE;case "legendary"->ChatFormatting.GOLD;case "arcane"->ChatFormatting.AQUA;default->ChatFormatting.GRAY;};}
    private Component line(String text){return Component.literal(Ui.caps(text)).withStyle(ChatFormatting.GRAY);}
    public void refresh(ItemStack stack,Record r){
        migrateScytheDurability(stack,r);
        int catalog=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr("kingdoms_catalog",0);
        stack.set(DataComponents.MAX_STACK_SIZE,1);stack.remove(DataComponents.UNBREAKABLE);
        int max=r.upgraded&&r.upgradedMaxUses>0?r.upgradedMaxUses:r.maxUses;
        if(!r.type.equals("guardian_angel")&&max>0)stack.set(DataComponents.MAX_DAMAGE,max);
        if(catalog<2&&r.legacyRemainingFraction<1&&stack.isDamageableItem())stack.setDamageValue(Math.min(stack.getMaxDamage()-1,(int)Math.round(stack.getMaxDamage()*(1-r.legacyRemainingFraction))));
        CustomData.update(DataComponents.CUSTOM_DATA,stack,t->{t.putString("kingdoms_artifact",r.id.toString());t.putString("kingdoms_nonce",r.nonce.toString());t.putInt("kingdoms_revision",r.revision);t.putInt("kingdoms_catalog",2);});
        stack.set(DataComponents.CUSTOM_NAME,Component.literal(Ui.caps(r.name+(r.upgraded?" • Netherite":""))).withStyle(color(r.rarity)));
        List<Component> lore=new ArrayList<>();lore.add(Component.literal(Ui.caps(r.rarity)).withStyle(color(r.rarity)));lore.add(line(description(r)));
        if(r.type.equals("guardian_angel"))lore.add(line(r.uses+" / "+r.maxUses+" uses"));
        if(r.type.equals("golden_crown"))lore.add(line("Bound • Glowing • Unbreakable"));
        if(r.type.equals("thunder_axe")&&r.upgraded)lore.add(line("No durability cost during thunderstorms"));
        stack.set(DataComponents.LORE,new ItemLore(lore));stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE,true);
        if(r.type.equals("wind_burst_spear")){
            var enchant=service.server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.WIND_BURST);
            net.minecraft.world.item.enchantment.EnchantmentHelper.updateEnchantments(stack,m->m.set(enchant,r.amplifier));
        }
        if(r.type.equals("golden_crown")){
            stack.set(DataComponents.UNBREAKABLE,net.minecraft.util.Unit.INSTANCE);stack.setDamageValue(0);
            var binding=service.server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.BINDING_CURSE);
            net.minecraft.world.item.enchantment.EnchantmentHelper.updateEnchantments(stack,m->m.set(binding,1));
            stack.set(DataComponents.ITEM_MODEL,Identifier.parse("minecraft:golden_helmet"));
            var equipment=stack.get(DataComponents.EQUIPPABLE);
            if(equipment!=null)stack.set(DataComponents.EQUIPPABLE,new net.minecraft.world.item.equipment.Equippable(equipment.slot(),equipment.equipSound(),Optional.of(net.minecraft.world.item.equipment.EquipmentAssets.GOLD),equipment.cameraOverlay(),equipment.allowedEntities(),equipment.dispensable(),equipment.swappable(),equipment.damageOnHurt(),equipment.equipOnInteract(),equipment.canBeSheared(),equipment.shearingSound()));
        }
    }
    private double value(Record r){return r.upgraded?r.upgradedValue:r.value;}
    private double duration(Record r){return r.upgraded?r.upgradedDurationSeconds:r.durationSeconds;}
    private String description(Record r){return switch(r.type){
        case "thunder_axe"->"Lightning on fully charged critical hits";
        case "thorns_shield"->"Reflects "+Math.round(value(r)*100)+"% of blocked damage";
        case "frostbite_sword"->"Each hit adds "+duration(r)+"s of freezing";
        case "multi_pickaxe"->r.upgraded?"Mines a centered 3×3 area":"Mines 2×2 from the top-left block";
        case "vampire_sword"->"Heals "+Math.round(value(r)*100)+"% of damage dealt";
        case "void_scythe"->"Hits disable held items for "+duration(r)+"s";
        case "guardian_angel"->"Right-click: ignore cobwebs for "+duration(r)+"s";
        case "wind_burst_spear"->"Wind Burst "+r.amplifier;
        case "runners_boots"->"Speed I while worn • Stronger potions take priority";
        case "kings_chestplate"->"Regeneration I while worn • Stronger potions take priority";
        case "golden_crown"->"+"+(config().crownHealthPerUniqueKill/2d)+" hearts per unique kill • Resets on death";
        case "archers_bow"->"Fires two arrows per shot";
        case "advanced_axe"->"Fells connected tree logs";
        case "advanced_pickaxe"->"Mines connected ore veins";
        default->"Retired artifact";
    };}
    public boolean forbiddenEnchantments(ItemStack stack){return false;}
    public boolean fixedEnchantment(ItemStack stack){Record r=record(stack);return r!=null&&r.type.equals("wind_burst_spear");}
    public boolean allowsAttack(ItemStack stack,Entity target){
        if(!fixedEnchantment(stack))return true;
        if(target instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragonPart part)target=part.parentMob;
        return target instanceof LivingEntity&&!(target instanceof net.minecraft.world.entity.decoration.ArmorStand)&&!(target instanceof net.minecraft.world.entity.decoration.Mannequin)&&target.isAlive();
    }
    public ItemStack enchantPreview(ItemStack input,ItemStack result){
        Record r=record(input);if(r==null||result.isEmpty())return ItemStack.EMPTY;
        int oldMaximum=result.getMaxDamage(),remaining=oldMaximum-result.getDamageValue();refresh(result,r);
        if(result.isDamageableItem()&&oldMaximum!=result.getMaxDamage())result.setDamageValue(Math.max(0,result.getMaxDamage()-remaining));
        return result;
    }
    /** Preserve exactly one identity. Combining two artifacts must never discard a power. */
    public void prepareCombination(ItemStack... inputs){for(ItemStack input:inputs)if(marked(input))record(input);}
    public ItemStack combinationPreview(ItemStack left,ItemStack right,ItemStack result){
        if(marked(left)&&marked(right)||fixerMarked(left)||fixerMarked(right))return ItemStack.EMPTY;
        if(marked(left))return enchantPreview(left,result);
        if(marked(right))return enchantPreview(right,result);
        return result;
    }
    private ItemStack fixer(Fixer f){ItemStack s=new ItemStack(item(config().fixerItem));s.set(DataComponents.MAX_STACK_SIZE,1);CustomData.update(DataComponents.CUSTOM_DATA,s,t->{t.putString("kingdoms_fixer",f.id.toString());t.putString("kingdoms_nonce",f.nonce.toString());});s.set(DataComponents.CUSTOM_NAME,Component.literal(Ui.caps("Artifact Fixer")).withStyle(ChatFormatting.AQUA));s.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE,true);s.set(DataComponents.LORE,new ItemLore(List.of(line("Restores "+Math.round(config().repairFraction*100)+"% of durability or Guardian Angel charges"),line("Use in an anvil • "+config().repairLevels+" experience levels"),line("Cannot repair ordinary items"))));return s;}
    /** Reserves an identity before the AirDrop outcome is saved. Orphan reservations reconcile on restart. */
    List<AirDropRules.Reward> roll(UUID drop,long day,Random random){
        check();if(!config().enabled)return null;var keys=ArtifactRules.eligible(config(),data,day);if(keys.isEmpty()||random.nextDouble()>=config().chance(day))return null;
        long total=keys.stream().mapToLong(k->config().types.get(k).weight).sum(),roll=random.nextLong(total);String selected=keys.getLast();for(String k:keys){roll-=config().types.get(k).weight;if(roll<0){selected=k;break;}}
        Record r=ArtifactRules.generate(selected,config().types.get(selected),random);r.drop=drop;data.records.put(r.id,r);List<AirDropRules.Reward> rewards=new ArrayList<>();rewards.add(reward(create(r),"artifact:"+r.id));
        if(random.nextDouble()<config().fixerChance)for(int i=0;i<config().fixerCount;i++){Fixer f=new Fixer();f.drop=drop;data.fixers.put(f.id,f);rewards.add(reward(fixer(f),"fixer:"+f.id));}save();return rewards;
    }
    private AirDropRules.Reward reward(ItemStack stack,String key){var r=new AirDropRules.Reward();r.rareKey=key;r.itemJson=AirDropLoot.encode(service.server,stack);return r;}
    public void reconcile(){if(service.airDrops==null||!healthy)return;boolean changed=false;for(Record r:data.records.values())if(!r.retired&&!r.delivered&&r.drop!=null){var drop=service.airDrops.data.drops.stream().filter(d->d.id.equals(r.drop)).findFirst().orElse(null);var reward=drop==null?null:drop.loot.stream().filter(v->("artifact:"+r.id).equals(v.rareKey)).findFirst().orElse(null);if(reward!=null&&reward.status==AirDropRules.LootStatus.COLLECTED){r.delivered=true;changed=true;}else if(reward==null||reward.status==AirDropRules.LootStatus.EXPIRED||drop.phase==AirDropRules.Phase.CANCELLED){r.retired=true;changed=true;}}if(changed)save();}
    public void announce(AirDropRules.Drop drop){if(config().announcements&&drop.loot.stream().anyMatch(r->r.rareKey!=null&&r.rareKey.startsWith("artifact:")))service.announce(Component.literal(Ui.caps("[artifact] A unique artifact drop has been detected at "+drop.x+", "+drop.y+", "+drop.z)).withStyle(ChatFormatting.LIGHT_PURPLE));}
    private boolean consume(ItemStack stack,Record r){if(!ArtifactRules.consume(r,service.now()))return false;save();refresh(stack,r);return true;}
    public boolean itemDisabled(ServerPlayer p){return service.now()<disabled.getOrDefault(p.getUUID(),0L);}
    public boolean preventsMining(ServerPlayer p){return itemDisabled(p);}
    public boolean preventsAttack(ServerPlayer p){return itemDisabled(p);}
    public void minedFace(ServerPlayer p,BlockPos pos,net.minecraft.core.Direction face){faces.put(p.getUUID(),new Face(pos.immutable(),face));}
    public boolean mine(ServerPlayer player,BlockPos pos,Supplier<Boolean> original){
        if(preventsMining(player))return false;
        if(mining.get()!=null)return original.get();
        ItemStack stack=player.getMainHandItem();Record r=usable(stack);BlockState state=player.level().getBlockState(pos);
        List<BlockPos> chain=r!=null&&(r.type.equals("advanced_axe")||r.type.equals("advanced_pickaxe"))?ArtifactChains.collect(service,this,player,pos,state,r.type):List.of();
        Mining context=new Mining(player,pos.immutable(),state,stack,false);mining.set(context);
        try{
            boolean success=original.get();
            if(success&&r!=null&&r.type.equals("multi_pickaxe")&&random.nextDouble()<r.chance){
                activated(r);
                Face remembered=faces.get(player.getUUID());net.minecraft.core.Direction face=remembered!=null&&remembered.pos.equals(pos)?remembered.direction:net.minecraft.core.Direction.getApproximateNearest(player.getLookAngle()).getOpposite();
                for(BlockPos extra:ArtifactMining.area(pos,face,player.getDirection(),r.upgraded)){
                    if(stack.isEmpty()||player.getMainHandItem()!=stack)break;
                    if(player.level().getChunkSource().getChunkNow(extra.getX()>>4,extra.getZ()>>4)==null||!player.level().getWorldBorder().isWithinBounds(extra)||!player.isWithinBlockInteractionRange(extra,1))continue;
                    BlockState next=player.level().getBlockState(extra);
                    if(next.isAir()||next.getDestroySpeed(player.level(),extra)<0||next.getBlock() instanceof net.minecraft.world.level.block.GameMasterBlock)continue;
                    player.gameMode.destroyBlock(extra); // Normal mining handles permissions, tool costs and loot.
                }
            }
            if(success&&r!=null&&!chain.isEmpty()&&random.nextDouble()<r.chance){activated(r);for(BlockPos extra:chain){if(stack.isEmpty()||player.getMainHandItem()!=stack)break;if(player.level().getChunkSource().getChunkNow(extra.getX()>>4,extra.getZ()>>4)!=null)player.gameMode.destroyBlock(extra);}}
            return success;
        }finally{mining.remove();}
    }
    public List<ItemStack> drops(ServerLevel level,BlockPos pos,List<ItemStack> drops){return drops;}
    boolean placed(Level level,BlockPos pos){return data.placedBlocks.contains(Provenance.key(level,pos));}
    public void blockChanged(Level level,BlockPos pos,BlockState state){if(!healthy||level.isClientSide())return;if(!state.is(net.minecraft.tags.BlockTags.LOGS)&&!ArtifactChains.ore(state)&&data.placedBlocks.remove(Provenance.key(level,pos)))dirty=true;}
    public void placedBlock(Level level,BlockPos pos){if(!healthy||level.isClientSide())return;BlockState state=level.getBlockState(pos);if((state.is(net.minecraft.tags.BlockTags.LOGS)||ArtifactChains.ore(state))&&data.placedBlocks.add(Provenance.key(level,pos)))dirty=true;}
    public boolean archerBow(ItemStack stack){Record record=usable(stack);return record!=null&&record.type.equals("archers_bow");}
    public boolean instantMine(ServerPlayer player,BlockPos pos,BlockState state){if(itemDisabled(player)||state.getDestroySpeed(player.level(),pos)<0||config().advancedProtectPlacedBlocks&&placed(player.level(),pos))return false;Record record=usable(player.getMainHandItem());return record!=null&&(record.type.equals("advanced_axe")&&state.is(net.minecraft.tags.BlockTags.LOGS)||record.type.equals("advanced_pickaxe")&&ArtifactChains.ore(state));}
    private boolean hostile(ServerPlayer p,LivingEntity target,DamageSource source){
        if(target==p||target.isAlliedTo(p)||target instanceof OwnableEntity||target instanceof Animal||!Protection.damage(target,source))return false;
        if(target instanceof ServerPlayer other){var a=service.own(p.getUUID());var b=service.own(other.getUUID());if(a!=null&&b!=null&&a.id.equals(b.id))return false;}
        return true;
    }
    public void attack(ServerPlayer p,Entity target,boolean critical,Runnable original){
        if(preventsAttack(p)||!allowsAttack(p.getMainHandItem(),target))return;Attack previous=attack.get();attack.set(new Attack(p,target,critical));
        try{original.run();}finally{if(previous==null)attack.remove();else attack.set(previous);}
    }
    public boolean hurt(LivingEntity target,ServerLevel level,DamageSource source,float amount,java.util.function.Function<Float,Boolean> original){
        if(reflected.get()||!(source.getEntity() instanceof ServerPlayer p)||source.getDirectEntity()!=p||!hostile(p,target,source))return original.apply(amount);
        if(preventsAttack(p))return false;
        ItemStack stack=p.getMainHandItem();Record r=usable(stack);float before=target.getHealth()+target.getAbsorptionAmount();
        boolean hit=original.apply(amount);float dealt=Math.max(0,before-target.getHealth()-target.getAbsorptionAmount());
        if(!hit||r==null||random.nextDouble()>=r.chance)return hit;
        if(r.type.equals("void_scythe")){disable(target,r);return hit;}
        if(dealt<=0)return hit;
        switch(r.type){
            case "vampire_sword"->{p.heal(dealt*(float)value(r));activated(r);}
            case "frostbite_sword"->{long until=Math.max(service.now(),frozen.containsKey(target.getUUID())?frozen.get(target.getUUID()).until:0)+(long)(duration(r)*1000);frozen.put(target.getUUID(),new Freeze(target,until));target.setTicksFrozen(target.getTicksRequiredToFreeze()+2);activated(r);}
            case "thunder_axe"->{Attack a=attack.get();if(a!=null&&a.player==p&&a.target==target&&a.critical){LightningBolt bolt=new LightningBolt(EntityTypes.LIGHTNING_BOLT,level);bolt.setPos(target.position());bolt.setCause(p);Provenance.entity(bolt.getUUID(),p.getUUID());level.addFreshEntity(bolt);activated(r);}}
            default->{}
        }
        return hit;
    }
    public float shield(LivingEntity target,DamageSource source,float incoming,float blocked){
        return reflect(target,source,incoming,blocked,usable(target.getUseItem()));
    }
    public float block(LivingEntity target,DamageSource source,float incoming,Supplier<Float> original){
        ItemStack blocking=target.getItemBlockingWith();Record r=blocking==null?null:usable(blocking);
        float blocked=original.get();float result=reflect(target,source,incoming,blocked,r);
        if(blocked>0&&blocked>=incoming&&!reflected.get()&&source.getEntity() instanceof ServerPlayer attacker&&source.getDirectEntity()==attacker&&!itemDisabled(attacker)&&hostile(attacker,target,source)){
            Record scythe=usable(attacker.getMainHandItem());if(scythe!=null&&scythe.type.equals("void_scythe")&&random.nextDouble()<scythe.chance)disable(target,scythe);
        }
        return result;
    }
    private void disable(LivingEntity target,Record record){disabled.put(target.getUUID(),service.now()+(long)(duration(record)*1000));target.stopUsingItem();if(target instanceof ServerPlayer player)voidCooldowns(player);activated(record);}
    private void voidCooldowns(ServerPlayer player){int remaining=(int)Math.ceil(Math.max(0,disabled.getOrDefault(player.getUUID(),0L)-service.now())/50d);if(remaining<=0)return;for(var hand:net.minecraft.world.InteractionHand.values()){ItemStack stack=player.getItemInHand(hand);if(!stack.isEmpty()&&!player.getCooldowns().isOnCooldown(stack))player.getCooldowns().addCooldown(stack,remaining);}}
    private float reflect(LivingEntity target,DamageSource source,float incoming,float blocked,Record r){
        if(reflected.get()||!(target instanceof ServerPlayer p)||blocked<=0||itemDisabled(p)||!Protection.damage(target,source)||!(source.getEntity() instanceof LivingEntity attacker))return blocked;
        if(r==null||!r.type.equals("thorns_shield")||random.nextDouble()>=r.chance||!Protection.damage(attacker,p.damageSources().thorns(p)))return blocked;
        reflected.set(true);try{attacker.hurtServer(p.level(),p.damageSources().thorns(p),blocked*(float)value(r));activated(r);}finally{reflected.remove();}
        return blocked;
    }
    private void activated(Record r){if(r.cooldownSeconds>0){r.readyAt=service.now()+r.cooldownSeconds*1000L;r.revision++;save();}}
    public boolean freeDurability(ItemStack stack,ServerLevel level){Record r=record(stack);if(r!=null&&r.type.equals("golden_crown"))return true;return healthy&&config().enabled&&r!=null&&r.type.equals("thunder_axe")&&config().types.get(r.type).enabled&&lease(stack,r)&&r.upgraded&&level.isThundering();}
    public boolean webImmune(ServerPlayer p){return guardian.active(p);}
    public void guardianPackets(ServerPlayer p,net.minecraft.network.protocol.Packet<?> packet){guardian.afterPacket(p,packet);}
    public net.minecraft.world.InteractionResult use(ServerPlayer p,net.minecraft.world.InteractionHand hand){
        if(itemDisabled(p))return net.minecraft.world.InteractionResult.FAIL;
        ItemStack stack=p.getItemInHand(hand);Record r=usable(stack);
        if(r==null||!r.type.equals("guardian_angel"))return net.minecraft.world.InteractionResult.PASS;
        if(guardian.active(p))return net.minecraft.world.InteractionResult.SUCCESS;
        if(consume(stack,r)&&random.nextDouble()<r.chance)guardian.start(p,(long)(duration(r)*1000));
        return net.minecraft.world.InteractionResult.SUCCESS;
    }
    ItemStack find(ServerPlayer p,UUID id){for(int i=0;i<p.getInventory().getContainerSize();i++){ItemStack s=p.getInventory().getItem(i);if(id.equals(tag(s,"kingdoms_artifact")))return s;}return ItemStack.EMPTY;}
    public void observe(ServerPlayer p,ItemStack stack){observedPlayers.put(p.getUUID(),p);Record r=record(stack);if(r==null){if(marked(stack))invalidate(stack);return;}if(!lease(stack,r)){invalidate(stack);return;}boolean changed=false;if(!r.delivered){r.delivered=true;changed=true;}if(r.owner==null||config().rebindOnTrade&&!p.getUUID().equals(r.owner)){r.owner=p.getUUID();changed=true;}if(changed)save();refresh(stack,r);}
    private void invalidate(ItemStack stack){Record saved=data.records.get(tag(stack,"kingdoms_artifact"));boolean retired=saved!=null&&saved.retired;CustomData.update(DataComponents.CUSTOM_DATA,stack,t->{t.remove("kingdoms_artifact");t.remove("kingdoms_fixer");t.remove("kingdoms_nonce");});stack.remove(DataComponents.UNBREAKABLE);stack.set(DataComponents.CUSTOM_NAME,Component.literal(Ui.caps(retired?"Retired artifact":"Invalid artifact replica")).withStyle(ChatFormatting.DARK_GRAY));stack.set(DataComponents.LORE,new ItemLore(List.of(line("This item has no artifact abilities"))));}
    public void leave(ServerPlayer p){armor.leave(p);observedPlayers.remove(p.getUUID());faces.remove(p.getUUID());disabled.remove(p.getUUID());guardian.clear(p);for(int i=0;i<p.getInventory().getContainerSize();i++){Record r=record(p.getInventory().getItem(i));if(r!=null&&leases.get(r.id)==p.getInventory().getItem(i))leases.remove(r.id);}}
    public void tick(){guardian.tick();try{crown.tick();}catch(RuntimeException failure){KingdomsMod.LOG.error("Crown state could not be saved; pending changes retained",failure);}frozen.entrySet().removeIf(e->{Freeze f=e.getValue();if(!f.entity.isAlive()||f.entity.isRemoved()||service.now()>=f.until){if(!f.entity.isInPowderSnow)f.entity.setTicksFrozen(0);return true;}f.entity.setTicksFrozen(f.entity.getTicksRequiredToFreeze()+2);return false;});disabled.entrySet().removeIf(e->service.now()>=e.getValue());if(!healthy)return;ticks++;try{
        for(ServerPlayer player:service.server.getPlayerList().getPlayers())if(itemDisabled(player))voidCooldowns(player);
        if(ticks%10==0)for(ServerPlayer p:service.server.getPlayerList().getPlayers())armor.tick(p);
        if(ticks%20==0){for(ServerPlayer p:service.server.getPlayerList().getPlayers()){releaseAbsent(p);for(int i=0;i<p.getInventory().getContainerSize();i++)if(marked(p.getInventory().getItem(i)))observe(p,p.getInventory().getItem(i));}reconcile();if(dirty)save();}
    }catch(Exception e){healthy=false;KingdomsMod.LOG.error("Artifact state locked after storage failure",e);}}
    private void releaseAbsent(ServerPlayer p){leases.entrySet().removeIf(e->{Record r=data.records.get(e.getKey());if(r==null||!p.getUUID().equals(r.owner))return false;for(int i=0;i<p.getInventory().getContainerSize();i++)if(p.getInventory().getItem(i)==e.getValue())return false;for(var slot:p.containerMenu.slots)if(slot.getItem()==e.getValue())return false;return p.containerMenu.getCarried()!=e.getValue();});}
    public void destroyed(ItemStack stack){Record r=record(stack);if(r!=null&&lease(stack,r)){r.retired=true;leases.remove(r.id);save();}UUID id=tag(stack,"kingdoms_fixer");Fixer f=data.fixers.get(id);if(f!=null&&!f.consumed){f.consumed=true;save();}}
    public List<ItemStack> keepOnDeath(ServerPlayer p){return List.of();}
    public void respawn(ServerPlayer oldPlayer,ServerPlayer player){crown.sync(player);armor.leave(oldPlayer);guardian.clear(oldPlayer);disabled.remove(player.getUUID());frozen.remove(player.getUUID());}
    public void returnAfterDeath(ServerPlayer p,List<ItemStack> kept){}
    private Fixer validFixer(ItemStack stack){Fixer f=data.fixers.get(tag(stack,"kingdoms_fixer"));return f!=null&&!f.consumed&&stack.getCount()==1&&f.nonce.equals(tag(stack,"kingdoms_nonce"))?f:null;}
    public ItemStack repairPreview(ItemStack artifact,ItemStack fixer){Record r=record(artifact);if(!healthy||r==null||validFixer(fixer)==null)return ItemStack.EMPTY;
        if(r.type.equals("guardian_angel")?r.uses>=r.maxUses:!artifact.isDamaged())return ItemStack.EMPTY;
        Record next=cloneRecord(r);if(r.type.equals("guardian_angel"))next.uses=ArtifactRules.repairedUses(r,config().repairFraction);next.revision++;
        ItemStack result=artifact.copy();if(!r.type.equals("guardian_angel"))result.setDamageValue(Math.max(0,artifact.getDamageValue()-(int)Math.ceil(artifact.getMaxDamage()*config().repairFraction)));refresh(result,next);return result;
    }
    public Journal prepareRepair(ServerPlayer p,ItemStack artifact,ItemStack fixer){check();Record r=record(artifact);Fixer f=validFixer(fixer);if(r==null||f==null||repairPreview(artifact,fixer).isEmpty())throw new IllegalArgumentException("Invalid artifact repair");Journal j=new Journal();j.player=p.getUUID();j.receipt=UUID.randomUUID();j.next=cloneRecord(r);if(r.type.equals("guardian_angel"))j.next.uses=ArtifactRules.repairedUses(r,config().repairFraction);j.next.revision++;j.fixer=f.id;begin(j);return j;}
    private void begin(Journal j){check();if(service.store.read("artifact-transaction.json",Journal.class,null)!=null)throw new IllegalStateException("An artifact transaction needs recovery");service.store.write("artifact-transaction.json",j);}
    private boolean savedReceipt(ServerPlayer p,String receipt){try{var path=service.server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(p.getStringUUID()+".dat");CompoundTag saved=NbtIo.readCompressed(path,NbtAccounter.unlimitedHeap());return saved!=null&&Arrays.asList(saved.getStringOr("kingdoms_airdrop_receipts","").split(",")).contains(receipt);}catch(Exception e){throw new IllegalStateException("Cannot verify artifact transaction player save",e);}}
    public void finish(ServerPlayer p,Journal j){String receipt="artifact-"+j.receipt;var receipts=((AirDropReceipts)p).kingdoms$airDropReceipts();receipts.add(receipt);((AirDropPlayerSaveInvoker)service.server.getPlayerList()).kingdoms$saveAirDropPlayer(p);if(!savedReceipt(p,receipt)){healthy=false;throw new IllegalStateException("Artifact transaction awaits verified player-save recovery");}commitJournal(j);receipts.remove(receipt);}
    private void commitJournal(Journal j){data.records.put(j.next.id,j.next);if(j.fixer!=null)data.fixers.get(j.fixer).consumed=true;save();try{java.nio.file.Files.deleteIfExists(service.store.dir.resolve("artifact-transaction.json"));}catch(Exception e){healthy=false;throw new IllegalStateException("Artifact journal cleanup failed",e);}leases.remove(j.next.id);}
    public void recover(ServerPlayer p){Journal j=service.store.read("artifact-transaction.json",Journal.class,null);if(j==null)return;if(!j.player.equals(p.getUUID()))return;healthy=true;if(savedReceipt(p,"artifact-"+j.receipt)){commitJournal(j);((AirDropReceipts)p).kingdoms$airDropReceipts().remove("artifact-"+j.receipt);}else{try{java.nio.file.Files.deleteIfExists(service.store.dir.resolve("artifact-transaction.json"));}catch(Exception e){healthy=false;throw new IllegalStateException(e);}save();}}
    public void upgrade(ServerPlayer p){check();ItemStack held=p.getMainHandItem();Record r=record(held);if(r==null||!lease(held,r))throw new IllegalArgumentException("Hold a genuine artifact in your main hand");if(!config().types.get(r.type).netheriteUpgradeable)throw new IllegalArgumentException("This artifact has no Netherite upgrade");if(r.upgraded)throw new IllegalArgumentException("This artifact is already Netherite");int count=0;for(int i=0;i<p.getInventory().getContainerSize();i++)if(p.getInventory().getItem(i).is(Items.NETHERITE_INGOT))count+=p.getInventory().getItem(i).getCount();if(count<config().upgradeIngots)throw new IllegalArgumentException("Requires "+config().upgradeIngots+" Netherite Ingots");Journal j=new Journal();j.player=p.getUUID();j.receipt=UUID.randomUUID();j.next=cloneRecord(r);j.next.upgraded=true;j.next.revision++;begin(j);int needed=config().upgradeIngots;for(int i=0;i<p.getInventory().getContainerSize()&&needed>0;i++){ItemStack s=p.getInventory().getItem(i);if(s.is(Items.NETHERITE_INGOT)){int n=Math.min(needed,s.getCount());s.shrink(n);needed-=n;}}String material=r.baseItem.replace("diamond_","netherite_");ItemStack result=held.transmuteCopy(item(material));refresh(result,j.next);if(result.isDamageableItem())result.setDamageValue(Math.min(result.getMaxDamage()-1,(int)Math.round((double)held.getDamageValue()/held.getMaxDamage()*result.getMaxDamage())));p.getInventory().setItem(p.getInventory().getSelectedSlot(),result);finish(p,j);Ui.tell(p,"Artifact upgraded to Netherite",false);}
    public void menu(ServerPlayer p){Ui.open(p,"artifacts",m->{ItemStack held=p.getMainHandItem();Record r=record(held);
        m.button(13,r==null?Items.BOOK:held.getItem(),r==null?"Hold an artifact":r.name,r==null?List.of("Hold an artifact to inspect or upgrade it"):List.of(description(r),r.type.equals("guardian_angel")?"Uses: "+r.uses+" / "+r.maxUses:"Durability: "+(held.getMaxDamage()-held.getDamageValue())+" / "+held.getMaxDamage()),null);
        boolean upgrade=r!=null&&!r.upgraded&&config().types.get(r.type).netheriteUpgradeable;
        m.button(29,upgrade?Items.NETHERITE_INGOT:Items.BARRIER,"Upgrade to Netherite",List.of("Cost: "+config().upgradeIngots+" Netherite Ingots","Keeps enchantments and remaining durability percentage"),upgrade?()->{upgrade(p);menu(p);}:null);
        m.button(33,Items.ANVIL,"Repair",List.of("Tools accept normal compatible repairs and enchantments","A fixer restores "+Math.round(config().repairFraction*100)+"% in an anvil","Guardian Angel uses charges instead of durability"),null);m.back(p);},false);}
    public void give(ServerPlayer p,String key){check();ArtifactConfig.Type type=config().types.get(key);if(type==null||!type.enabled)throw new IllegalArgumentException("Unknown or disabled artifact");if(p.getInventory().getFreeSlot()<0)throw new IllegalArgumentException("Keep an empty inventory slot");Record r=ArtifactRules.generate(key,type,random);r.owner=p.getUUID();r.delivered=true;data.records.put(r.id,r);save();p.getInventory().add(create(r));((AirDropPlayerSaveInvoker)service.server.getPlayerList()).kingdoms$saveAirDropPlayer(p);}
    public void retire(UUID id){check();Record r=data.records.get(id);if(r==null)throw new IllegalArgumentException("Unknown artifact ID");r.retired=true;save();leases.remove(id);}
    public String inspect(ServerPlayer p){Record r=record(p.getMainHandItem());if(r==null)throw new IllegalArgumentException("Hold a genuine artifact");return r.name+" • id "+r.id+" • "+(r.type.equals("guardian_angel")?"uses "+r.uses+" / "+r.maxUses:"durability "+(p.getMainHandItem().getMaxDamage()-p.getMainHandItem().getDamageValue())+" / "+p.getMainHandItem().getMaxDamage());}
    public void giveFixer(ServerPlayer p){check();if(p.getInventory().getFreeSlot()<0)throw new IllegalArgumentException("Keep an empty inventory slot");Fixer f=new Fixer();data.fixers.put(f.id,f);save();p.getInventory().add(fixer(f));((AirDropPlayerSaveInvoker)service.server.getPlayerList()).kingdoms$saveAirDropPlayer(p);}
    public void recoverStorage(){Data loaded=service.store.read("artifacts.json",Data.class,new Data());ArtifactRules.validate(loaded);data=loaded;healthy=service.store.read("artifact-transaction.json",Journal.class,null)==null;leases.clear();if(!healthy)throw new IllegalArgumentException("A pending artifact transaction needs its player to reconnect for recovery");save();}
    public long count(){return data.records.values().stream().filter(r->!r.retired).count();}
    public void stop(){for(ServerPlayer player:service.server.getPlayerList().getPlayers())armor.leave(player);guardian.stop();if(healthy&&dirty)save();}
}

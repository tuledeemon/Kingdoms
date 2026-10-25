package com.celik.kingdoms;

import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import java.util.*;
import java.util.function.BiConsumer;
import static com.celik.kingdoms.ArtifactRules.*;
import com.celik.kingdoms.ArtifactRules.Record;

final class ExpandedArtifactsIntegration {
    static void run(KingdomService s,ServerPlayer p,ArtifactConfig cfg,BiConsumer<Boolean,String> check){
        ItemStack[] inventory=new ItemStack[p.getInventory().getContainerSize()];for(int i=0;i<inventory.length;i++)inventory[i]=p.getInventory().getItem(i).copy();
        var position=p.position();float health=p.getHealth();var mode=p.gameMode.getGameModeForPlayer();var effects=p.getActiveEffects().stream().map(MobEffectInstance::new).toList();
        try{
            p.closeContainer();s.artifacts.leave(p);p.getInventory().clearContent();p.removeAllEffects();p.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
            armor(s,p,cfg,check);CrownLocatorIntegration.run(s,p,cfg,check);p.removeAllEffects();s.artifacts.armor.leave(p);p.getInventory().clearContent();
            bow(s,p,cfg,check);p.getInventory().clearContent();mining(s,p,cfg,check);
            var flags=new HashMap<String,Boolean>();cfg.types.forEach((key,value)->{flags.put(key,value.enabled);value.enabled=false;});
            try{cfg.types.get("golden_crown").enabled=true;check.accept(s.artifacts.roll(UUID.randomUUID(),999,new Random(4))==null,"Golden Crown cannot be rolled even as the only enabled type");p.getInventory().clearContent();s.artifacts.give(p,"golden_crown");check.accept(java.util.stream.IntStream.range(0,p.getInventory().getContainerSize()).anyMatch(i->{Record r=s.artifacts.record(p.getInventory().getItem(i));return r!=null&&r.type.equals("golden_crown");}),"staff give can issue an event-only Golden Crown");}
            finally{flags.forEach((key,value)->cfg.types.get(key).enabled=value);}
        }finally{s.artifacts.armor.leave(p);s.artifacts.leave(p);p.stopUsingItem();p.removeAllEffects();effects.forEach(p::addEffect);for(int i=0;i<inventory.length;i++)p.getInventory().setItem(i,inventory[i]);p.setPos(position);p.setHealth(health);p.gameMode.changeGameModeForPlayer(mode);}
    }
    private static ItemStack issue(KingdomService s,ArtifactConfig cfg,String type){return s.artifacts.create(ArtifactIntegration.issue(s.artifacts,cfg,type));}
    private static void armor(KingdomService s,ServerPlayer p,ArtifactConfig cfg,BiConsumer<Boolean,String> check){
        ItemStack boots=issue(s,cfg,"runners_boots"),chest=issue(s,cfg,"kings_chestplate"),crown=issue(s,cfg,"golden_crown");
        p.setItemSlot(EquipmentSlot.FEET,boots);p.setItemSlot(EquipmentSlot.CHEST,chest);p.setItemSlot(EquipmentSlot.HEAD,crown);s.artifacts.armor.tick(p);
        check.accept(p.hasEffect(MobEffects.SPEED)&&p.getEffect(MobEffects.SPEED).getAmplifier()==0&&p.hasEffect(MobEffects.REGENERATION)&&p.getEffect(MobEffects.REGENERATION).getAmplifier()==0&&!p.hasEffect(MobEffects.STRENGTH),"boots and chestplate grant their worn effects while the crown no longer grants Strength");
        p.setHealth(10);for(int tick=0;tick<100;tick++){if(tick%10==0)s.artifacts.armor.tick(p);p.getEffect(MobEffects.REGENERATION).tickServer(p.level(),p,()->{});}
        check.accept(p.getHealth()==12,"King's Chestplate heals at vanilla Regeneration I cadence rather than healing every refresh");
        p.addEffect(new MobEffectInstance(MobEffects.SPEED,900,1));p.addEffect(new MobEffectInstance(MobEffects.REGENERATION,900,1));s.artifacts.armor.tick(p);
        check.accept(p.getEffect(MobEffects.SPEED).getAmplifier()==1&&p.getEffect(MobEffects.SPEED).getDuration()==900&&p.getEffect(MobEffects.REGENERATION).getAmplifier()==1,"Speed II and Regeneration II potions retain their level and duration over armor effects");
        check.accept(((com.celik.kingdoms.mixin.ArtifactEffectAccessor)(Object)p.getEffect(MobEffects.SPEED)).kingdoms$hiddenEffect()==null&&((com.celik.kingdoms.mixin.ArtifactEffectAccessor)(Object)p.getEffect(MobEffects.REGENERATION)).kingdoms$hiddenEffect()==null,"stronger potion stacks do not retain hidden artifact effects after gear removal");
        p.setItemSlot(EquipmentSlot.FEET,ItemStack.EMPTY);p.setItemSlot(EquipmentSlot.CHEST,ItemStack.EMPTY);p.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);s.artifacts.armor.tick(p);
        check.accept(p.hasEffect(MobEffects.SPEED)&&p.getEffect(MobEffects.SPEED).getAmplifier()==1&&p.hasEffect(MobEffects.REGENERATION)&&!p.hasEffect(MobEffects.STRENGTH),"unequipping removes only owned armor effects and preserves stronger potions");
        p.removeAllEffects();p.addEffect(new MobEffectInstance(MobEffects.SPEED,900,0));p.setItemSlot(EquipmentSlot.FEET,boots);s.artifacts.armor.tick(p);p.setItemSlot(EquipmentSlot.FEET,ItemStack.EMPTY);s.artifacts.armor.tick(p);
        check.accept(p.getEffect(MobEffects.SPEED).getDuration()==900,"ordinary longer Speed I potions are not shortened or removed by boots");
        p.addEffect(new MobEffectInstance(MobEffects.SPEED,20,1));p.setItemSlot(EquipmentSlot.FEET,boots);s.artifacts.armor.tick(p);p.setItemSlot(EquipmentSlot.FEET,ItemStack.EMPTY);s.artifacts.armor.tick(p);check.accept(((com.celik.kingdoms.mixin.ArtifactEffectAccessor)(Object)p.getEffect(MobEffects.SPEED)).kingdoms$hiddenEffect()!=null,"legitimate hidden potion effects remain intact when the armor did not own them");
        check.accept(crown.is(Items.DIAMOND_HELMET)&&crown.getMaxDamage()==new ItemStack(Items.DIAMOND_HELMET).getMaxDamage()&&Objects.equals(crown.get(DataComponents.ATTRIBUTE_MODIFIERS),new ItemStack(Items.DIAMOND_HELMET).get(DataComponents.ATTRIBUTE_MODIFIERS)),"Golden Crown retains native Diamond Helmet armor stats and durability");
        check.accept(crown.get(DataComponents.ITEM_MODEL).toString().equals("minecraft:golden_helmet")&&crown.get(DataComponents.EQUIPPABLE).assetId().orElseThrow().equals(net.minecraft.world.item.equipment.EquipmentAssets.GOLD),"Golden Crown uses built-in gold-helmet item and worn assets without a resource pack");
        var ench=s.server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);var book=new AirDropConfig.Loot("minecraft:enchanted_book",1,1,1);book.enchantments.put("minecraft:respiration",3);
        var anvil=new AnvilMenu(601,p.getInventory());anvil.getSlot(0).set(crown);anvil.getSlot(1).set(AirDropLoot.template(s.server,book));check.accept(anvil.getSlot(2).hasItem()&&s.artifacts.record(anvil.getSlot(2).getItem())!=null,"native helmet enchantment books remain compatible with the crown");
        EnchantmentHelper.updateEnchantments(crown,m->m.set(ench.getOrThrow(Enchantments.MENDING),1));crown.setDamageValue(100);p.setItemInHand(InteractionHand.MAIN_HAND,crown);p.getInventory().setItem(9,new ItemStack(Items.NETHERITE_INGOT,3));s.artifacts.upgrade(p);ItemStack upgraded=p.getMainHandItem();
        check.accept(upgraded.is(Items.NETHERITE_HELMET)&&p.getInventory().getItem(9).isEmpty()&&Objects.equals(upgraded.get(DataComponents.ATTRIBUTE_MODIFIERS),new ItemStack(Items.NETHERITE_HELMET).get(DataComponents.ATTRIBUTE_MODIFIERS)),"crown upgrade consumes exactly three ingots and gains native Netherite armor stats");
        check.accept(upgraded.getDamageValue()==Math.round(100d/363*407)&&EnchantmentHelper.getItemEnchantmentLevel(ench.getOrThrow(Enchantments.MENDING),upgraded)==1&&upgraded.get(DataComponents.EQUIPPABLE).assetId().orElseThrow().equals(net.minecraft.world.item.equipment.EquipmentAssets.GOLD),"crown upgrade preserves wear percentage, enchantments and golden appearance");
    }
    private static void bow(KingdomService s,ServerPlayer p,ArtifactConfig cfg,BiConsumer<Boolean,String> check){
        ItemStack bow=issue(s,cfg,"archers_bow");p.setItemInHand(InteractionHand.MAIN_HAND,bow);p.getInventory().setItem(9,new ItemStack(Items.ARROW,5));
        var ench=s.server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);EnchantmentHelper.updateEnchantments(bow,m->{m.set(ench.getOrThrow(Enchantments.POWER),3);m.set(ench.getOrThrow(Enchantments.FLAME),1);});
        var beforeFirst=arrows(p);p.startUsingItem(InteractionHand.MAIN_HAND);boolean fired=((BowItem)Items.BOW).releaseUsing(bow,p.level(),p,71980);p.stopUsingItem();var fresh=arrows(p).stream().filter(a->!beforeFirst.contains(a)).toList();
        check.accept(fired&&fresh.size()==2&&p.getInventory().countItem(Items.ARROW)==4,"actual Archer's Bow release fires two arrows while consuming one ammunition");
        check.accept(fresh.stream().filter(a->a.pickup==AbstractArrow.Pickup.ALLOWED).count()==1&&fresh.stream().allMatch(a->a.isOnFire()),"extra arrow cannot duplicate ammunition and both arrows retain native Flame");
        check.accept(bow.getDamageValue()==2,"Archer's Bow uses native durability for both projectiles");fresh.forEach(a->a.discard());
        EnchantmentHelper.updateEnchantments(bow,m->m.set(ench.getOrThrow(Enchantments.INFINITY),1));var beforeInfinity=arrows(p);p.startUsingItem(InteractionHand.MAIN_HAND);((BowItem)Items.BOW).releaseUsing(bow,p.level(),p,71980);p.stopUsingItem();var infinite=arrows(p).stream().filter(a->!beforeInfinity.contains(a)).toList();
        check.accept(infinite.size()==2&&p.getInventory().countItem(Items.ARROW)==4&&infinite.stream().noneMatch(a->a.pickup==AbstractArrow.Pickup.ALLOWED),"Infinity still fires two non-collectable arrows without spending ammunition");infinite.forEach(a->a.discard());
        ItemStack ordinary=new ItemStack(Items.BOW);p.setItemInHand(InteractionHand.MAIN_HAND,ordinary);var beforeNormal=arrows(p);p.startUsingItem(InteractionHand.MAIN_HAND);((BowItem)Items.BOW).releaseUsing(ordinary,p.level(),p,71980);p.stopUsingItem();var normal=arrows(p).stream().filter(a->!beforeNormal.contains(a)).toList();check.accept(normal.size()==1,"ordinary bows still fire one arrow");normal.forEach(a->a.discard());
    }
    private static Set<AbstractArrow> arrows(ServerPlayer p){return new HashSet<>(p.level().getEntitiesOfClass(AbstractArrow.class,new AABB(p.blockPosition()).inflate(12)));}
    private static void mining(KingdomService s,ServerPlayer p,ArtifactConfig cfg,BiConsumer<Boolean,String> check){
        ServerLevel level=p.level();BlockPos base=new BlockPos(9128,210,7048);level.getChunk(base);level.getChunk(base.east(16));level.getChunk(base.west(16));
        level.getEntitiesOfClass(ItemEntity.class,new AABB(base).inflate(16)).forEach(ItemEntity::discard);
        p.setPos(base.getX()+.5,base.getY(),base.getZ()+3);ItemStack axe=issue(s,cfg,"advanced_axe");p.setItemInHand(InteractionHand.MAIN_HAND,axe);
        try(var scope=Protection.scope(level,base,null,null,Protection.Cause.BLOCK)){for(int i=0;i<4;i++)level.setBlockAndUpdate(base.above(i),Blocks.OAK_LOG.defaultBlockState());level.setBlockAndUpdate(base.above(4),Blocks.OAK_LEAVES.defaultBlockState());}
        check.accept(level.getBlockState(base).getDestroyProgress(p,level,base)==1,"Advanced Axe starts tree mining instantly");
        check.accept(p.gameMode.destroyBlock(base)&&java.util.stream.IntStream.range(0,4).allMatch(i->level.getBlockState(base.above(i)).isAir())&&level.getBlockState(base.above(4)).is(Blocks.OAK_LEAVES),"Advanced Axe fells the connected trunk while leaving normal leaf decay");
        check.accept(axe.getDamageValue()==4,"tree harvesting pays ordinary tool durability per broken log");
        BlockPos placed=base.east(4);p.setPos(placed.getX()+.5,placed.getY(),placed.getZ()+3);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.OAK_LOG,3));
        try(var scope=Protection.scope(level,placed,p.getUUID(),p,Protection.Cause.PLAYER)){level.setBlockAndUpdate(placed.below(),Blocks.STONE.defaultBlockState());for(int i=0;i<3;i++){level.setBlockAndUpdate(placed.above(i),Blocks.AIR.defaultBlockState());var support=placed.above(i).below();var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(support).add(0,.5,0),Direction.UP,support,false);((BlockItem)Items.OAK_LOG).place(new net.minecraft.world.item.context.BlockPlaceContext(p,InteractionHand.MAIN_HAND,p.getMainHandItem(),hit));}level.setBlockAndUpdate(placed.above(3),Blocks.OAK_LEAVES.defaultBlockState());}
        check.accept(s.artifacts.placed(level,placed),"actual BlockItem placement records player-built logs");p.setItemInHand(InteractionHand.MAIN_HAND,axe);p.gameMode.destroyBlock(placed);check.accept(level.getBlockState(placed.above()).is(Blocks.OAK_LOG),"placed-log protection prevents felling a player's construction");
        BlockPos grown=base.east(7);try(var scope=Protection.scope(level,grown,p.getUUID(),p,Protection.Cause.PLAYER)){level.setBlockAndUpdate(grown,Blocks.OAK_LOG.defaultBlockState());}check.accept(!s.artifacts.placed(level,grown),"tree generation in a player's interaction scope is not mistaken for direct block placement");
        BlockPos ore=base.west(4);ItemStack pick=issue(s,cfg,"advanced_pickaxe");var ench=s.server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);EnchantmentHelper.updateEnchantments(pick,m->m.set(ench.getOrThrow(Enchantments.SILK_TOUCH),1));p.setItemInHand(InteractionHand.MAIN_HAND,pick);p.setPos(ore.getX()+.5,ore.getY(),ore.getZ()+3);
        try(var scope=Protection.scope(level,ore,null,null,Protection.Cause.BLOCK)){level.setBlockAndUpdate(ore,Blocks.COAL_ORE.defaultBlockState());level.setBlockAndUpdate(ore.above(),Blocks.DEEPSLATE_COAL_ORE.defaultBlockState());level.setBlockAndUpdate(ore.above(2),Blocks.COAL_ORE.defaultBlockState());level.setBlockAndUpdate(ore.east(),Blocks.IRON_ORE.defaultBlockState());}
        check.accept(level.getBlockState(ore).getDestroyProgress(p,level,ore)==1,"Advanced Pickaxe starts ore mining instantly");
        check.accept(p.gameMode.destroyBlock(ore)&&level.getBlockState(ore.above()).isAir()&&level.getBlockState(ore.above(2)).isAir()&&level.getBlockState(ore.east()).is(Blocks.IRON_ORE)&&pick.getDamageValue()==3,"ore vein includes deepslate variants, excludes other minerals and uses native wear");
        var loot=level.getEntitiesOfClass(ItemEntity.class,new AABB(ore).inflate(4));check.accept(loot.stream().filter(e->e.getItem().is(Items.COAL_ORE)||e.getItem().is(Items.DEEPSLATE_COAL_ORE)).mapToInt(e->e.getItem().getCount()).sum()==3,"vein mining honors Silk Touch through ordinary block loot");
        BlockPos capped=base.north(5);int limit=cfg.advancedOreMaxBlocks;cfg.advancedOreMaxBlocks=2;
        try{for(int i=0;i<3;i++)try(var scope=Protection.scope(level,capped,null,null,Protection.Cause.BLOCK)){level.setBlockAndUpdate(capped.above(i),Blocks.GOLD_ORE.defaultBlockState());}p.setPos(capped.getX()+.5,capped.getY(),capped.getZ()+3);p.gameMode.destroyBlock(capped);check.accept(level.getBlockState(capped).isAir()&&level.getBlockState(capped.above()).isAir()&&level.getBlockState(capped.above(2)).is(Blocks.GOLD_ORE),"configured vein limit counts the original block and bounds the chain");}finally{cfg.advancedOreMaxBlocks=limit;}
        BlockPos fragile=base.south(5);for(int i=0;i<3;i++)try(var scope=Protection.scope(level,fragile,null,null,Protection.Cause.BLOCK)){level.setBlockAndUpdate(fragile.above(i),Blocks.COPPER_ORE.defaultBlockState());}ItemStack breaking=issue(s,cfg,"advanced_pickaxe");breaking.setDamageValue(breaking.getMaxDamage()-2);p.setItemInHand(InteractionHand.MAIN_HAND,breaking);p.setPos(fragile.getX()+.5,fragile.getY(),fragile.getZ()+3);p.gameMode.destroyBlock(fragile);check.accept(breaking.isEmpty()&&level.getBlockState(fragile.above()).isAir()&&level.getBlockState(fragile.above(2)).is(Blocks.COPPER_ORE),"vein harvesting stops as soon as normal durability breaks the tool");p.setItemInHand(InteractionHand.MAIN_HAND,pick);
        var saved=s.store.copy(s.state);boolean protect=cfg.advancedProtectPlacedBlocks;
        try{cfg.advancedProtectPlacedBlocks=false;BlockPos edge=new BlockPos(9135,210,7064);level.getChunk(edge);level.getChunk(edge.east());UUID owner=UUID.randomUUID();var foreign=new Rules.Kingdom();foreign.name="Advanced Guard";foreign.members.put(owner,"Guard");foreign.mayors.add(owner);foreign.claims.add(KingdomService.key(level,edge.east()));foreign.nextUpkeep=s.now()+86400000;var next=s.store.copy(s.state);next.kingdoms.put(foreign.id,foreign);s.commit(next);
            for(BlockPos pos:List.of(edge,edge.above(),edge.east()))try(var scope=Protection.scope(level,pos,owner,null,Protection.Cause.PLAYER)){level.setBlockAndUpdate(pos,Blocks.DIAMOND_ORE.defaultBlockState());}
            p.setPos(edge.getX()+.5,edge.getY(),edge.getZ()+3);int damage=pick.getDamageValue();p.gameMode.destroyBlock(edge);
            check.accept(level.getBlockState(edge.above()).isAir()&&level.getBlockState(edge.east()).is(Blocks.DIAMOND_ORE)&&pick.getDamageValue()==damage+2,"Advanced Pickaxe stops at foreign protected claims without charging wear for denied blocks");
        }finally{cfg.advancedProtectPlacedBlocks=protect;s.commit(saved);}
    }
    private ExpandedArtifactsIntegration(){}
}

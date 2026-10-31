package com.celik.kingdoms;

import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import java.util.*;
import java.util.function.BiConsumer;

final class ReplantHoeIntegration {
    static void run(KingdomService s,ServerPlayer p,ArtifactConfig cfg,BiConsumer<Boolean,String> check){
        var level=p.level();var mode=p.gameMode.getGameModeForPlayer();var position=p.position();
        ItemStack held=p.getMainHandItem();var saved=s.store.copy(s.state);
        BlockPos pos=new BlockPos(9180,210,7080);level.getChunk(pos);
        ItemStack hoe=s.artifacts.create(ArtifactIntegration.issue(s.artifacts,cfg,"replant_hoe"));
        try{
            p.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);p.setItemInHand(InteractionHand.MAIN_HAND,hoe);p.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+2);
            var crops=List.of(((CropBlock)Blocks.WHEAT).getStateForAge(7),((CropBlock)Blocks.CARROTS).getStateForAge(7),((CropBlock)Blocks.POTATOES).getStateForAge(7),((CropBlock)Blocks.BEETROOTS).getStateForAge(3),Blocks.NETHER_WART.defaultBlockState().setValue(NetherWartBlock.AGE,3),Blocks.COCOA.defaultBlockState().setValue(CocoaBlock.AGE,2));
            for(BlockState crop:crops){
                setup(level,pos,crop);Item seed=ReplantHoe.seed(crop);
                // This is a query only: it must not consume seeds or mutate the crop.
                Block.getDrops(crop,level,pos,null,p,hoe);
                check.accept(level.getBlockState(pos).equals(crop),"Replant Hoe loot preview leaves crop unchanged: "+crop.getBlock());
                level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
                var loot=new ArrayList<>(List.of(new ItemStack(seed,4),new ItemStack(Items.DIAMOND,2)));
                ReplantHoe.harvest(crop,level,pos,p,loot);
                check.accept(level.getBlockState(pos).equals(ReplantHoe.young(crop))&&loot.getFirst().getCount()==3&&loot.get(1).getCount()==2,"replant reserves exactly one matching seed and preserves other drops: "+crop.getBlock());
                ReplantHoe.harvest(crop,level,pos,p,loot);
                check.accept(loot.getFirst().getCount()==3,"duplicate callback cannot consume or replant twice");
                setup(level,pos,crop);check.accept(p.gameMode.destroyBlock(pos)&&level.getBlockState(pos).equals(ReplantHoe.young(crop)),"native crop break replants through emission hook: "+crop.getBlock());
            }
            BlockState mature=((CropBlock)Blocks.CARROTS).getStateForAge(7);
            setup(level,pos,mature);level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
            var none=new ArrayList<ItemStack>();ReplantHoe.harvest(mature,level,pos,p,none);check.accept(level.getBlockState(pos).isAir(),"no harvested seed means no free plant");
            var loot=new ArrayList<>(List.of(new ItemStack(Items.CARROT,3)));
            level.setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());ReplantHoe.harvest(mature,level,pos,p,loot);check.accept(level.getBlockState(pos).isAir()&&loot.getFirst().getCount()==3,"invalid soil preserves all loot");
            setup(level,pos,((CropBlock)Blocks.CARROTS).getStateForAge(0));p.gameMode.destroyBlock(pos);check.accept(level.getBlockState(pos).isAir(),"immature crop is not replanted");
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND_HOE));setup(level,pos,mature);p.gameMode.destroyBlock(pos);check.accept(level.getBlockState(pos).isAir(),"ordinary hoe has vanilla behavior");
            p.setItemInHand(InteractionHand.MAIN_HAND,hoe);cfg.types.get("replant_hoe").enabled=false;setup(level,pos,mature);p.gameMode.destroyBlock(pos);check.accept(level.getBlockState(pos).isAir(),"disabled artifact does not replant");cfg.types.get("replant_hoe").enabled=true;
            p.gameMode.changeGameModeForPlayer(GameType.CREATIVE);setup(level,pos,mature);p.gameMode.destroyBlock(pos);check.accept(level.getBlockState(pos).isAir(),"creative breaking cannot create free seedlings");p.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
            UUID owner=UUID.randomUUID();var kingdom=new Rules.Kingdom();kingdom.name="Replant Guard";kingdom.members.put(owner,"Guard");kingdom.mayors.add(owner);kingdom.claims.add(KingdomService.key(level,pos));kingdom.nextUpkeep=s.now()+86400000;kingdom.grants.put(p.getUUID(),EnumSet.of(Rules.Permission.BREAK));var next=s.store.copy(s.state);next.kingdoms.put(kingdom.id,kingdom);s.commit(next);
            setup(level,pos,mature);check.accept(p.gameMode.destroyBlock(pos)&&level.getBlockState(pos).isAir(),"break-only permission harvests without unauthorized planting");
            next=s.store.copy(s.state);next.kingdoms.get(kingdom.id).grants.clear();s.commit(next);setup(level,pos,mature);check.accept(!p.gameMode.destroyBlock(pos)&&level.getBlockState(pos).equals(mature),"denied break leaves crop untouched");s.commit(saved);
            var rules=level.getGameRules();boolean blockDrops=rules.get(net.minecraft.world.level.gamerules.GameRules.BLOCK_DROPS);
            try{rules.set(net.minecraft.world.level.gamerules.GameRules.BLOCK_DROPS,false,s.server);setup(level,pos,mature);p.gameMode.destroyBlock(pos);check.accept(level.getBlockState(pos).isAir(),"disabled block drops cannot create a free replanted crop");}finally{rules.set(net.minecraft.world.level.gamerules.GameRules.BLOCK_DROPS,blockDrops,s.server);}
            var ench=s.server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);EnchantmentHelper.updateEnchantments(hoe,m->m.set(ench.getOrThrow(Enchantments.FORTUNE),3));
            setup(level,pos,mature);check.accept(p.gameMode.destroyBlock(pos)&&level.getBlockState(pos).equals(ReplantHoe.young(mature)),"Fortune III uses native harvest and still leaves exactly one young crop");
            // 100 consecutive player-like harvests: one live plant per harvest, no async/shared reservation.
            for(int i=0;i<100;i++){setup(level,pos,mature);if(!p.gameMode.destroyBlock(pos)||!level.getBlockState(pos).equals(ReplantHoe.young(mature)))throw new AssertionError("replant batch "+i);}
            check.accept(true,"100 sequential harvests retain one plant with no pending replant work");
        }finally{cfg.types.get("replant_hoe").enabled=true;s.commit(saved);level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(4)).forEach(ItemEntity::discard);p.setItemInHand(InteractionHand.MAIN_HAND,held);p.setPos(position);p.gameMode.changeGameModeForPlayer(mode);}
    }
    private static void setup(ServerLevel level,BlockPos pos,BlockState crop){
        try(var scope=Protection.scope(level,pos,null,null,Protection.Cause.BLOCK)){
            level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(pos.below(),crop.is(Blocks.NETHER_WART)?Blocks.SOUL_SAND.defaultBlockState():Blocks.FARMLAND.defaultBlockState());
            if(crop.is(Blocks.COCOA))level.setBlockAndUpdate(pos.relative(crop.getValue(CocoaBlock.FACING)),Blocks.JUNGLE_LOG.defaultBlockState());
            level.setBlockAndUpdate(pos,crop);
        }
    }
    private ReplantHoeIntegration(){}
}

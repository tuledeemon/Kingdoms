package com.celik.kingdoms;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

/** Bounded scans over loaded terrain; each block is still broken through native player mining. */
final class ArtifactChains {
    static boolean ore(BlockState state){String path=BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();return path.endsWith("_ore")||path.equals("ancient_debris");}
    private static String oreKind(BlockState state){return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString().replace("deepslate_","");}
    static List<BlockPos> collect(KingdomService service,Artifacts artifacts,ServerPlayer player,BlockPos origin,BlockState initial,String type){
        boolean tree=type.equals("advanced_axe");if(tree?!initial.is(BlockTags.LOGS):!ore(initial))return List.of();
        ArtifactConfig config=service.config.artifacts;if(config.advancedProtectPlacedBlocks&&artifacts.placed(player.level(),origin))return List.of();
        int limit=tree?config.advancedTreeMaxBlocks:config.advancedOreMaxBlocks,radius=config.advancedChainRadius;
        List<BlockPos> found=new ArrayList<>();Set<BlockPos> seen=new HashSet<>();ArrayDeque<BlockPos> queue=new ArrayDeque<>();queue.add(origin);seen.add(origin);boolean leaves=false;
        while(!queue.isEmpty()&&found.size()<limit){
            BlockPos pos=queue.removeFirst();found.add(pos);
            for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++){
                if(x==0&&y==0&&z==0)continue;BlockPos next=pos.offset(x,y,z);
                if(Math.abs(next.getX()-origin.getX())>radius||Math.abs(next.getY()-origin.getY())>radius||Math.abs(next.getZ()-origin.getZ())>radius||!seen.add(next))continue;
                if(player.level().getChunkSource().getChunkNow(next.getX()>>4,next.getZ()>>4)==null||!player.level().getWorldBorder().isWithinBounds(next))continue;
                BlockState block=player.level().getBlockState(next);if(tree&&block.is(BlockTags.LEAVES))leaves=true;
                if(tree?block.getBlock()!=initial.getBlock():!ore(block)||!oreKind(block).equals(oreKind(initial)))continue;
                if(block.getDestroySpeed(player.level(),next)<0||config.advancedProtectPlacedBlocks&&artifacts.placed(player.level(),next)||!Protection.permitted(player.level(),next,player.getUUID(),Rules.Permission.BREAK)||service.airDrops.protectedAt(player.level(),next)||service.airDrops.chests.protects(player.level(),next)||service.cores.at(player.level(),next)!=null)continue;
                if(found.size()+queue.size()<limit)queue.addLast(next);
            }
        }
        return tree&&config.advancedTreeRequireLeaves&&!leaves?List.of():found.stream().filter(p->!p.equals(origin)).toList();
    }
    private ArtifactChains(){}
}

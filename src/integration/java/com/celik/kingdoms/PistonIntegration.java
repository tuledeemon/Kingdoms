package com.celik.kingdoms;

import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.piston.*;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;
import java.util.function.BiConsumer;

final class PistonIntegration {
    static void run(KingdomService service,ServerLevel level,ServerPlayer mayor,BiConsumer<Boolean,String> check){
        var original=service.store.copy(service.state);
        BlockPos piston=new BlockPos(16,245,8);level.getChunkAt(piston);
        try{
            // Observer in chunk 0, piston in chunk 1: same kingdom, no flags enabled.
            service.edit(service.own(mayor.getUUID()).id,k->k.claims.add(KingdomService.key(level,piston)));
            check.accept(service.own(mayor.getUUID()).flags.isEmpty(),"machines need no enabled kingdom flags");
            cycles(level,mayor,piston,check,"same kingdom across chunk border");
            service.commit(original);
            // The observer is now claimed, while the piston and all moving blocks are wilderness.
            cycles(level,mayor,piston,check,"claimed observer powering wilderness piston");
            // The reverse ownership boundary rejects a complete move, without changing its base.
            BlockPos border=piston.west();clear(level,mayor,border);
            put(level,mayor,border,Blocks.STICKY_PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING,Direction.EAST));
            put(level,mayor,border.west(),Blocks.REDSTONE_BLOCK.defaultBlockState());
            boolean moved=level.getBlockState(border).triggerEvent(level,border,0,Direction.EAST.get3DDataValue());
            check.accept(!moved&&!level.getBlockState(border).getValue(PistonBaseBlock.EXTENDED)&&level.getBlockState(border.east()).isAir()&&level.getBlockEntity(border)==null,"forbidden border extension cancels before changing piston state");
            clear(level,mayor,border);
        }finally{service.commit(original);}
    }
    private static void cycles(ServerLevel level,ServerPlayer mayor,BlockPos pos,BiConsumer<Boolean,String> check,String label){
        clear(level,mayor,pos);
        put(level,mayor,pos,Blocks.STICKY_PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING,Direction.EAST));
        put(level,mayor,pos.east(),Blocks.STONE.defaultBlockState());
        put(level,mayor,pos.west(),Blocks.OBSERVER.defaultBlockState().setValue(ObserverBlock.FACING,Direction.WEST));
        for(int cycle=0;cycle<4;cycle++){
            put(level,mayor,pos.west(),level.getBlockState(pos.west()).setValue(ObserverBlock.POWERED,true));
            check.accept(level.getSignal(pos.west(),Direction.WEST)==15,label+": observer output reaches adjacent chunk");
            level.getBlockState(pos).triggerEvent(level,pos,0,Direction.EAST.get3DDataValue());
            check.accept(level.getBlockState(pos).getValue(PistonBaseBlock.EXTENDED)&&level.getBlockState(pos.east(2)).is(Blocks.MOVING_PISTON)&&level.getBlockEntity(pos.east(2)) instanceof PistonMovingBlockEntity,label+": extension creates matching moving block and entity");
            finish(level,pos);
            check.accept(level.getBlockState(pos.east(2)).is(Blocks.STONE)&&level.getBlockState(pos.east()).is(Blocks.PISTON_HEAD),label+": moving blocks finish in claimed or unclaimed terrain");
            put(level,mayor,pos.west(),level.getBlockState(pos.west()).setValue(ObserverBlock.POWERED,false));
            level.getBlockState(pos).triggerEvent(level,pos,1,Direction.EAST.get3DDataValue());
            check.accept(level.getBlockState(pos).is(Blocks.MOVING_PISTON)&&level.getBlockEntity(pos) instanceof PistonMovingBlockEntity,label+": retraction creates valid moving base before pulling");
            finish(level,pos);
            check.accept(level.getBlockState(pos).is(Blocks.STICKY_PISTON)&&!level.getBlockState(pos).getValue(PistonBaseBlock.EXTENDED)&&level.getBlockState(pos.east()).is(Blocks.STONE)&&level.getBlockState(pos.east(2)).isAir(),label+": repeated retraction restores base and pulled block without phantom entities");
        }
        clear(level,mayor,pos);
    }
    private static void finish(ServerLevel level,BlockPos pos){for(int tick=0;tick<4;tick++)for(int x=0;x<=3;x++){BlockPos target=pos.east(x);if(level.getBlockEntity(target) instanceof PistonMovingBlockEntity entity)PistonMovingBlockEntity.tick(level,target,level.getBlockState(target),entity);}}
    private static void clear(ServerLevel level,ServerPlayer mayor,BlockPos pos){for(int x=-1;x<=3;x++)put(level,mayor,pos.east(x),Blocks.AIR.defaultBlockState());}
    private static void put(ServerLevel level,ServerPlayer mayor,BlockPos pos,BlockState state){try(var scope=Protection.scope(level,pos,mayor.getUUID(),null,Protection.Cause.PLAYER)){level.setBlock(pos,state,2);}}
    private PistonIntegration(){}
}

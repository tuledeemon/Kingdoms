package com.celik.kingdoms;

import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.*;
import java.util.*;
import java.util.function.BiConsumer;

final class FluidIntegration {
    static void run(KingdomService service,ServerLevel level,ServerPlayer mayor,ServerPlayer outsider,BiConsumer<Boolean,String> check)throws Exception{
        var original=service.store.copy(service.state);boolean prevention=service.config.preventFluidGriefing;
        BlockPos inside=new BlockPos(15,238,8),outside=inside.east();
        try{
            check.accept(prevention,"fluid-grief prevention is enabled by default");
            spread(level,mayor,inside,outside,Direction.EAST);
            check.accept(level.getFluidState(outside).is(Fluids.FLOWING_WATER),"native water flow may leave a claim for wilderness");
            spread(level,mayor,outside,inside,Direction.WEST);
            check.accept(level.getFluidState(inside).isEmpty(),"native wilderness water cannot enter a protected claim");
            service.edit(service.own(mayor.getUUID()).id,k->k.claims.add(KingdomService.key(level,outside)));
            spread(level,mayor,inside,outside,Direction.EAST);
            check.accept(!level.getFluidState(outside).isEmpty(),"native flow crosses chunks with the same kingdom owner");
            BlockPos other=new BlockPos(32,238,8);level.getChunkAt(other);
            spread(level,mayor,other.west(),other,Direction.EAST);
            check.accept(level.getFluidState(other).isEmpty(),"native flow cannot cross into another kingdom");
            try(var scope=Protection.scope(level,other.west(),outsider.getUUID(),null,Protection.Cause.FLUID)){
                check.accept(!Protection.fluid(level,other.west(),other),"flow does not borrow the original placer's permissions");
            }
            BlockPos wild=new BlockPos(64,238,8);level.getChunkAt(wild);
            spread(level,mayor,wild,wild.east(),Direction.EAST);
            check.accept(!level.getFluidState(wild.east()).isEmpty(),"native wilderness flow remains normal");
            service.commit(original);
            BlockPos bucket=new BlockPos(8,238,8);put(level,mayor,bucket,Blocks.AIR.defaultBlockState());
            var water=(BucketItem)Items.WATER_BUCKET;
            try(var scope=Protection.scope(level,bucket,outsider.getUUID(),outsider,Protection.Cause.PLAYER)){
                check.accept(!water.emptyContents(outsider,level,bucket,null)&&level.getFluidState(bucket).isEmpty(),"outsider cannot pour a bucket without place permission");
            }
            service.edit(service.own(mayor.getUUID()).id,k->k.grants.put(outsider.getUUID(),EnumSet.of(Rules.Permission.PLACE)));
            try(var scope=Protection.scope(level,bucket,outsider.getUUID(),outsider,Protection.Cause.PLAYER)){
                check.accept(water.emptyContents(outsider,level,bucket,null)&&level.getFluidState(bucket).is(Fluids.WATER),"place permission alone authorizes native bucket placement");
            }
            outsider.setPos(bucket.getX()+.5,bucket.getY()+.5-outsider.getEyeHeight(),bucket.getZ()-2.5);outsider.setYRot(0);outsider.setXRot(0);outsider.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BUCKET));
            new net.minecraft.server.network.ServerGamePacketListenerImpl(service.server,new RecordingConnection(),outsider,net.minecraft.server.network.CommonListenerCookie.createInitial(outsider.getGameProfile(),false));
            try(var scope=Protection.scope(level,bucket,outsider.getUUID(),outsider,Protection.Cause.PLAYER)){
                InteractionResult result=((BucketItem)Items.BUCKET).use(level,outsider,InteractionHand.MAIN_HAND);
                check.accept(result==InteractionResult.FAIL&&level.getFluidState(bucket).is(Fluids.WATER),"place permission does not allow native bucket collection");
            }
            service.edit(service.own(mayor.getUUID()).id,k->k.grants.put(outsider.getUUID(),EnumSet.of(Rules.Permission.BREAK)));
            try(var scope=Protection.scope(level,bucket,outsider.getUUID(),outsider,Protection.Cause.PLAYER)){
                InteractionResult result=((BucketItem)Items.BUCKET).use(level,outsider,InteractionHand.MAIN_HAND);
                check.accept(result.consumesAction()&&level.getFluidState(bucket).isEmpty(),"break permission alone authorizes native bucket collection");
            }
            dispenser(service,level,mayor,inside,outside,check);
            service.commit(original);
            service.config.preventFluidGriefing=false;
            spread(level,mayor,outside,inside,Direction.WEST);
            check.accept(!level.getFluidState(inside).isEmpty(),"disabling fluid-grief prevention permits ordinary cross-owner flow");
        }finally{service.config.preventFluidGriefing=prevention;service.commit(original);}
    }
    private static void dispenser(KingdomService service,ServerLevel level,ServerPlayer mayor,BlockPos inside,BlockPos outside,BiConsumer<Boolean,String> check)throws Exception{
        var method=DispenserBlock.class.getDeclaredMethod("dispenseFrom",ServerLevel.class,BlockState.class,BlockPos.class);method.setAccessible(true);
        put(level,mayor,inside,Blocks.AIR.defaultBlockState());put(level,mayor,outside,Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING,Direction.WEST));
        var inventory=(DispenserBlockEntity)level.getBlockEntity(outside);inventory.setItem(0,new ItemStack(Items.WATER_BUCKET));
        method.invoke(Blocks.DISPENSER,level,level.getBlockState(outside),outside);
        check.accept(inventory.getItem(0).is(Items.WATER_BUCKET)&&level.getFluidState(inside).isEmpty(),"forbidden dispenser bucket is neither consumed nor ejected");
        inventory.setItem(0,new ItemStack(Items.ARROW));method.invoke(Blocks.DISPENSER,level,level.getBlockState(outside),outside);
        check.accept(inventory.getItem(0).isEmpty(),"ordinary dispenser projectiles are not blocked by a blanket border rule");
        inventory.setItem(0,new ItemStack(Items.WATER_BUCKET));service.config.preventFluidGriefing=false;
        method.invoke(Blocks.DISPENSER,level,level.getBlockState(outside),outside);
        check.accept(inventory.getItem(0).is(Items.BUCKET)&&level.getFluidState(inside).is(Fluids.WATER),"fluid setting also controls native dispenser buckets");
        service.config.preventFluidGriefing=true;
        put(level,mayor,outside,Blocks.AIR.defaultBlockState());put(level,mayor,inside,Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING,Direction.EAST));
        inventory=(DispenserBlockEntity)level.getBlockEntity(inside);inventory.setItem(0,new ItemStack(Items.WATER_BUCKET));method.invoke(Blocks.DISPENSER,level,level.getBlockState(inside),inside);
        check.accept(inventory.getItem(0).is(Items.BUCKET)&&level.getFluidState(outside).is(Fluids.WATER),"claimed dispenser may pour out into wilderness");
        put(level,mayor,inside,Blocks.DROPPER.defaultBlockState().setValue(DispenserBlock.FACING,Direction.EAST));put(level,mayor,outside,Blocks.CHEST.defaultBlockState());
        inventory=(DispenserBlockEntity)level.getBlockEntity(inside);inventory.setItem(0,new ItemStack(Items.DIAMOND));
        var drop=DropperBlock.class.getDeclaredMethod("dispenseFrom",ServerLevel.class,BlockState.class,BlockPos.class);drop.setAccessible(true);drop.invoke(Blocks.DROPPER,level,level.getBlockState(inside),inside);
        check.accept(inventory.getItem(0).isEmpty()&&((ChestBlockEntity)level.getBlockEntity(outside)).getItem(0).is(Items.DIAMOND),"claimed inventory machines may export into wilderness");
        put(level,mayor,inside,Blocks.AIR.defaultBlockState());put(level,mayor,outside,Blocks.AIR.defaultBlockState());
    }
    private static void spread(ServerLevel level,ServerPlayer mayor,BlockPos from,BlockPos to,Direction direction)throws Exception{
        put(level,mayor,from,Blocks.WATER.defaultBlockState());put(level,mayor,to,Blocks.AIR.defaultBlockState());
        var method=FlowingFluid.class.getDeclaredMethod("spreadTo",LevelAccessor.class,BlockPos.class,BlockState.class,Direction.class,FluidState.class);method.setAccessible(true);
        try(var scope=Protection.scope(level,from,null,null,Protection.Cause.FLUID)){method.invoke(Fluids.WATER,level,to,level.getBlockState(to),direction,Fluids.WATER.getFlowing(7,false));}
    }
    private static void put(ServerLevel level,ServerPlayer mayor,BlockPos pos,BlockState state){try(var scope=Protection.scope(level,pos,mayor.getUUID(),null,Protection.Cause.PLAYER)){level.setBlock(pos,state,2);}}
    private static final class RecordingConnection extends net.minecraft.network.Connection {
        RecordingConnection(){super(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);}
        @Override public void send(net.minecraft.network.protocol.Packet<?> packet,io.netty.channel.ChannelFutureListener listener,boolean flush){}
    }
    private FluidIntegration(){}
}

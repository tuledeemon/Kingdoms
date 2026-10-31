package com.celik.kingdoms;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;

/** Runs only at native drop emission, never during a loot-table preview. */
public final class ReplantHoe {
    static Item seed(BlockState state){
        if(state.is(Blocks.WHEAT)&&((CropBlock)Blocks.WHEAT).isMaxAge(state))return Items.WHEAT_SEEDS;
        if(state.is(Blocks.CARROTS)&&((CropBlock)Blocks.CARROTS).isMaxAge(state))return Items.CARROT;
        if(state.is(Blocks.POTATOES)&&((CropBlock)Blocks.POTATOES).isMaxAge(state))return Items.POTATO;
        if(state.is(Blocks.BEETROOTS)&&((CropBlock)Blocks.BEETROOTS).isMaxAge(state))return Items.BEETROOT_SEEDS;
        if(state.is(Blocks.NETHER_WART)&&state.getValue(NetherWartBlock.AGE)==3)return Items.NETHER_WART;
        if(state.is(Blocks.COCOA)&&state.getValue(CocoaBlock.AGE)==2)return Items.COCOA_BEANS;
        return null;
    }
    static BlockState young(BlockState state){
        if(state.getBlock() instanceof CropBlock crop)return crop.getStateForAge(0);
        if(state.is(Blocks.NETHER_WART))return state.setValue(NetherWartBlock.AGE,0);
        return state.setValue(CocoaBlock.AGE,0); // Preserve attachment direction.
    }
    public static List<ItemStack> harvest(BlockState old,ServerLevel level,BlockPos pos,Entity breaker,List<ItemStack> drops){
        var service=KingdomsMod.service;
        if(service==null||!level.getServer().isSameThread()||!(breaker instanceof ServerPlayer player)||player.level()!=level||player.isCreative()||player.isSpectator())return drops;
        var record=service.artifacts.usable(player.getMainHandItem());
        if(record==null||!record.type.equals("replant_hoe")||service.artifacts.preventsMining(player))return drops;
        if(!level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.BLOCK_DROPS))return drops;
        Item seed=seed(old);if(seed==null||!level.getBlockState(pos).isAir())return drops;
        if(!Protection.permitted(level,pos,player.getUUID(),Rules.Permission.BREAK)||!Protection.permitted(level,pos,player.getUUID(),Rules.Permission.PLACE))return drops;
        BlockState plant=young(old);if(!plant.canSurvive(level,pos))return drops;
        // Reserve from this one loot result before any world update can invoke other hooks.
        // No inventory writes, second loot roll, entities to collect, or deferred state.
        for(ItemStack stack:drops)if(stack.is(seed)&&!stack.isEmpty()){
            stack.shrink(1);
            if(!level.setBlock(pos,plant,Block.UPDATE_ALL))stack.grow(1);
            break;
        }
        return drops;
    }
    private ReplantHoe(){}
}

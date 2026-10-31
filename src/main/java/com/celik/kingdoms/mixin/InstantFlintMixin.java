package com.celik.kingdoms.mixin;

import com.celik.kingdoms.InstantFlint;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TntBlock.class)
public abstract class InstantFlintMixin {
    @WrapMethod(method="useItemOn")
    private InteractionResult use(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit,Operation<InteractionResult> original){
        return player instanceof ServerPlayer p?InstantFlint.use(p,stack,()->original.call(stack,state,level,pos,player,hand,hit)):original.call(stack,state,level,pos,player,hand,hit);
    }
    @WrapOperation(method="prime(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/LivingEntity;)Z",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private static boolean prime(Level level,Entity entity,Operation<Boolean> original){return InstantFlint.prime(entity,()->original.call(level,entity));}
}

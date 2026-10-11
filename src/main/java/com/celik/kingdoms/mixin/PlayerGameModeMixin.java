package com.celik.kingdoms.mixin;
import com.celik.kingdoms.*;
import com.celik.kingdoms.Rules.Permission;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ServerPlayerGameMode.class)
public abstract class PlayerGameModeMixin {
 @Shadow protected ServerPlayer player;
 @Shadow protected ServerLevel level;
 @Inject(method="handleBlockBreakAction",at=@At("HEAD"),cancellable=true)
 private void face(BlockPos pos,net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action action,net.minecraft.core.Direction direction,int maxY,int sequence,CallbackInfo ci){
  if(KingdomsMod.service!=null){if(KingdomsMod.service.artifacts.preventsMining(player)){var chunk=level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);if(chunk!=null)player.connection.send(new net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket(pos,chunk.getBlockState(pos)));ci.cancel();return;}KingdomsMod.service.artifacts.minedFace(player,pos,direction);}
 }
 @WrapMethod(method="destroyBlock")
 private boolean destroy(BlockPos pos,Operation<Boolean> original){
  if(!Protection.permitted(level,pos,player.getUUID(),Permission.BREAK))return false;
  try(var scope=Protection.scope(level,pos,player.getUUID(),player,Protection.Cause.PLAYER)){return KingdomsMod.service==null?original.call(pos):KingdomsMod.service.artifacts.mine(player,pos,()->KingdomsMod.service.cores.breakCore(player,pos,()->original.call(pos)));}
 }
 @WrapMethod(method="useItemOn")
 private InteractionResult useOn(ServerPlayer p,Level l,ItemStack stack,InteractionHand hand,BlockHitResult hit,Operation<InteractionResult> original){
  if(KingdomsMod.service!=null){InteractionResult result=KingdomsMod.service.artifacts.use(p,hand);if(result!=InteractionResult.PASS)return result;}
  try(var scope=Protection.scope(l,hit.getBlockPos(),p.getUUID(),p,Protection.Cause.PLAYER)){return original.call(p,l,stack,hand,hit);}
 }
 @WrapMethod(method="useItem")
 private InteractionResult use(ServerPlayer p,Level l,ItemStack stack,InteractionHand hand,Operation<InteractionResult> original){
  if(KingdomsMod.service!=null){InteractionResult result=KingdomsMod.service.artifacts.use(p,hand);if(result!=InteractionResult.PASS)return result;}
  try(var scope=Protection.scope(l,p.blockPosition(),p.getUUID(),p,Protection.Cause.PLAYER)){return original.call(p,l,stack,hand);}
 }
}

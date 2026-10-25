package com.celik.kingdoms.mixin;
import com.celik.kingdoms.KingdomsMod;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class CrownCreativeMixin {
    @Shadow public ServerPlayer player;
    @Inject(method="handleSetCreativeModeSlot",at=@At("HEAD"),cancellable=true)
    private void bound(ServerboundSetCreativeModeSlotPacket packet,CallbackInfo ci){net.minecraft.network.protocol.PacketUtils.ensureRunningOnSameThread(packet,player.connection,player.level());if(packet.slotNum()==5&&KingdomsMod.service!=null&&KingdomsMod.service.artifacts.crown.locked(player)){ci.cancel();player.inventoryMenu.broadcastFullState();}}
}

package com.celik.kingdoms.mixin;
import com.celik.kingdoms.AirDropReceipts;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.*;
@Mixin(ServerPlayer.class)
public abstract class AirDropReceiptsMixin implements AirDropReceipts {
    @Unique private final Set<String> kingdoms$receipts=new HashSet<>();
    public Set<String> kingdoms$airDropReceipts(){return kingdoms$receipts;}
    @Inject(method="addAdditionalSaveData",at=@At("TAIL"))
    private void save(ValueOutput output,CallbackInfo ci){output.putString("kingdoms_airdrop_receipts",String.join(",",kingdoms$receipts));}
    @Inject(method="readAdditionalSaveData",at=@At("TAIL"))
    private void load(ValueInput input,CallbackInfo ci){kingdoms$receipts.clear();for(String receipt:input.getStringOr("kingdoms_airdrop_receipts","").split(","))if(!receipt.isEmpty())kingdoms$receipts.add(receipt);}
}

package com.celik.kingdoms.mixin;
import com.celik.kingdoms.AirDropChestMarker;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.UUID;

@Mixin(ChestBlockEntity.class)
public abstract class AirDropChestMixin implements AirDropChestMarker {
    @Unique private UUID kingdoms$drop;
    public UUID kingdoms$airDropId(){return kingdoms$drop;}
    public void kingdoms$airDropId(UUID id){kingdoms$drop=id;((ChestBlockEntity)(Object)this).setChanged();}
    @Inject(method="saveAdditional",at=@At("TAIL"))
    private void save(ValueOutput output,CallbackInfo ci){if(kingdoms$drop!=null)output.putString("kingdoms_airdrop",kingdoms$drop.toString());}
    @Inject(method="loadAdditional",at=@At("TAIL"))
    private void load(ValueInput input,CallbackInfo ci){String id=input.getStringOr("kingdoms_airdrop","");kingdoms$drop=id.isEmpty()?null:UUID.fromString(id);}
}

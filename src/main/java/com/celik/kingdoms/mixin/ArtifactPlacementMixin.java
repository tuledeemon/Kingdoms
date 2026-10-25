package com.celik.kingdoms.mixin;
import com.celik.kingdoms.KingdomsMod;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(BlockItem.class)
public abstract class ArtifactPlacementMixin {
    @Inject(method="place",at=@At("RETURN"))
    private void placed(BlockPlaceContext context,CallbackInfoReturnable<InteractionResult> cir){if(KingdomsMod.service!=null&&context.getPlayer()!=null&&cir.getReturnValue().consumesAction())KingdomsMod.service.artifacts.placedBlock(context.getLevel(),context.getClickedPos());}
}

package com.celik.kingdoms.mixin;

import com.celik.kingdoms.Protection;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.phys.Vec3;
import java.util.UUID;
import org.spongepowered.asm.mixin.*;

/** Preserve attribution for entity damage, secondary changes and explosion-created fire. */
@Mixin(ServerExplosion.class)
public abstract class ExplosionScopeMixin {
    @Shadow @Final private ServerLevel level;
    @Shadow @Final private Entity source;
    @Shadow @Final private Vec3 center;

    @WrapMethod(method="explode")
    private int kingdoms$explosionScope(Operation<Integer> original){
        var existing=Protection.current();UUID actor=Protection.owner(source);
        if(actor==null&&existing!=null&&existing.level()==level)actor=existing.actor();
        try(var scope=Protection.scope(level,BlockPos.containing(center),actor,source,Protection.Cause.EXPLOSION)){
            return original.call();
        }
    }
}

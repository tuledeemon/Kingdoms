package com.celik.kingdoms;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.function.Supplier;

public final class InstantFlint {
    private record Context(ServerPlayer player,ArtifactRules.Record record){}
    private static final ThreadLocal<Context> current=new ThreadLocal<>();
    public static boolean blocked(ServerPlayer player,ItemStack stack){
        var service=KingdomsMod.service;if(service==null)return false;
        var record=service.artifacts.record(stack);
        if(record==null||!record.type.equals("instant_flint"))return false;
        if(record.readyAt>service.now()){Ui.tell(player,"Artifact is cooling down",true);return true;}
        return service.artifacts.preventsMining(player)||service.artifacts.usable(stack)==null;
    }
    public static InteractionResult use(ServerPlayer player,ItemStack stack,Supplier<InteractionResult> original){
        var service=KingdomsMod.service;if(service==null)return original.get();
        var artifacts=service.artifacts;var record=artifacts.record(stack);
        if(record==null||!record.type.equals("instant_flint")||!stack.is(Items.FLINT_AND_STEEL))return original.get();
        if(artifacts.preventsMining(player))return InteractionResult.FAIL;
        if(record.readyAt>service.now()){Ui.tell(player,"Artifact is cooling down",true);return InteractionResult.FAIL;}
        if(artifacts.usable(stack)==null)return InteractionResult.FAIL;
        Context previous=current.get();current.set(new Context(player,record));
        try{return original.get();}finally{if(previous==null)current.remove();else current.set(previous);}
    }
    public static boolean prime(Entity entity,Supplier<Boolean> original){
        Context context=current.get();var service=KingdomsMod.service;
        if(context==null||service==null||!(entity instanceof PrimedTnt tnt)||tnt.getOwner()!=context.player)return original.get();
        // Persist cooldown BEFORE creating an explosive. Save failure cannot leave both
        // a live explosive and the original TNT block available for another ignition.
        service.artifacts.armInstantFlint(context.record);
        var config=service.config.artifacts;
        ((ArtifactTnt)tnt).kingdoms$policy(config.instantTntDamageMultiplier,config.instantTntMaxDamage);
        tnt.setFuse(0);
        return original.get();
    }
    private InstantFlint(){}
}

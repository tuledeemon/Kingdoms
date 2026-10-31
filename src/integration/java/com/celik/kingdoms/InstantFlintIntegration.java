package com.celik.kingdoms;

import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import java.util.*;
import java.util.function.BiConsumer;

final class InstantFlintIntegration {
    static void run(KingdomService s,ServerPlayer p,ArtifactConfig cfg,BiConsumer<Boolean,String> check){
        var level=p.level();BlockPos pos=new BlockPos(9240,210,7120);level.getChunk(pos);
        var mode=p.gameMode.getGameModeForPlayer();var location=p.position();ItemStack held=p.getMainHandItem();
        double multiplier=cfg.instantTntDamageMultiplier,maximum=cfg.instantTntMaxDamage;int cooldown=cfg.instantFlintCooldownSeconds;
        var r=ArtifactIntegration.issue(s.artifacts,cfg,"instant_flint");var flint=s.artifacts.create(r);
        try{
            p.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);p.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+2);p.setItemInHand(InteractionHand.MAIN_HAND,flint);
            cfg.instantTntDamageMultiplier=2;cfg.instantTntMaxDamage=3;cfg.instantFlintCooldownSeconds=10;
            level.setBlockAndUpdate(pos,Blocks.TNT.defaultBlockState());var before=tnt(level,pos);
            light(p,pos);var fresh=tnt(level,pos).stream().filter(t->!before.contains(t)).toList();
            check.accept(fresh.size()==1&&fresh.getFirst().getFuse()==0&&level.getBlockState(pos).isAir(),"native artifact ignition consumes one TNT and creates exactly one zero-fuse entity");
            check.accept(flint.getDamageValue()==1&&r.readyAt>s.now(),"native flint durability and saved cooldown apply once");
            var stored=s.store.read("artifacts.json",ArtifactRules.Data.class,new ArtifactRules.Data());check.accept(stored.records.get(r.id).readyAt==r.readyAt,"flint cooldown survives artifact-state reload");
            PrimedTnt t=fresh.getFirst();check.accept(((ArtifactTnt)t).kingdoms$damage(50)==3&&((ArtifactTnt)t).kingdoms$damage(1)==2,"artifact TNT multiplies then caps incoming damage");
            var output=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,level.registryAccess());t.saveWithoutId(output);var restored=new PrimedTnt(EntityTypes.TNT,level);restored.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,level.registryAccess(),output.buildResult()));
            check.accept(restored.getFuse()==0&&((ArtifactTnt)restored).kingdoms$damage(50)==3,"TNT save/load preserves zero fuse and damage policy");
            Pig pig=new Pig(EntityTypes.PIG,level);pig.setPos(pos.getX()+1.5,pos.getY(),pos.getZ()+.5);level.addFreshEntity(pig);float hp=pig.getHealth();t.tick();check.accept(t.isRemoved()&&pig.getHealth()<hp&&pig.getHealth()>=hp-3.001,"zero-fuse TNT detonates on first tick and native explosion damage honors cap");pig.discard();
            level.setBlockAndUpdate(pos,Blocks.TNT.defaultBlockState());int count=tnt(level,pos).size(),damage=flint.getDamageValue();light(p,pos);
            check.accept(level.getBlockState(pos).is(Blocks.TNT)&&tnt(level,pos).size()==count&&flint.getDamageValue()==damage,"cooldown blocks repeated ignition without TNT duplication or tool wear: block="+level.getBlockState(pos)+" entities="+tnt(level,pos).size()+" before="+count+" damage="+flint.getDamageValue()+" beforeDamage="+damage+" remaining="+(r.readyAt-s.now()));
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.FLINT_AND_STEEL));light(p,pos);var normal=tnt(level,pos).stream().filter(x->!before.contains(x)).toList();check.accept(normal.size()==1&&normal.getFirst().getFuse()>0&&((ArtifactTnt)normal.getFirst()).kingdoms$damage(50)==50,"ordinary flint/TNT keeps normal fuse and damage");normal.forEach(PrimedTnt::discard);
            p.setItemInHand(InteractionHand.MAIN_HAND,flint);r.readyAt=0;level.setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());light(p,pos);check.accept(r.readyAt==0,"lighting ordinary fire does not spend TNT ability cooldown");
        }finally{cfg.instantTntDamageMultiplier=multiplier;cfg.instantTntMaxDamage=maximum;cfg.instantFlintCooldownSeconds=cooldown;tnt(level,pos).forEach(PrimedTnt::discard);p.setItemInHand(InteractionHand.MAIN_HAND,held);p.setPos(location);p.gameMode.changeGameModeForPlayer(mode);}
    }
    private static List<PrimedTnt> tnt(ServerLevel level,BlockPos pos){return level.getEntitiesOfClass(PrimedTnt.class,new AABB(pos).inflate(4));}
    private static void light(ServerPlayer p,BlockPos pos){p.gameMode.useItemOn(p,p.level(),p.getMainHandItem(),InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));}
    private InstantFlintIntegration(){}
}

package com.celik.kingdoms;
import java.util.function.BiConsumer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import static com.celik.kingdoms.Rules.*;

final class GlowingBeaconIntegration {
    static void run(KingdomService s,ServerPlayer mayor,ServerPlayer enemy,BiConsumer<Boolean,String> check){
        State original=s.store.copy(s.state);Long price=s.config.beaconEffectFees.get("glowing");var position=enemy.position();
        try{
            s.config.beaconEffectFees.put("glowing",1L);long treasury=s.own(mayor.getUUID()).treasury;
            s.buyBeaconEffect(mayor,BeaconPower.GLOWING,1);
            check.accept(s.own(mayor.getUUID()).treasury==treasury-1,"glowing hostile beacon purchases debit the configured treasury price");
            enemy.setPos(36,201,8);s.applyBeaconEffects(enemy);
            check.accept(enemy.hasEffect(MobEffects.GLOWING)&&enemy.getEffect(MobEffects.GLOWING).getAmplifier()==0,"glowing beacon applies the actual vanilla Level I Glowing effect to enemy members");
            check.accept(enemy.hasEffect(MobEffects.MINING_FATIGUE),"glowing can coexist with the existing hostile mining-fatigue power");
            enemy.setPos(36,-62,8);s.applyBeaconEffects(enemy);check.accept(enemy.hasEffect(MobEffects.GLOWING),"enemy glowing also applies underground within claimed chunks");
            enemy.setPos(20,-62,8);s.applyBeaconEffects(enemy);check.accept(!enemy.hasEffect(MobEffects.GLOWING),"hostile glowing is removed in wilderness");
            enemy.setPos(36,-62,8);s.applyBeaconEffects(enemy);check.accept(enemy.hasEffect(MobEffects.GLOWING),"hostile glowing returns upon reentering claimed land");
            State saved=s.store.read("kingdoms.json",State.class,new State());check.accept(saved.beaconEffects.stream().anyMatch(e->e.power==BeaconPower.GLOWING&&e.hostile&&e.targetKingdom.equals(s.own(enemy.getUUID()).id)),"glowing enemy target and expiry persist in kingdom data");
            State next=s.store.copy(s.state);War war=next.wars.getFirst();war.startedAt=s.now()+60000;s.commit(next);boolean rejected=false;
            try{s.buyBeaconEffect(mayor,BeaconPower.GLOWING,1);}catch(IllegalArgumentException expected){rejected=expected.getMessage().contains("active wartime");}check.accept(rejected,"glowing remains unavailable for purchase during war preparation");
            next=s.store.copy(s.state);war=next.wars.getFirst();war.startedAt=s.now()-900001;war.declaredAt=war.startedAt-10000;war.endsAt=s.now()-1;s.commit(next);s.applyBeaconEffects(enemy);check.accept(enemy.hasEffect(MobEffects.GLOWING),"purchased glowing continues after war until its saved expiry like other hostile powers");
        }finally{if(price==null)s.config.beaconEffectFees.remove("glowing");else s.config.beaconEffectFees.put("glowing",price);s.commit(original);enemy.setPos(position);s.applyBeaconEffects(enemy);}
    }
    private GlowingBeaconIntegration(){}
}

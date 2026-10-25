package com.celik.kingdoms;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import java.util.*;
import static com.celik.kingdoms.Rules.*;

public final class Protection {
    public enum Cause { PLAYER, BLOCK, FLUID, FIRE, EXPLOSION, ENTITY, PISTON, AUTOMATION }
    public record Context(Level level, BlockPos origin, UUID actor, Entity entity, Cause cause) {}
    private static final ThreadLocal<Context> CURRENT = new ThreadLocal<>();

    public static Context current() {return CURRENT.get();}
    public static Scope scope(Level level,BlockPos pos,UUID actor,Entity entity,Cause cause) {return new Scope(new Context(level,pos.immutable(),actor,entity,cause));}
    public static final class Scope implements AutoCloseable {private final Context previous;Scope(Context next){previous=CURRENT.get();CURRENT.set(next);}public void close(){if(previous==null)CURRENT.remove();else CURRENT.set(previous);}}
    public static KingdomService service(Level l) {return l instanceof ServerLevel?KingdomsMod.service:null;}
    public static UUID owner(Entity entity) {
        if(entity==null)return null;
        if(entity instanceof Player)return entity.getUUID();
        if(entity instanceof Projectile p && p.getOwner()!=null && p.getOwner()!=entity)return owner(p.getOwner());
        if(entity instanceof PrimedTnt t && t.getOwner()!=null)return owner(t.getOwner());
        if(entity instanceof OwnableEntity o && o.getOwner()!=null && o.getOwner()!=entity)return owner(o.getOwner());
        return Provenance.entityOwner(entity.getUUID());
    }
    public static boolean bypass(Level level,UUID actor) {KingdomService s=service(level);return s!=null&&actor!=null&&s.operatorBypass(actor);}
    public static boolean permitted(Level level,BlockPos pos,UUID p,Permission perm) {KingdomService s=service(level);if(s!=null&&s.airDrops!=null){if(s.airDrops.chests.protects(level,pos)&&(perm==Permission.PLACE||perm==Permission.BREAK||perm==Permission.CONTAINERS||perm==Permission.INTERACT))return false;if(s.airDrops.protectedAt(level,pos)&&(perm==Permission.PLACE||perm==Permission.BREAK))return false;}return s==null||bypass(level,p)||s.permission(s.land(level,pos),p,perm);}
    private static boolean eventTerrainFlag(Flag flag){return flag==Flag.FIRE||flag==Flag.EXPLOSIONS||flag==Flag.MOB_GRIEFING;}
    public static boolean flag(Level level,BlockPos pos,Flag flag) {KingdomService s=service(level);if(s!=null&&s.airDrops!=null&&eventTerrainFlag(flag)&&(s.airDrops.protectedAt(level,pos)||s.airDrops.chests.protects(level,pos)))return false;return s==null||s.flag(s.land(level,pos),flag);}
    public static boolean sameLand(Level level,BlockPos from,BlockPos to) {
        KingdomService s=service(level);if(s==null)return true;Kingdom a=s.land(level,from),b=s.land(level,to);return a==b;
    }
    public static boolean container(Level level,BlockPos pos,UUID actor) {
        if(!permitted(level,pos,actor,Permission.CONTAINERS))return false;
        if(level.getBlockState(pos).getBlock() instanceof net.minecraft.world.level.block.ChestBlock)
            for(var direction:net.minecraft.core.Direction.Plane.HORIZONTAL) {BlockPos other=pos.relative(direction);if(level.getBlockState(other).getBlock() instanceof net.minecraft.world.level.block.ChestBlock&&!permitted(level,other,actor,Permission.CONTAINERS))return false;}
        return true;
    }
    public static boolean automation(Level level,BlockPos from,BlockPos to) {
        if(!machine(level,from,to))return false;
        if(level.getBlockState(to).getBlock() instanceof net.minecraft.world.level.block.ChestBlock)
            for(var direction:net.minecraft.core.Direction.Plane.HORIZONTAL) {BlockPos other=to.relative(direction);if(level.getBlockState(other).getBlock() instanceof net.minecraft.world.level.block.ChestBlock&&!machine(level,from,other))return false;}
        return true;
    }
    /** Machines work normally within one owner's land, without a mayor toggle. */
    public static boolean machine(Level level,BlockPos from,BlockPos to){
        return machine(level,from,to,true);
    }
    private static boolean machine(Level level,BlockPos from,BlockPos to,boolean allowWilderness){
        KingdomService s=service(level);if(s==null)return true;
        if(s.airDrops!=null&&(s.airDrops.protectedAt(level,from)||s.airDrops.protectedAt(level,to)||s.airDrops.chests.protects(level,from)||s.airDrops.chests.protects(level,to)))return false;
        Kingdom a=s.land(level,from),b=s.land(level,to);
        if(a==b)return true;
        if(a!=null&&b!=null){War war=Rules.war(s.state,a.id,s.now());return war!=null&&war.opposes(a.id,b.id);}
        return b==null&&(allowWilderness||a==null);
    }
    public static boolean piston(Level level,BlockPos from,BlockPos to){
        return piston(level,from,to,false);
    }
    public static boolean piston(Level level,BlockPos from,BlockPos to,boolean allowWilderness){
        KingdomService s=service(level);
        return machine(level,from,to,allowWilderness)&&(s==null||s.cores.at(level,from)==null&&s.cores.at(level,to)==null);
    }
    /** Towny-style flow boundaries; unlike a player action, flow never borrows its placer's grants. */
    public static boolean fluid(Level level,BlockPos from,BlockPos to){
        KingdomService s=service(level);if(s==null)return true;
        if(s.cores.at(level,to)!=null)return false;
        if(s.airDrops!=null&&(s.airDrops.protectedAt(level,from)||s.airDrops.protectedAt(level,to)||s.airDrops.chests.protects(level,from)||s.airDrops.chests.protects(level,to)))return false;
        return !s.config.preventFluidGriefing||machine(level,from,to,true);
    }
    public static boolean environmental(Level level,BlockPos from,BlockPos to,Flag flag) {
        KingdomService s=service(level);if(s==null)return true;Kingdom a=s.land(level,from),b=s.land(level,to);
        if(s.airDrops!=null&&eventTerrainFlag(flag)&&(s.airDrops.protectedAt(level,from)||s.airDrops.protectedAt(level,to)||s.airDrops.chests.protects(level,from)||s.airDrops.chests.protects(level,to)))return false;
        if(flag==Flag.FIRE&&s.cores.at(level,to)!=null)return false;
        Context ctx=current();
        if(b!=null&&!s.flag(b,flag))return false;
        if(b!=null&&ctx!=null&&ctx.actor!=null)return permitted(level,to,ctx.actor,Permission.PLACE);
        if(a==b)return a==null||s.flag(a,flag);
        if(a!=null&&b!=null){War w=Rules.war(s.state,a.id,s.now());return w!=null&&w.opposes(a.id,b.id);}
        if(a!=null&&b==null&&Rules.war(s.state,a.id,s.now())!=null)return true;
        return b==null;
    }
    public static boolean mutation(Level level,BlockPos pos,BlockState next) {
        KingdomService s=service(level);if(s==null)return true;Kingdom k=s.land(level,pos);Context c=current();BlockState old=level.getBlockState(pos);
        if(old.equals(next))return true;
        if(PistonMoves.approved(level,pos))return true;
        if(s.airDrops!=null){if(s.airDrops.chests.internal(level,pos,next))return true;if(s.airDrops.protectedAt(level,pos)||s.airDrops.chests.protects(level,pos))return false;}
        if(!s.cores.allowsMutation(level,pos,next))return false;
        if(!WarRestrictions.placement(level,pos,old,next)||WarRestrictions.preventFormation(level,pos,old,next))return false;
        if(k!=null) {
            if(next.is(BlockTags.FIRE)&&!s.flag(k,Flag.FIRE))return false;
        }
        if(c==null) {
            // Unattributed destructive changes fail closed. Block ticks, players and entities have scopes below.
            return k==null || old.isAir()&&next.isAir() || old.getBlock()==next.getBlock();
        }
        if(c.cause==Cause.EXPLOSION)return explosion(level,c.origin,pos,c.actor);
        if(c.cause==Cause.PLAYER) {
            if(k==null)return true;
            Permission permission=next.isAir()?Permission.BREAK:old.getBlock()==next.getBlock()?Permission.INTERACT:Permission.PLACE;
            return permitted(level,pos,c.actor,permission);
        }
        if(c.cause==Cause.FLUID)return fluid(level,c.origin,pos);
        if(c.cause==Cause.FIRE)return environmental(level,c.origin,pos,Flag.FIRE);
        if(c.cause==Cause.PISTON)return piston(level,c.origin,pos);
        if(c.cause==Cause.AUTOMATION)return !old.getFluidState().isEmpty()||!next.getFluidState().isEmpty()?fluid(level,c.origin,pos):machine(level,c.origin,pos);
        if(c.cause==Cause.ENTITY) {
            if(k==null)return true;
            if(c.actor!=null)return permitted(level,pos,c.actor,next.isAir()?Permission.BREAK:Permission.PLACE);
            return sameLand(level,c.origin,pos)&&s.flag(k,Flag.MOB_GRIEFING);
        }
        return sameLand(level,c.origin,pos); // Natural growth never crosses a kingdom boundary.
    }
    public static boolean explosion(Level level,BlockPos origin,BlockPos target,UUID actor) {
        if(WarRestrictions.preventBlast(level,origin,target,true))return false;
        KingdomService s=service(level);
        if(s!=null){
            if(s.cores.at(level,target)!=null)return false;
            if(s.airDrops!=null&&(s.airDrops.protectedAt(level,target)||s.airDrops.chests.protects(level,target)))return false;
        }
        return explosionDamage(level,origin,target,actor);
    }
    /** Terrain protection must not make combatants immune to explosions. */
    public static boolean explosionDamage(Level level,BlockPos origin,BlockPos target,UUID actor) {
        KingdomService s=service(level);if(s==null)return true;Kingdom k=s.land(level,target);if(k==null)return true;
        if(!s.flag(k,Flag.EXPLOSIONS))return false;
        if(actor!=null)return permitted(level,target,actor,Permission.BREAK);
        Context c=current();if(c!=null&&c.actor!=null)return permitted(level,target,c.actor,Permission.BREAK);
        return sameLand(level,origin,target); // Unowned cannon blasts cannot cross into a claim.
    }
    public static boolean damage(Entity target,DamageSource source) {
        if(target instanceof AirDropVisuals.Crate||target instanceof AirDropVisuals.HitBox)return false;
        KingdomService s=service(target.level());if(s==null)return true;Kingdom k=s.land(target.level(),target.blockPosition());
        UUID actor=owner(source.getEntity()); if(actor==null)actor=owner(source.getDirectEntity());
        Context artifact=current();if(actor==null&&source.is(net.minecraft.world.damagesource.DamageTypes.LIGHTNING_BOLT)&&artifact!=null)actor=artifact.actor;
        if(actor!=null&&target instanceof Player&&!s.config.alliedFriendlyFire){Kingdom a=s.own(actor),b=s.own(target.getUUID());if(a!=null&&b!=null){War war=Rules.war(s.state,a.id,s.now());if(Rules.allied(s.state,a.id,b.id)||war!=null&&!a.id.equals(b.id)&&war.sameSide(a.id,b.id))return false;}}
        if(source.is(DamageTypeTags.IS_EXPLOSION)) {
            Entity direct=source.getDirectEntity();Context c=current();BlockPos origin=direct!=null?direct.blockPosition():c!=null?c.origin:target.blockPosition();
            if(WarRestrictions.preventBlast(target.level(),origin,target.blockPosition(),false))return false;
            if(!explosionDamage(target.level(),origin,target.blockPosition(),actor))return false;
        }
        if(k!=null&&source.is(DamageTypeTags.IS_FIRE)&&!s.flag(k,Flag.FIRE))return false;
        if(actor!=null&&!actor.equals(target.getUUID())) {
            Permission perm=target instanceof Player?Permission.PVP:Permission.ENTITIES;
            if(k!=null&&(!permitted(target.level(),target.blockPosition(),actor,perm)||(perm==Permission.PVP&&!s.flag(k,Flag.PVP))))return false;
            Entity attacker=source.getEntity();
            if(target instanceof Player&&attacker!=null) {Kingdom from=s.land(attacker.level(),attacker.blockPosition());if(from!=null&&(!s.flag(from,Flag.PVP)||!permitted(attacker.level(),attacker.blockPosition(),actor,Permission.PVP)))return false;}
        }
        if(k!=null&&actor==null&&source.getEntity() instanceof Mob&&!s.flag(k,Flag.MOB_GRIEFING))return false;
        if(k!=null&&source.is(net.minecraft.world.damagesource.DamageTypes.CRAMMING)&&!s.flag(k,Flag.MOB_GRIEFING))return false;
        return true;
    }
    public static boolean allowedMob(Mob mob,Kingdom k) {
        KingdomService s=service(mob.level());
        return k==null||s==null||s.flag(k,Flag.MOBS)||k.allowedMobs.contains(mob.getUUID())||mob instanceof Animal||mob instanceof OwnableEntity o&&o.getOwner()!=null||s.config.allowedMobTypes.contains(BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString());
    }
    public static boolean admitSpawn(Entity entity) {
        if(!WarRestrictions.entityAllowed(entity))return false;
        KingdomService s=service(entity.level());if(s==null)return true;Kingdom k=s.land(entity.level(),entity.blockPosition());Context context=current();
        if(s.airDrops!=null&&s.airDrops.protectedAt(entity.level(),entity.blockPosition())){
            if(entity instanceof PrimedTnt||entity instanceof net.minecraft.world.entity.item.FallingBlockEntity)return false;
            if(context!=null&&context.actor!=null&&(entity instanceof Mob||entity instanceof net.minecraft.world.entity.vehicle.VehicleEntity||entity instanceof net.minecraft.world.entity.decoration.ArmorStand||entity instanceof net.minecraft.world.entity.decoration.HangingEntity))return false;
        }
        if((entity instanceof PrimedTnt||entity instanceof net.minecraft.world.entity.item.FallingBlockEntity)&&s.cores.at(entity.level(),entity.blockPosition())!=null)return false;
        if(k!=null&&context!=null&&context.actor!=null&&!permitted(entity.level(),entity.blockPosition(),context.actor,Permission.PLACE))return false;
        if(context!=null&&context.actor!=null&&(entity instanceof Mob||entity instanceof Projectile||entity instanceof PrimedTnt||entity instanceof net.minecraft.world.entity.item.FallingBlockEntity||entity instanceof net.minecraft.world.entity.vehicle.VehicleEntity))Provenance.entity(entity.getUUID(),context.actor);
        if(!(entity instanceof Mob mob))return true;
        return allowedMob(mob,k);
    }
    public static void inspectMob(Entity entity) {
        WarRestrictions.inspect(entity);if(entity.isRemoved())return;
        if(!(entity instanceof Mob mob))return;KingdomService s=service(mob.level());if(s==null)return;Kingdom k=s.land(mob.level(),mob.blockPosition());
        if(allowedMob(mob,k))return;
        
        mob.discard();
    }

    public static void reset(){CURRENT.remove();WarRestrictions.reset();}
    private Protection(){}
}

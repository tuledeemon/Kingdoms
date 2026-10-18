package com.celik.kingdoms;

import java.util.*;
import java.util.function.BiConsumer;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.material.*;
import net.minecraft.world.phys.*;
import static com.celik.kingdoms.Rules.*;

final class WarRestrictionsIntegration {
    static void run(KingdomService s,ServerLevel level,ServerPlayer mayor,ServerPlayer enemy,BiConsumer<Boolean,String> check)throws Exception{
        Config originalConfig=s.config;WarConfig previous=s.config.warRestrictions;ItemStack previousTool=enemy.getMainHandItem();BlockPos pos=new BlockPos(4,214,4),wild=new BlockPos(24,214,8);
        try{
            WarConfig config=new WarConfig();s.config.warRestrictions=config;
            check.accept(WarAdmin.canonical("block","obsidian",true).equals("minecraft:obsidian"),"war admin accepts namespaced and short block IDs");
            boolean rejected=false;try{WarAdmin.canonical("block","bedrock",true);}catch(IllegalArgumentException expected){rejected=true;}check.accept(rejected,"war instant-mining command rejects unbreakable blocks");
            rejected=false;try{WarAdmin.canonical("entity","player",true);}catch(IllegalArgumentException expected){rejected=true;}check.accept(rejected,"war entity command cannot ban players");
            config.instantBreakBlocks.add("minecraft:obsidian");place(level,pos,Blocks.OBSIDIAN.defaultBlockState(),enemy);
            enemy.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND_PICKAXE));
            check.accept(level.getBlockState(pos).getDestroyProgress(enemy,level,pos)==1,"listed obsidian has immediate server-side mining progress during active war");
            War war=Rules.war(s.state,s.own(enemy.getUUID()).id,s.now());long originalStart=war.startedAt;war.startedAt=s.now()+10000;
            check.accept(!WarRestrictions.restricted(level,pos,enemy.getUUID())&&level.getBlockState(pos).getDestroyProgress(enemy,level,pos)<1,"preparation does not activate wartime restrictions or instant mining");war.startedAt=originalStart;
            place(level,wild,Blocks.OBSIDIAN.defaultBlockState(),enemy);check.accept(level.getBlockState(wild).getDestroyProgress(enemy,level,wild)<1,"instant-war mining does not affect wilderness by default");
            var outsider=new ServerPlayer(s.server,level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"WarOutsider"),ClientInformation.createDefault());
            check.accept(level.getBlockState(pos).getDestroyProgress(outsider,level,pos)<1,"instant blocks do not let outsiders bypass claims");
            float originalSpeed=Blocks.BEDROCK.defaultBlockState().getDestroyProgress(enemy,level,pos);config.instantBreakBlocks.add("minecraft:bedrock");check.accept(Blocks.BEDROCK.defaultBlockState().getDestroyProgress(enemy,level,pos)==originalSpeed,"even manually configured instant lists preserve bedrock mining rules");config.instantBreakBlocks.remove("minecraft:bedrock");
            check.accept(enemy.gameMode.destroyBlock(pos)&&level.getBlockState(pos).isAir(),"instant mining uses the normal protected vanilla block-break operation");
            config.blockedPlacementBlocks.add("minecraft:obsidian");try(var context=Protection.scope(level,pos,enemy.getUUID(),enemy,Protection.Cause.PLAYER)){check.accept(!level.setBlockAndUpdate(pos,Blocks.OBSIDIAN.defaultBlockState()),"placement bans are independent of instant-mining lists");}config.blockedPlacementBlocks.clear();
            config.blockedEntities.add("minecraft:tnt");var banned=new PrimedTnt(level,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,enemy);check.accept(!level.addFreshEntity(banned),"banned TNT entities cannot spawn in active-war claims");
            var existing=new PrimedTnt(level,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,enemy);WarRestrictions.inspect(existing);check.accept(existing.isRemoved(),"existing banned entities are removed on inspection");
            var allowedOutside=new PrimedTnt(level,wild.getX()+.5,wild.getY()+.5,wild.getZ()+.5,enemy);check.accept(WarRestrictions.entityAllowed(allowedOutside),"entity bans leave wilderness unchanged by default");config.blockedEntities.clear();
            config.blockPearlTeleport=true;check.accept(WarRestrictions.pearlBlocked(enemy,level,pos),"pearl entry into wartime claims is blocked");
            Vec3 oldPosition=enemy.position();enemy.setPos(pos.getX(),pos.getY(),pos.getZ());check.accept(WarRestrictions.pearlBlocked(enemy,level,wild),"pearl exits from wartime claims are blocked");enemy.setPos(oldPosition);
            var pearl=new ThrownEnderpearl(level,enemy,new ItemStack(Items.ENDER_PEARL));pearl.setPos(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5);Vec3 beforeTeleport=enemy.position();
            var onHit=ThrownEnderpearl.class.getDeclaredMethod("onHit",HitResult.class);onHit.setAccessible(true);onHit.invoke(pearl,new BlockHitResult(pearl.position(),Direction.UP,pos,false));check.accept(pearl.isRemoved()&&enemy.position().equals(beforeTeleport),"real pearl collision is cancelled before teleport and impact effects");config.blockPearlTeleport=false;
            place(level,pos,Blocks.STONE.defaultBlockState(),enemy);var source=new PrimedTnt(level,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,enemy);config.blockedExplosions.add("minecraft:tnt");
            level.explode(source,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,3,Level.ExplosionInteraction.TNT);check.accept(level.getBlockState(pos).is(Blocks.STONE),"TNT detonation ban cancels the actual explosion before block damage");config.blockedExplosions.clear();config.blockedExplosionBlockDamage.add("minecraft:tnt");
            var victim=new Zombie(level);victim.setPos(pos.getX()+1.5,pos.getY()+.5,pos.getZ()+.5);level.addFreshEntity(victim);float health=victim.getHealth();
            level.explode(source,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,3,Level.ExplosionInteraction.TNT);check.accept(level.getBlockState(pos).is(Blocks.STONE),"TNT terrain ban preserves blocks through real explosion callbacks");check.accept(victim.getHealth()<health,"terrain-only explosion ban still permits entity damage");victim.discard();
            place(level,wild,Blocks.STONE.defaultBlockState(),enemy);var wildSource=new PrimedTnt(level,wild.getX()+.5,wild.getY()+.5,wild.getZ()+.5,enemy);level.explode(wildSource,wild.getX()+.5,wild.getY()+.5,wild.getZ()+.5,3,Level.ExplosionInteraction.TNT);check.accept(level.getBlockState(wild).isAir(),"real wilderness TNT still damages terrain with wartime-only restrictions configured");
            config.blockedExplosionBlockDamage.clear();config.blockedExplosions.add("minecraft:tnt");BlockPos boundary=new BlockPos(15,214,4);place(level,boundary,Blocks.STONE.defaultBlockState(),enemy);var borderSource=new PrimedTnt(level,17.5,214.5,4.5,enemy);level.explode(borderSource,17.5,214.5,4.5,3,Level.ExplosionInteraction.TNT);check.accept(level.getBlockState(boundary).is(Blocks.STONE),"detonations outside a claim cannot bypass a ban on affected war-claim blocks");config.blockedExplosions.clear();
            config.preventLavaCasting=true;BlockPos casting=new BlockPos(3,250,12);place(level,casting.east(),Blocks.WATER.defaultBlockState(),enemy);place(level,casting,Blocks.LAVA.defaultBlockState(),enemy);check.accept(level.getBlockState(casting).is(Blocks.LAVA),"lava-water contact cannot create obsidian while casting is banned");
            config.preventLavaCasting=false;place(level,casting,Blocks.AIR.defaultBlockState(),enemy);place(level,casting,Blocks.LAVA.defaultBlockState(),enemy);check.accept(level.getBlockState(casting).is(Blocks.OBSIDIAN),"normal lava-water formation resumes when the ban is disabled");
            config.preventLavaCasting=true;BlockPos flowing=casting.above(4);place(level,flowing.east(),Blocks.WATER.defaultBlockState(),enemy);place(level,flowing,Blocks.LAVA.defaultBlockState().setValue(LiquidBlock.LEVEL,1),enemy);check.accept(level.getBlockState(flowing).is(Blocks.LAVA),"flowing lava-water contact cannot create cobblestone");
            BlockPos falling=casting.above(8);place(level,falling,Blocks.WATER.defaultBlockState(),enemy);var spread=LavaFluid.class.getDeclaredMethod("spreadTo",LevelAccessor.class,BlockPos.class,net.minecraft.world.level.block.state.BlockState.class,Direction.class,FluidState.class);spread.setAccessible(true);
            try(var context=Protection.scope(level,falling.above(),enemy.getUUID(),enemy,Protection.Cause.FLUID)){spread.invoke(Fluids.LAVA,level,falling,level.getBlockState(falling),Direction.DOWN,Fluids.FLOWING_LAVA.getFlowing(8,true));}
            check.accept(level.getBlockState(falling).is(Blocks.WATER),"downward lava into water cannot create stone");
            config.preventLavaCasting=false;try(var context=Protection.scope(level,falling.above(),enemy.getUUID(),enemy,Protection.Cause.FLUID)){spread.invoke(Fluids.LAVA,level,falling,level.getBlockState(falling),Direction.DOWN,Fluids.FLOWING_LAVA.getFlowing(8,true));}check.accept(level.getBlockState(falling).is(Blocks.STONE),"downward vanilla stone formation still works when permitted");
            config.scope="war";check.accept(WarRestrictions.restricted(level,wild,enemy.getUUID()),"war scope extends restrictions to active participants outside claims");config.scope="global";check.accept(WarRestrictions.restricted(level,wild,outsider.getUUID()),"global scope is an explicit administrator option");config.enabled=false;check.accept(!WarRestrictions.restricted(level,pos,enemy.getUUID()),"administrator can disable the whole restriction system immediately");
            var commands=s.server.getCommands().getDispatcher();var console=s.server.createCommandSourceStack();
            check.accept(commands.execute("kingdomadmin war block add minecraft:obsidian",console)==1&&s.config.warRestrictions.instantBreakBlocks.contains("minecraft:obsidian"),"real admin command accepts namespaced block identifiers");
            check.accept(commands.execute("kingdomadmin war placement add minecraft:reinforced_deepslate",console)==1&&s.config.warRestrictions.blockedPlacementBlocks.contains("minecraft:reinforced_deepslate"),"real admin command saves independent placement bans");
            check.accept(commands.execute("kingdomadmin war entity add minecraft:tnt_minecart",console)==1&&s.config.warRestrictions.blockedEntities.contains("minecraft:tnt_minecart"),"real admin command saves entity bans");
            check.accept(commands.execute("kingdomadmin war explosion add minecraft:bed",console)==1&&s.config.warRestrictions.blockedExplosions.contains("minecraft:bed"),"real admin command accepts block-origin explosion sources");
            check.accept(commands.execute("kingdomadmin war blockdamage add *",console)==1&&s.config.warRestrictions.blockedExplosionBlockDamage.contains("*"),"real admin command accepts explosion-source wildcard");
            check.accept(commands.execute("kingdomadmin war set blockPearlTeleport true",console)==1&&s.config.warRestrictions.blockPearlTeleport,"real admin command updates pearl restriction toggle");
            var saved=KingdomsMod.configStore.read("config.json",Config.class,new Config());check.accept(saved.warRestrictions.blockedEntities.contains("minecraft:tnt_minecart")&&saved.warRestrictions.blockPearlTeleport,"war commands persist lists and toggles to the real configuration file");
            String before=Store.JSON.toJson(s.config);check.accept(commands.execute("kingdomadmin war set scope invalid",console)==0&&Store.JSON.toJson(s.config).equals(before),"invalid war setting is rejected without changing running policy");
            check.accept(commands.execute("kingdomadmin war block remove minecraft:obsidian",console)==1&&!s.config.warRestrictions.instantBreakBlocks.contains("minecraft:obsidian"),"real admin removal accepts namespaced identifiers");
            check.accept(commands.execute("kingdomadmin war blockdamage clear",console)==1&&s.config.warRestrictions.blockedExplosionBlockDamage.isEmpty(),"real admin command clears only the selected list");
            check.accept(commands.execute("kingdomadmin war entity list 1",console)==1,"administrator list pagination is executable without producing unbounded chat output");
            rejected=false;try{commands.execute("kingdomadmin war set enabled true",enemy.createCommandSourceStack());}catch(com.mojang.brigadier.exceptions.CommandSyntaxException expected){rejected=true;}check.accept(rejected,"ordinary kingdom mayors cannot execute administrator war commands");
        }finally{s.config=originalConfig;s.config.warRestrictions=previous;KingdomsMod.configStore.write("config.json",originalConfig);enemy.setItemInHand(InteractionHand.MAIN_HAND,previousTool);}
    }
    private static void place(ServerLevel level,BlockPos pos,net.minecraft.world.level.block.state.BlockState block,ServerPlayer player){try(var context=Protection.scope(level,pos,player.getUUID(),player,Protection.Cause.PLAYER)){level.setBlockAndUpdate(pos,block);}}
    private WarRestrictionsIntegration(){}
}

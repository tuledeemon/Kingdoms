package com.celik.kingdoms;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.tags.BlockTags;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.Heightmap;
import java.util.*;
import java.util.function.BooleanSupplier;
import static com.celik.kingdoms.Rules.*;

/** Core tokens designate existing blocks; the saved nonce is the authority, never the item alone. */
public final class KingdomCores {
    private final KingdomService service;
    private record Selection(UUID owner,UUID holder,UUID nonce,long until) {}
    private final Map<UUID,Selection> selecting = new HashMap<>();
    private boolean changing;
    public final CoreHighlight highlights;
    public KingdomCores(KingdomService service){this.service=service;highlights=new CoreHighlight(service);}
    public static BlockPos position(Core core){return new BlockPos(core.x,core.y,core.z);}
    public Kingdom at(Level level,BlockPos pos){
        String dimension=KingdomService.dimension(level);
        return service.state.kingdoms.values().stream().filter(k->k.core!=null&&!k.core.carried&&dimension.equals(k.core.dimension)&&position(k.core).equals(pos)).findFirst().orElse(null);
    }
    private boolean reserved(Level level,BlockPos pos,UUID except){return service.state.kingdoms.values().stream().anyMatch(k->!k.id.equals(except)&&k.core!=null&&KingdomService.dimension(level).equals(k.core.dimension)&&position(k.core).equals(pos));}
    public boolean allowsMutation(Level level,BlockPos pos,net.minecraft.world.level.block.state.BlockState next){
        if(changing)return true;Kingdom owner=at(level,pos);
        if(owner==null&&next.getDestroySpeed(level,pos)<0&&reserved(level,pos,null))return false;
        return owner==null||level.getBlockState(pos).getBlock()==next.getBlock();
    }
    public Core initial(ServerLevel level,BlockPos chunkPosition){
        int x=(chunkPosition.getX()>>4)*16+8,z=(chunkPosition.getZ()>>4)*16+8;
        int y=level.getHeight(Heightmap.Types.WORLD_SURFACE,x,z)-1;
        while(y>=level.getMinY()&&(level.getBlockState(new BlockPos(x,y,z)).getBlock() instanceof LiquidBlock||level.getBlockState(new BlockPos(x,y,z)).is(BlockTags.FIRE)))y--;
        if(y<level.getMinY()||!suitable(level,new BlockPos(x,y,z)))throw new IllegalArgumentException("Place a breakable block at the center of this chunk before creating the kingdom.");
        Core core=new Core();setLocation(core,level,new BlockPos(x,Math.max(level.getMinY(),y),z));return core;
    }
    private void setLocation(Core core,ServerLevel level,BlockPos pos){
        service.require(suitable(level,pos),"Choose an existing breakable block. Air, fire, fluid and unbreakable blocks cannot be cores.");
        core.dimension=KingdomService.dimension(level);core.x=pos.getX();core.y=pos.getY();core.z=pos.getZ();
        core.block=BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString();
    }
    private boolean suitable(ServerLevel level,BlockPos pos){var block=level.getBlockState(pos);return !block.isAir()&&!block.is(BlockTags.FIRE)&&!(block.getBlock() instanceof LiquidBlock)&&block.getDestroySpeed(level,pos)>=0;}
    public void beginSelection(ServerPlayer player){
        beginSelection(player,service.requireOwn(player.getUUID()).id);
    }
    private Kingdom movableCore(ServerPlayer player,UUID ownerId){
        Kingdom holder=service.requireOwn(player.getUUID());service.mayor(holder,player.getUUID());service.peaceful(holder);
        Kingdom owner=service.state.kingdoms.get(ownerId);
        service.require(owner!=null&&owner.core!=null,"This kingdom no longer has a core.");
        service.require(!owner.core.carried,"Secure this core before changing its block.");
        service.require(holder.id.equals(owner.id)?owner.core.tributeTo==null:holder.id.equals(owner.core.tributeTo),"Your kingdom must hold this core to change its block.");
        service.peaceful(owner);return owner;
    }
    public void beginSelection(ServerPlayer player,UUID ownerId){
        Kingdom owner=movableCore(player,ownerId),holder=service.requireOwn(player.getUUID());
        selecting.put(player.getUUID(),new Selection(owner.id,holder.id,owner.core.nonce,service.now()+120_000));if(player.connection!=null)player.closeContainer();
        tell(player,"Right-click an existing block in your kingdom within two minutes to move "+owner.display()+"'s core.",false);
    }
    public void leave(UUID player){selecting.remove(player);highlights.leave(player);}
    private void tell(ServerPlayer player,String text,boolean error){if(player.connection!=null)Ui.tell(player,text,error);}
    public boolean use(ServerPlayer player,InteractionHand hand,BlockPos pos){
        ItemStack held=player.getItemInHand(hand);
        if(isToken(held)){
            Ui.run(player,()->place(player,held,pos));return true;
        }
        Selection selection=selecting.get(player.getUUID());if(selection==null)return false;
        if(selection.until<=service.now()){selecting.remove(player.getUUID());tell(player,"Core selection expired. Choose Change block again.",true);return true;}
        Ui.run(player,()->{
            Kingdom owner=movableCore(player,selection.owner),own=service.requireOwn(player.getUUID());
            service.require(own.id.equals(selection.holder)&&owner.core.nonce.equals(selection.nonce),"This core changed since selection began. Choose Change block again.");
            Kingdom land=service.land(player.level(),pos);service.require(land!=null&&own.id.equals(land.id),"The center block must be inside your kingdom.");
            service.require(!reserved(player.level(),pos,owner.id),"Another core reserves this block.");
            service.edit(owner.id,k->{setLocation(k.core,(ServerLevel)player.level(),pos);k.core.nonce=UUID.randomUUID();});
            selecting.remove(player.getUUID());tell(player,owner.display()+"'s core block updated.",false);service.audit(player.getUUID(),"moved core for "+owner.name+" within "+own.name);
        });return true;
    }
    public boolean canBreak(ServerPlayer player,Kingdom owner){
        Kingdom host=service.land(player.level(),position(owner.core));
        return host!=null&&Rules.enemy(service.state,host,player.getUUID(),service.now());
    }
    /** Called by the vanilla player-break wrapper, so real tool/block rules and ordinary drops still apply. */
    public boolean breakCore(ServerPlayer player,BlockPos pos,BooleanSupplier vanillaBreak){
        Kingdom owner=at(player.level(),pos);if(owner==null)return vanillaBreak.getAsBoolean();
        if(!canBreak(player,owner)){tell(player,"A town center can only be taken by the opposing kingdom during active wartime. Mayors move it through Settings.",true);return false;}
        Kingdom carrier=service.requireOwn(player.getUUID());
        boolean broken;changing=true;try{broken=vanillaBreak.getAsBoolean();}finally{changing=false;}
        if(!broken)return false;
        service.edit(owner.id,k->{k.core.carried=true;k.core.carriedByKingdom=carrier.id;k.core.takenAt=service.now();k.core.nonce=UUID.randomUUID();});
        ItemStack token=token(service.state.kingdoms.get(owner.id));if(!player.getInventory().add(token))player.drop(token,false);
        service.announce(carrier.display()+" took "+owner.display()+"'s core. Right-click a block in your own claim to secure it before it returns.");
        return true;
    }
    public static boolean isToken(ItemStack stack){return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().contains("kingdoms_core");}
    public static ItemStack token(Kingdom kingdom){
        ItemStack stack=new ItemStack(Items.NETHER_STAR);CompoundTag tag=new CompoundTag();
        tag.putString("kingdoms_core",kingdom.id.toString());tag.putString("kingdoms_core_nonce",kingdom.core.nonce.toString());
        stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE,true);
        stack.set(DataComponents.MAX_STACK_SIZE,1);
        stack.set(DataComponents.CUSTOM_NAME,Component.literal(Ui.caps(kingdom.display()+" core")));
        stack.set(DataComponents.LORE,new ItemLore(List.of(Component.literal(Ui.caps("Right-click a block in your kingdom to secure it.")),Component.literal(Ui.caps("Cannot be stored. Returns if unplaced.")))));
        return stack;
    }
    public void place(ServerPlayer player,ItemStack token,BlockPos pos){
        CompoundTag tag=token.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();Kingdom owner;
        try{owner=service.state.kingdoms.get(UUID.fromString(tag.getStringOr("kingdoms_core","")));}catch(IllegalArgumentException e){throw new IllegalArgumentException("Invalid core item.");}
        service.require(owner!=null&&owner.core!=null&&owner.core.carried&&owner.core.nonce.toString().equals(tag.getStringOr("kingdoms_core_nonce","")),"This core item expired or was already secured.");
        service.require(service.now()<owner.core.takenAt+service.config.minute(service.config.coreReturnMinutes),"This core's return timer has expired.");
        Kingdom own=service.requireOwn(player.getUUID());Kingdom land=service.land(player.level(),pos);
        service.require(land!=null&&own.id.equals(land.id),"Right-click an existing block inside your own kingdom.");
        service.require(own.id.equals(owner.id)||own.id.equals(owner.core.carriedByKingdom),"Only the kingdom that took this core, or its original kingdom, can secure it.");
        service.require(service.permission(land,player.getUUID(),Permission.PLACE),"You need building permission to secure a core.");
        service.require(!reserved(player.level(),pos,owner.id),"Another core reserves this block.");
        boolean independence=own.id.equals(owner.id);
        service.edit(owner.id,k->{setLocation(k.core,(ServerLevel)player.level(),pos);k.core.tributeTo=independence?null:own.id;k.core.carried=false;k.core.carriedByKingdom=null;k.core.takenAt=0;k.core.nonce=UUID.randomUUID();k.core.taxRemainders.clear();});
        token.shrink(1);
        service.announce(independence?owner.display()+" restored its core and regained independence.":own.display()+" secured "+owner.display()+"'s core and now collects "+service.config.coreTaxPercent+"% tribute.");
    }
    public void tick(){
        long now=service.now();selecting.values().removeIf(selection->selection.until<=now);
        for(Kingdom kingdom:new ArrayList<>(service.state.kingdoms.values())){
            Core core=kingdom.core;
            if(core==null){
                String claim=kingdom.claims.stream().filter(c->!kingdom.outposts.contains(c)).sorted().findFirst().orElseGet(()->kingdom.claims.iterator().next());
                String[] parts=claim.split("\\|");ServerLevel level=level(parts[0]);if(level==null)continue;
                Core created;
                try{created=initial(level,new BlockPos(Integer.parseInt(parts[1])*16,level.getMinY(),Integer.parseInt(parts[2])*16));}catch(IllegalArgumentException emptyColumn){continue;}
                service.edit(kingdom.id,k->k.core=created);continue;
            }
            if(core.carried&&now>=core.takenAt+service.config.minute(service.config.coreReturnMinutes)){returnCore(kingdom);continue;}
        }
    }
    private ServerLevel level(String dimension){return java.util.stream.StreamSupport.stream(service.server.getAllLevels().spliterator(),false).filter(l->KingdomService.dimension(l).equals(dimension)).findFirst().orElse(null);}
    public void returnCore(Kingdom kingdom){
        Core core=kingdom.core;ServerLevel level=level(core.dimension);if(level==null)return;
        BlockPos pos=position(core);
        if(!suitable(level,pos)){
            if(!level.getBlockState(pos).isAir()&&!level.getBlockState(pos).is(BlockTags.FIRE)&&!(level.getBlockState(pos).getBlock() instanceof LiquidBlock))return;
            var replacement=BuiltInRegistries.BLOCK.getValue(Identifier.parse(core.block)).defaultBlockState();
            Kingdom host=service.state.kingdoms.get(core.tributeTo==null?kingdom.id:core.tributeTo);if(host==null)return;
            try(var scope=Protection.scope(level,pos,host.mayors.iterator().next(),null,Protection.Cause.PLAYER)){
                changing=true;try{if(!level.setBlockAndUpdate(pos,replacement))return;}finally{changing=false;}
            }
        }
        if(at(level,pos)!=null||!suitable(level,pos))return;
        BlockPos returned=pos;service.edit(kingdom.id,k->{setLocation(k.core,level,returned);k.core.carried=false;k.core.carriedByKingdom=null;k.core.takenAt=0;k.core.nonce=UUID.randomUUID();});
        service.announce(kingdom.display()+"'s unplaced core returned to its previous territory.");
    }
    public void releaseHeldBy(State state,UUID kingdom){
        for(Kingdom k:state.kingdoms.values())if(k.core!=null&&kingdom.equals(k.core.tributeTo)){k.core=null;}
    }
}

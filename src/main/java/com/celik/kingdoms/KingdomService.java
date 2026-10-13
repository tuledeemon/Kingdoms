package com.celik.kingdoms;

import com.reazip.economycraft.api.v1.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import static com.celik.kingdoms.Rules.*;

public final class KingdomService {
    public final MinecraftServer server;
    public final Store store;
    public final PlayerFeedback feedback;
    public final KingdomCores cores;
    public final AirDrops airDrops;
    public final Artifacts artifacts;
    public final KingdomMaps maps;
    public Config config;
    public State state;
    private final Map<String, Kingdom> claims = new HashMap<>();
    private final Map<UUID, String> beaconTerritories = new HashMap<>();
    private final Map<UUID, EnumMap<BeaconPower, Integer>> appliedBeaconEffects = new HashMap<>();
    private boolean healthy = true;
    private com.reazip.economycraft.api.v1.ListenerRegistration tributeListener;
    public void startTribute(){if(tributeListener==null)tributeListener=api().balanceEvents().register(this::taxIncome);}
    public void stopTribute(){if(tributeListener!=null){tributeListener.unregister();tributeListener=null;}}
    private long creditTribute(State next,UUID player,long income){
        Kingdom victim=Rules.membership(next,player);
        if(victim==null||victim.core==null||victim.core.tributeTo==null)return 0;
        Kingdom recipient=next.kingdoms.get(victim.core.tributeTo);if(recipient==null)return 0;
        Tribute tax=Rules.tribute(income,config.coreTaxPercent,victim.core.taxRemainders.getOrDefault(player,0));
        victim.core.taxRemainders.put(player,tax.remainder());recipient.treasury=Math.addExact(recipient.treasury,tax.amount());
        require(recipient.treasury<=api().balances().getMaximumBalance(),"The tribute treasury balance limit would be exceeded.");return tax.amount();
    }
    private void taxIncome(com.reazip.economycraft.api.v1.BalanceChangeEvent event){
        if(event.difference()<=0||event.source().map(source->source.namespace().equals("kingdoms")).orElse(false))return;
        Kingdom victim=own(event.playerId());if(victim==null||victim.core==null||victim.core.tributeTo==null)return;
        try{checkHealthy();State next=store.copy(state);long tax=creditTribute(next,event.playerId(),event.difference());transfer(next,event.playerId(),-tax,"core_tax");}
        catch(RuntimeException failure){healthy=false;KingdomsMod.LOG.error("Core tribute failed for {}; kingdom changes are locked pending administrator recovery",event.playerId(),failure);}
    }
    public KingdomService(MinecraftServer server, Path path, Config config) {
        this.server=server; this.config=config; store=new Store(path);
        state=store.read("kingdoms.json",State.class,new State()); store.validate(state);
        Store.Journal journal=store.read("transaction.json",Store.Journal.class,null);
        if(journal!=null) {
            long balance=api().balances().getBalance(journal.player);
            if(balance==journal.after) { store.validate(journal.result); state=journal.result; store.write("kingdoms.json",state); }
            else if(balance!=journal.before) throw new IllegalStateException("Uncertain economy transaction. Restore/reconcile transaction.json before opening the server.");
            store.clearJournal();
        }
        index();
        feedback=new PlayerFeedback(this);
        cores=new KingdomCores(this);
        artifacts=new Artifacts(this);
        airDrops=new AirDrops(this);artifacts.reconcile();
        Provenance.load(store);
        maps=KingdomMaps.start(this);
    }
    public EconomyCraftApi api() { return EconomyCraftApi.get(server); }
    public long now() { return System.currentTimeMillis(); }
    public String money(long amount) { return api().formatMoney(amount); }
    public static String dimension(Level level) { return level.dimension().identifier().toString(); }
    public static String key(Level level, BlockPos pos) { return dimension(level)+"|"+(pos.getX()>>4)+"|"+(pos.getZ()>>4); }
    public Kingdom land(Level level, BlockPos pos) { return claims.get(key(level,pos)); }
    public Kingdom own(UUID player) { return Rules.membership(state,player); }
    public Kingdom requireOwn(UUID player) { Kingdom k=own(player); require(k!=null,"You are not in a kingdom."); return k; }
    public Kingdom named(String name) { return state.kingdoms.values().stream().filter(k->k.name.equalsIgnoreCase(name)).findFirst().orElseThrow(()->new IllegalArgumentException("Kingdom not found.")); }
    public void require(boolean condition, String message) { if(!condition) throw new IllegalArgumentException(message); }
    public void mayor(Kingdom k,UUID p) { require(k.mayor(p),"Only a mayor can do that."); }
    public void peaceful(Kingdom k) { require(Rules.conflict(state,k.id,now())==null,"This cannot change during war preparation or wartime."); }
    public static int chunkDistance(BlockPos first,BlockPos second){return Math.max(Math.abs((first.getX()>>4)-(second.getX()>>4)),Math.abs((first.getZ()>>4)-(second.getZ()>>4)));}
    public int spawnDistanceChunks(Level level,BlockPos pos){if(!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)||!dimension(level).equals("minecraft:overworld"))return Integer.MAX_VALUE;return chunkDistance(pos,serverLevel.getRespawnData().pos());}
    private void claimLocation(ServerPlayer p){require(!airDrops.protectsChunk(p.level(),p.blockPosition()),"An active AirDrop protects this chunk. Wait until it ends before claiming here.");require(config.allowedDimensions.contains(dimension(p.level())),"Claims are disabled in this dimension.");if(config.spawnClaimProtectionChunks<=0)return;int distance=spawnDistanceChunks(p.level(),p.blockPosition());if(distance<=config.spawnClaimProtectionChunks)throw new IllegalArgumentException("Claims are blocked within "+config.spawnClaimProtectionChunks+" chunks of overworld spawn. You are "+distance+" chunk"+(distance==1?"":"s")+" away; move at least "+(config.spawnClaimProtectionChunks-distance+1)+" farther.");}
    public int memberCap(Kingdom k) { return k.memberCap>0?k.memberCap:config.maxMembers; }
    public int claimCap(Kingdom k) { return k.claimCap>0?k.claimCap:config.maxClaims; }
    public int claimSlots(Kingdom k) {return claimSlots(k,config.outpostClaimWeight);}
    static int claimSlots(Kingdom k,int outpostWeight) {return Math.addExact(k.claims.size(),Math.multiplyExact(k.outposts.size(),outpostWeight-1));}
    public boolean permission(Kingdom k, UUID p, Permission permission) { return Rules.permitted(state,k,p,permission,now()); }
    public boolean flag(Kingdom k, Flag flag) { return Rules.flag(state,k,flag,now()); }
    public boolean operatorBypass(UUID player) {if(player==null)return false;var required=switch(config.opLevel){case 1->Permissions.COMMANDS_MODERATOR;case 3->Permissions.COMMANDS_ADMIN;case 4->Permissions.COMMANDS_OWNER;default->Permissions.COMMANDS_GAMEMASTER;};ServerPlayer online=server.getPlayerList().getPlayer(player);if(online!=null&&online.permissions().hasPermission(required))return true;var entry=server.getPlayerList().getOps().get(new net.minecraft.server.players.NameAndId(player,""));return entry!=null&&entry.permissions().hasPermission(required);}
    private void index() { claims.clear(); state.kingdoms.values().forEach(k->k.claims.forEach(c->claims.put(c,k))); }
    private void checkHealthy() { if(!healthy) throw new IllegalStateException("Kingdoms storage needs administrator attention; changes are locked."); }
    public void commit(State next) {
        checkHealthy(); store.validate(next);
        try { store.write("kingdoms.json",next); state=next; index(); }
        catch(RuntimeException e) { healthy=false; throw e; }
    }
    public void edit(UUID kingdom, Consumer<Kingdom> edit) { State next=store.copy(state); edit.accept(next.kingdoms.get(kingdom)); commit(next); }
    /** The receipt and treasury credit commit together; solo credits use the existing economy journal. */
    public void airDropMoney(UUID reward,UUID kingdom,UUID player,long amount){
        checkHealthy();if(state.airDropMoneyReceipts.contains(reward))return;require(amount>0,"Invalid AirDrop cash amount");State next=store.copy(state);next.airDropMoneyReceipts.add(reward);
        if(kingdom==null){long tribute=creditTribute(next,player,amount);transfer(next,player,amount-tribute,"airdrop_reward");return;}
        Kingdom k=next.kingdoms.get(kingdom);require(k!=null,"The winning kingdom disbanded; its treasury reward cannot be collected.");long tax=0;
        if(k.core!=null&&k.core.tributeTo!=null){Kingdom captor=next.kingdoms.get(k.core.tributeTo);if(captor!=null){Tribute tribute=Rules.tribute(amount,config.coreTaxPercent,k.core.taxRemainders.getOrDefault(k.id,0));tax=tribute.amount();k.core.taxRemainders.put(k.id,tribute.remainder());captor.treasury=Math.addExact(captor.treasury,tax);require(captor.treasury<=api().balances().getMaximumBalance(),"Captor treasury balance limit would be exceeded.");}}
        k.treasury=Math.addExact(k.treasury,amount-tax);require(k.treasury<=api().balances().getMaximumBalance(),"Kingdom treasury balance limit would be exceeded.");commit(next);
    }
    private void transfer(State next,UUID player,long delta,String reason) {
        checkHealthy(); store.validate(next);
        if(delta==0) { commit(next); return; }
        BalanceApi balances=api().balances(); long before=balances.getBalance(player), after=Math.addExact(before,delta);
        require(after>=0,"Not enough money."); require(after<=balances.getMaximumBalance(),"That would exceed the EconomyCraft balance limit.");
        Store.Journal journal=new Store.Journal(); journal.player=player; journal.before=before; journal.after=after; journal.result=next; journal.reason=reason;
        try {
            store.write("transaction.json",journal);
            BalanceMutationResult result=delta>0?balances.addMoney(player,delta,MutationSource.of("kingdoms:"+reason)):balances.removeMoney(player,-delta,MutationSource.of("kingdoms:"+reason));
            if(!result.successful()) { store.clearJournal(); throw new IllegalArgumentException("EconomyCraft rejected payment: "+result.status()); }
            store.write("kingdoms.json",next); state=next; index(); store.clearJournal();
        } catch(IllegalArgumentException e) { throw e; }
        catch(RuntimeException e) { healthy=false; throw new IllegalStateException("Economy transaction locked for recovery. Contact an administrator.",e); }
    }
    public void create(ServerPlayer p,String name) {
        require(own(p.getUUID())==null,"Leave your current kingdom first.");
        require(name.matches("[A-Za-z0-9_ -]{3,"+config.maxNameLength+"}") && name.equals(name.trim()),"Use 3.."+config.maxNameLength+" letters, numbers, spaces, underscores or hyphens.");
        require(state.kingdoms.values().stream().noneMatch(k->k.name.equalsIgnoreCase(name)),"That name is already taken.");
        claimLocation(p);
        require(land(p.level(),p.blockPosition())==null,"This chunk is already claimed.");
        Nearby nearby=nearestClaim(p.level(),p.blockPosition());
        if(nearby!=null&&nearby.distance<=config.kingdomCreationBufferChunks){int farther=config.kingdomCreationBufferChunks-nearby.distance+1;throw new IllegalArgumentException("Kingdom creation is blocked within "+config.kingdomCreationBufferChunks+" chunks of another kingdom. Nearest: "+nearby.kingdom.display()+" ("+nearby.distance+" chunks away). Move at least "+farther+" more chunk"+(farther==1?".":"s."));}
        State next=store.copy(state); Kingdom k=new Kingdom(); k.name=name;k.displayName=name;k.members.put(p.getUUID(),p.getName().getString()); k.mayors.add(p.getUUID());
        k.memberPermissions=new HashSet<>(config.defaultMemberPermissions); k.publicPermissions=new HashSet<>(config.defaultPublicPermissions); k.flags=new HashSet<>(config.defaultFlags);
        k.claims.add(key(p.level(),p.blockPosition())); long createdAt=now();k.nextUpkeep=createdAt+config.minute(config.upkeepIntervalMinutes);k.protectedUntil=config.newKingdomProtectionMinutes==0?0:Math.addExact(createdAt,config.minute(config.newKingdomProtectionMinutes));next.kingdoms.put(k.id,k);
        k.cooldownUntil=state.playerCooldowns.getOrDefault(p.getUUID(),0L);
        k.core=cores.initial((net.minecraft.server.level.ServerLevel)p.level(),p.blockPosition());
        transfer(next,p.getUUID(),-config.creationFee,"create"); Kingdom created=state.kingdoms.get(k.id);if(config.broadcastKingdomCreation)feedback.kingdomCreated(p,created);audit(p.getUUID(),"created "+name);
    }
    public void claim(ServerPlayer p) {
        Kingdom k=requireOwn(p.getUUID()); mayor(k,p.getUUID()); peaceful(k);
        require(claimSlots(k)<claimCap(k),"The kingdom has reached its claim cap.");
        claimLocation(p);String dim=dimension(p.level()), key=key(p.level(),p.blockPosition());
        require(!claims.containsKey(key),"This chunk is already claimed.");
        require(config.allowMultipleDimensions || k.claims.stream().anyMatch(c->c.startsWith(dim+"|")),"Multi-dimension claims are disabled.");
        int x=p.blockPosition().getX()>>4,z=p.blockPosition().getZ()>>4;
        boolean adjacent=k.claims.contains(dim+"|"+(x+1)+"|"+z)||k.claims.contains(dim+"|"+(x-1)+"|"+z)||k.claims.contains(dim+"|"+x+"|"+(z+1))||k.claims.contains(dim+"|"+x+"|"+(z-1));
        require(!config.adjacentExpansion || adjacent || (config.allowMultipleDimensions && k.claims.stream().noneMatch(c->c.startsWith(dim+"|"))),"Expansion must touch an existing claim or begin with an outpost.");
        require(k.treasury>=config.expansionFee,"The treasury cannot afford expansion.");
        edit(k.id,n->{n.treasury-=config.expansionFee;n.claims.add(key);}); audit(p.getUUID(),"claimed "+key);
    }
    public void outpost(ServerPlayer p) {
        Kingdom k=requireOwn(p.getUUID());mayor(k,p.getUUID());peaceful(k);
        require(config.maxOutposts>0,"Outposts are disabled.");require(k.outposts.size()<config.maxOutposts,"The kingdom has reached its outpost cap.");
        require(claimSlots(k)+config.outpostClaimWeight<=claimCap(k),"This outpost needs "+config.outpostClaimWeight+" free claim slots.");
        claimLocation(p);String dim=dimension(p.level()), key=key(p.level(),p.blockPosition());
        require(!claims.containsKey(key),"This chunk is already claimed.");require(config.allowMultipleDimensions||k.claims.stream().anyMatch(c->c.startsWith(dim+"|")),"Multi-dimension claims are disabled.");
        require(k.treasury>=config.outpostFee,"The treasury cannot afford an outpost.");
        edit(k.id,n->{n.treasury-=config.outpostFee;n.claims.add(key);n.outposts.add(key);});audit(p.getUUID(),"claimed outpost "+key);
    }
    public void unclaim(ServerPlayer p) {
        Kingdom k=requireOwn(p.getUUID()); mayor(k,p.getUUID()); peaceful(k); String key=key(p.level(),p.blockPosition());
        require(k.claims.contains(key),"Your kingdom does not own this chunk."); require(k.claims.size()>1,"Disband the kingdom to remove its last claim.");
        require(state.kingdoms.values().stream().noneMatch(owner->owner.core!=null&&owner.core.claimKey().equals(key)),"Move or recover every core in this chunk before unclaiming it.");
        Set<String> remaining=new HashSet<>(k.claims),anchors=new HashSet<>(k.outposts);remaining.remove(key);anchors.remove(key);
        if(config.adjacentExpansion) require(outpostTopology(remaining,anchors),k.outposts.contains(key)?"Remove this outpost's connected expansion chunks first.":"That would split a claim region. Remove an edge chunk first.");
        edit(k.id,n->{n.claims.remove(key);n.outposts.remove(key);}); audit(p.getUUID(),"unclaimed "+key);
    }
    static boolean connected(Set<String> claims) {return outpostTopology(claims,Set.of());}
    static boolean outpostTopology(Set<String> claims,Set<String> outposts) {
        if(claims.isEmpty()||!claims.containsAll(outposts))return false;
        Map<String,Set<String>> byDim=new HashMap<>();for(String c:claims)byDim.computeIfAbsent(c.split("\\|")[0],q->new HashSet<>()).add(c);
        for(var entry:byDim.entrySet()) {Set<String> unseen=new HashSet<>(entry.getValue());int unanchored=0;
            while(!unseen.isEmpty()){Set<String> component=new HashSet<>();Deque<String> queue=new ArrayDeque<>();queue.add(unseen.iterator().next());
                while(!queue.isEmpty()){String c=queue.remove();if(!unseen.remove(c))continue;component.add(c);String[] parts=c.split("\\|");int x=Integer.parseInt(parts[1]),z=Integer.parseInt(parts[2]);for(String adjacent:List.of(parts[0]+"|"+(x+1)+"|"+z,parts[0]+"|"+(x-1)+"|"+z,parts[0]+"|"+x+"|"+(z+1),parts[0]+"|"+x+"|"+(z-1)))if(unseen.contains(adjacent))queue.add(adjacent);}
                if(Collections.disjoint(component,outposts)&&++unanchored>1)return false;
            }
        }
        return true;
    }
    public void treasury(ServerPlayer p,long amount,boolean withdraw) {
        Kingdom k=requireOwn(p.getUUID()); require(amount>0,"Amount must be positive.");
        require(permission(k,p.getUUID(),withdraw?Permission.WITHDRAW:Permission.DEPOSIT),"You do not have that treasury permission.");
        require(!withdraw||config.allowTreasuryWithdrawals,"Treasury withdrawals are disabled."); require(!withdraw||k.treasury>=amount,"The treasury does not have enough money.");
        State next=store.copy(state); Kingdom n=next.kingdoms.get(k.id); n.treasury=Math.addExact(n.treasury,withdraw?-amount:amount);
        require(n.treasury<=api().balances().getMaximumBalance(),"The treasury balance limit would be exceeded.");
        long tax=withdraw?creditTribute(next,p.getUUID(),amount):0;
        transfer(next,p.getUUID(),withdraw?amount-tax:-amount,withdraw?"withdraw":"deposit"); audit(p.getUUID(),(withdraw?"withdrew ":"deposited ")+amount+" in "+k.name); tick();
    }
    public void invite(ServerPlayer p,UUID target) { Kingdom k=requireOwn(p.getUUID());mayor(k,p.getUUID());peaceful(k);require(own(target)==null,"That player is already in a kingdom.");require(k.members.size()<memberCap(k),"Member cap reached.");State n=store.copy(state);n.invitations.put(target,k.id);commit(n); }
    public void join(ServerPlayer p) { require(own(p.getUUID())==null,"You are already a member.");UUID id=state.invitations.get(p.getUUID()); Kingdom k=state.kingdoms.get(id);require(k!=null,"You have no valid invitation.");require(state.rejoinCooldowns.getOrDefault(p.getUUID(),Map.of()).getOrDefault(k.id,0L)<=now(),"You must wait "+config.kingdomRejoinCooldownMinutes+" minutes after leaving or being kicked before rejoining this kingdom.");peaceful(k);require(k.members.size()<memberCap(k),"Member cap reached.");State n=store.copy(state);n.kingdoms.get(k.id).members.put(p.getUUID(),p.getName().getString());n.invitations.remove(p.getUUID());n.kingdoms.get(k.id).cooldownUntil=Math.max(n.kingdoms.get(k.id).cooldownUntil,n.playerCooldowns.getOrDefault(p.getUUID(),0L));commit(n); }
    public void removeMember(ServerPlayer actor,UUID target) { Kingdom k=requireOwn(actor.getUUID());if(!actor.getUUID().equals(target))mayor(k,actor.getUUID());peaceful(k);require(k.members.containsKey(target),"That player is not a member."); require(!k.mayor(target)||k.mayors.size()>1,"The last mayor must appoint another mayor or disband.");State next=store.copy(state);Kingdom changed=next.kingdoms.get(k.id);changed.members.remove(target);changed.mayors.remove(target);changed.grants.remove(target);next.rejoinCooldowns.computeIfAbsent(target,q->new HashMap<>()).put(k.id,Math.addExact(now(),config.minute(config.kingdomRejoinCooldownMinutes)));commit(next); }
    public void setMayor(ServerPlayer p,UUID target,boolean value) {Kingdom k=requireOwn(p.getUUID());mayor(k,p.getUUID());peaceful(k);require(k.members.containsKey(target),"The player must be a member.");require(value||!k.mayor(target)||k.mayors.size()>1,"A kingdom needs at least one mayor.");edit(k.id,n->{if(value)n.mayors.add(target);else n.mayors.remove(target);});}
    public void grant(ServerPlayer p,UUID target,Permission perm,boolean value) {Kingdom k=requireOwn(p.getUUID());mayor(k,p.getUUID());peaceful(k);edit(k.id,n->{Set<Permission> set=n.grants.computeIfAbsent(target,q->new HashSet<>());if(value)set.add(perm);else set.remove(perm);});}
    public void setFlag(ServerPlayer p,Flag flag,boolean value) { Kingdom k=requireOwn(p.getUUID());mayor(k,p.getUUID());peaceful(k);edit(k.id,n->{if(value)n.flags.add(flag);else n.flags.remove(flag);}); }
    public void setDefaultPermission(ServerPlayer p,boolean publicAccess,Permission perm,boolean value) {Kingdom k=requireOwn(p.getUUID());mayor(k,p.getUUID());peaceful(k);require(!publicAccess||config.allowPublicPermissions,"Public grants are disabled.");require(!publicAccess||(perm!=Permission.DEPOSIT&&perm!=Permission.WITHDRAW),"Treasury access requires membership.");edit(k.id,n->{Set<Permission> set=publicAccess?n.publicPermissions:n.memberPermissions;if(value)set.add(perm);else set.remove(perm);});}
    public void protect(ServerPlayer p) {Kingdom k=requireOwn(p.getUUID());mayor(k,p.getUUID());peaceful(k);require(!Rules.holdsForeignCore(state,k.id),"You cannot buy war protection while holding another kingdom's core.");require(k.protectedUntil<=now(),"Protection is already active; it cannot be stacked.");require(k.treasury>=config.protectionFee,"The treasury cannot afford protection.");edit(k.id,n->{n.treasury-=config.protectionFee;n.protectedUntil=now()+config.minute(config.protectionMinutes);});audit(p.getUUID(),"bought protection for "+k.name);}
    public void declare(ServerPlayer p,UUID target) {
        tick(); Kingdom a=requireOwn(p.getUUID()),b=state.kingdoms.get(target);require(b!=null,"That kingdom no longer exists.");
        Set<UUID> online=new HashSet<>();server.getPlayerList().getPlayers().forEach(q->online.add(q.getUUID()));String failure=Rules.declarationFailure(state,a,b,p.getUUID(),online,now());require(failure==null,failure);
        State n=store.copy(state);War w=new War();w.first=a.id;w.second=b.id;w.declaredAt=now();w.startedAt=w.declaredAt+config.second(config.warPreparationSeconds);w.endsAt=w.startedAt+config.minute(config.warMinutes);w.cooldownMillis=config.minute(config.warCooldownMinutes);if(config.warPreparationSeconds>0)w.announcedMilestones.add(config.warPreparationSeconds);n.wars.add(w);
        n.kingdoms.get(a.id).cooldownUntil=w.endsAt+w.cooldownMillis;n.kingdoms.get(b.id).cooldownUntil=w.endsAt+w.cooldownMillis;for(UUID member:a.members.keySet())n.playerCooldowns.put(member,w.endsAt+w.cooldownMillis);for(UUID member:b.members.keySet())n.playerCooldowns.put(member,w.endsAt+w.cooldownMillis);commit(n);
        feedback.warDeclared(state.wars.stream().filter(q->q.id.equals(w.id)).findFirst().orElse(w));audit(p.getUUID(),"declared war against "+b.name);
    }
    public record Nearby(Kingdom kingdom,int distance) {}
    public Nearby nearestClaim(Level level,BlockPos pos) {
        String dim=dimension(level);int x=pos.getX()>>4,z=pos.getZ()>>4;Nearby best=null;
        for(Kingdom k:state.kingdoms.values())for(String claim:k.claims){String[] parts=claim.split("\\|");if(parts.length!=3||!parts[0].equals(dim))continue;int distance=Math.max(Math.abs(x-Integer.parseInt(parts[1])),Math.abs(z-Integer.parseInt(parts[2])));if(best==null||distance<best.distance)best=new Nearby(k,distance);}
        return best;
    }
    public void setIcon(ServerPlayer p) {Kingdom k=requireOwn(p.getUUID());mayor(k,p.getUUID());peaceful(k);var held=p.getMainHandItem();require(!held.isEmpty(),"Hold the item you want to use as the kingdom icon.");require(!(held.getItem() instanceof net.minecraft.world.item.BlockItem),"Block icons are not supported. Hold a non-block item.");String id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(held.getItem()).toString();edit(k.id,n->n.iconItem=id);audit(p.getUUID(),"changed icon for "+k.name+" to "+id);}
    public long beaconCost(BeaconPower power,int level){return KingdomBeacon.cost(config,power,level);}
    public List<BeaconEffect> beaconEffects(UUID kingdom){long now=now();return state.beaconEffects.stream().filter(e->e.active(now)&&(e.sourceKingdom.equals(kingdom)||e.targetKingdom.equals(kingdom))).toList();}
    public void buyBeaconEffect(ServerPlayer buyer,BeaconPower power,int level){
        require(config.beaconEnabled,"The kingdom beacon is disabled.");Kingdom source=requireOwn(buyer.getUUID());mayor(source,buyer.getUUID());
        require(level==1||(power.beneficial()&&level==2),"Only member powers support Level II.");long now=now();Kingdom target=source;
        if(!power.beneficial()){War war=Rules.war(state,source.id,now);require(war!=null,"Opponent powers can only be purchased during active wartime.");target=state.kingdoms.get(war.opponent(source.id));require(target!=null,"The enemy kingdom no longer exists.");}
        long cost=beaconCost(power,level);require(source.treasury>=cost,"The treasury cannot afford this beacon power.");UUID targetId=target.id;
        BeaconEffect existing=state.beaconEffects.stream().filter(e->e.active(now)&&e.sourceKingdom.equals(source.id)&&e.targetKingdom.equals(targetId)&&e.power==power).findFirst().orElse(null);
        require(existing==null||level>existing.level,"That beacon power is already active at this level or higher.");
        State next=store.copy(state);Kingdom changed=next.kingdoms.get(source.id);changed.treasury-=cost;next.beaconEffects.removeIf(e->e.sourceKingdom.equals(source.id)&&e.targetKingdom.equals(targetId)&&e.power==power);
        BeaconEffect effect=new BeaconEffect();effect.sourceKingdom=source.id;effect.targetKingdom=targetId;effect.power=power;effect.level=level;effect.hostile=!power.beneficial();effect.expiresAt=Math.addExact(now,config.minute(config.beaconDurationMinutes));next.beaconEffects.add(effect);commit(next);
        BeaconEffect saved=state.beaconEffects.stream().filter(e->e.id.equals(effect.id)).findFirst().orElse(effect);for(ServerPlayer player:server.getPlayerList().getPlayers())if(targetId.equals(Optional.ofNullable(own(player.getUUID())).map(k->k.id).orElse(null)))applyBeaconEffects(player);feedback.beaconPurchased(saved);audit(buyer.getUUID(),"bought beacon "+power.key()+" "+level+" for "+target.display()+" at "+cost);
    }
    public void tickBeaconEffects(){
        if(!healthy)return;long now=now();if(state.beaconEffects.stream().anyMatch(e->!e.active(now))){State next=store.copy(state);next.beaconEffects.removeIf(e->!e.active(now));commit(next);}for(ServerPlayer player:server.getPlayerList().getPlayers())applyBeaconEffects(player);
    }
    public void tickBeaconTerritories(){
        if(!healthy)return;
        for(ServerPlayer player:server.getPlayerList().getPlayers()){
            Kingdom territory=land(player.level(),player.blockPosition()),membership=own(player.getUUID());
            String token=(territory==null?"wilderness":territory.id.toString())+"|"+(membership==null?"none":membership.id.toString());
            if(!Objects.equals(beaconTerritories.put(player.getUUID(),token),token))applyBeaconEffects(player);
        }
    }
    public void leaveBeaconTerritory(ServerPlayer player){
        beaconTerritories.remove(player.getUUID());EnumMap<BeaconPower,Integer> previous=appliedBeaconEffects.remove(player.getUUID());if(previous==null)return;
        for(var entry:previous.entrySet()){var active=player.getEffect(KingdomBeacon.effect(entry.getKey()));if(active!=null&&active.isAmbient()&&active.getAmplifier()==entry.getValue())player.removeEffect(KingdomBeacon.effect(entry.getKey()));}
    }
    public void applyBeaconEffects(ServerPlayer player){
        Kingdom kingdom=own(player.getUUID());long now=now();EnumMap<BeaconPower,Integer> desired=new EnumMap<>(BeaconPower.class);
        if(config.beaconEnabled&&kingdom!=null&&land(player.level(),player.blockPosition())!=null)for(BeaconEffect effect:state.beaconEffects)if(effect.active(now)&&effect.targetKingdom.equals(kingdom.id))desired.put(effect.power,effect.level-1);
        EnumMap<BeaconPower,Integer> previous=appliedBeaconEffects.get(player.getUUID());
        if(previous!=null)for(var entry:previous.entrySet())if(!Objects.equals(desired.get(entry.getKey()),entry.getValue())){
            var active=player.getEffect(KingdomBeacon.effect(entry.getKey()));
            if(active!=null&&active.isAmbient()&&active.getAmplifier()==entry.getValue())player.removeEffect(KingdomBeacon.effect(entry.getKey()));
        }
        int ticks=Math.min(3600,Math.max(60,config.beaconRefreshTicks+40));
        for(var entry:desired.entrySet())player.addEffect(new net.minecraft.world.effect.MobEffectInstance(KingdomBeacon.effect(entry.getKey()),ticks,entry.getValue(),true,true,true));
        if(desired.isEmpty())appliedBeaconEffects.remove(player.getUUID());else appliedBeaconEffects.put(player.getUUID(),desired);
    }
    public List<Kingdom> leaderboard() {return state.kingdoms.values().stream().sorted(Comparator.comparingLong((Kingdom k)->k.treasury).reversed().thenComparing(k->k.name,String.CASE_INSENSITIVE_ORDER)).limit(config.leaderboardSize).toList();}
    public void clearWarCooldown(UUID kingdom) {
        Kingdom k=state.kingdoms.get(kingdom);require(k!=null,"Kingdom not found.");
        require(Rules.conflict(state,k.id,now())==null,"End this kingdom's war before clearing its war cooldown.");
        State next=store.copy(state);next.kingdoms.get(k.id).cooldownUntil=0;next.kingdoms.get(k.id).protectedUntil=0;
        for(UUID member:k.members.keySet())next.playerCooldowns.remove(member);
        commit(next);
    }
    public void acknowledge(ServerPlayer p) {Kingdom k=requireOwn(p.getUUID());mayor(k,p.getUUID());if(k.overdueSince>0)edit(k.id,n->n.warningAcknowledged=n.overdueSince);}
    public boolean needsWarning(ServerPlayer p) {Kingdom k=own(p.getUUID());return k!=null&&k.mayor(p.getUUID())&&k.overdueSince>0&&k.warningAcknowledged!=k.overdueSince;}
    public void disband(ServerPlayer p) {Kingdom k=requireOwn(p.getUUID());mayor(k,p.getUUID());peaceful(k);State n=store.copy(state);n.kingdoms.remove(k.id);cores.releaseHeldBy(n,k.id);n.invitations.values().removeIf(k.id::equals);n.beaconEffects.removeIf(e->e.sourceKingdom.equals(k.id)||e.targetKingdom.equals(k.id));commit(n);announce(k.name+" was disbanded. Its former claims are unprotected.");audit(p.getUUID(),"disbanded "+k.name+"; treasury forfeited "+k.treasury);}
    public void tick() {
        if(!healthy)return;long now=now();State n=null;List<War> countdowns=new ArrayList<>(),starts=new ArrayList<>(),ends=new ArrayList<>();List<Kingdom[]> endedKingdoms=new ArrayList<>();List<String> notices=new ArrayList<>();
        for(War w:state.wars) {
            if(w.endsAt<=now){if(n==null)n=store.copy(state);n.wars.removeIf(q->q.id.equals(w.id));ends.add(w);endedKingdoms.add(new Kingdom[]{state.kingdoms.get(w.first),state.kingdoms.get(w.second)});continue;}
            if(w.preparing(now)) {
                int remaining=(int)Math.max(1,(w.startedAt-now+999)/1000);List<Integer> passed=config.warCountdownMilestonesSeconds.stream().filter(m->m<=config.warPreparationSeconds&&remaining<=m&&!w.announcedMilestones.contains(m)).sorted().toList();
                if(!passed.isEmpty()){if(n==null)n=store.copy(state);War changed=n.wars.stream().filter(q->q.id.equals(w.id)).findFirst().orElseThrow();changed.announcedMilestones.addAll(passed);countdowns.add(copyForMilestone(w,passed.get(0)));}
            } else if(!w.startAnnounced) {if(n==null)n=store.copy(state);War changed=n.wars.stream().filter(q->q.id.equals(w.id)).findFirst().orElseThrow();changed.startAnnounced=true;starts.add(w);}
        }
        for(Kingdom k:state.kingdoms.values()) {
            boolean due=k.nextUpkeep<=now, pay=k.debt>0&&k.treasury>=k.debt, expire=k.overdueSince>0&&now>=k.overdueSince+config.minute(config.gracePeriodMinutes);
            if(!due&&!pay&&!expire)continue;if(n==null)n=store.copy(state);Kingdom a=n.kingdoms.get(k.id);if(a==null)continue;
            if(due){long interval=config.minute(config.upkeepIntervalMinutes),periods=(now-a.nextUpkeep)/interval+1,rate=Math.addExact(config.upkeepBase,Math.multiplyExact(config.upkeepPerMember,a.members.size()));a.debt=Math.addExact(a.debt,Math.multiplyExact(rate,periods));if(a.debt>0&&a.overdueSince==0){a.overdueSince=a.nextUpkeep;a.warningAcknowledged=0;}a.nextUpkeep=Math.addExact(a.nextUpkeep,Math.multiplyExact(interval,periods));}
            if(a.treasury>=a.debt){a.treasury-=a.debt;a.debt=0;a.overdueSince=0;a.warningAcknowledged=0;}
            if(a.overdueSince>0&&now>=a.overdueSince+config.minute(config.gracePeriodMinutes)&&Rules.conflict(n,a.id,now)==null){n.kingdoms.remove(a.id);cores.releaseHeldBy(n,a.id);n.invitations.values().removeIf(a.id::equals);n.beaconEffects.removeIf(e->e.sourceKingdom.equals(a.id)||e.targetKingdom.equals(a.id));notices.add(a.display()+" was disbanded for unpaid upkeep. Its claims are now unprotected.");}
        }
        if(n!=null)commit(n);
        for(War w:countdowns)feedback.warCountdown(w,(int)w.declaredAt);
        for(War w:starts)feedback.warStarted(w);
        for(int i=0;i<ends.size();i++){Kingdom[] pair=endedKingdoms.get(i);if(pair[0]!=null&&pair[1]!=null)feedback.warEnded(ends.get(i),pair[0],pair[1]);}
        notices.forEach(this::announce);
    }
    private War copyForMilestone(War source,int seconds){War copy=new War();copy.id=source.id;copy.first=source.first;copy.second=source.second;copy.declaredAt=seconds;copy.startedAt=source.startedAt;copy.endsAt=source.endsAt;copy.cooldownMillis=source.cooldownMillis;return copy;}
    public void announce(net.minecraft.network.chat.Component message) {server.getPlayerList().getPlayers().forEach(p->p.sendSystemMessage(message));}
    public void announce(String message) {announce(net.minecraft.network.chat.Component.literal(Ui.caps(message)).withStyle(net.minecraft.ChatFormatting.AQUA));}
    public void audit(UUID actor,String message) {if(config.logActions)KingdomsMod.LOG.info("[audit] {} {}",actor,message);}
}

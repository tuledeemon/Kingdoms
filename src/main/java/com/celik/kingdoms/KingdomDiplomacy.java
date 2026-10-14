package com.celik.kingdoms;

import java.util.*;
import java.util.stream.Collectors;
import net.minecraft.server.level.ServerPlayer;
import static com.celik.kingdoms.Rules.*;

/** Persisted, mutually approved alliances and opt-in preparation-time war recruitment. */
public final class KingdomDiplomacy {
    private final KingdomService s;
    public KingdomDiplomacy(KingdomService service){s=service;}
    public String names(Collection<UUID> ids){return ids.stream().map(s.state.kingdoms::get).filter(Objects::nonNull).map(Kingdom::display).collect(Collectors.joining(", "));}
    public List<Kingdom> allies(Kingdom k){return k.allies.stream().map(s.state.kingdoms::get).filter(Objects::nonNull).sorted(Comparator.comparing(Kingdom::display,String.CASE_INSENSITIVE_ORDER)).toList();}
    public void notifyKingdom(UUID id,String message){for(ServerPlayer p:s.server.getPlayerList().getPlayers()){Kingdom own=s.own(p.getUUID());if(own!=null&&own.id.equals(id))Ui.tell(p,message,false);}}
    private Kingdom target(UUID id){Kingdom k=s.state.kingdoms.get(id);s.require(k!=null,"That kingdom no longer exists.");return k;}
    private Kingdom mayor(ServerPlayer p){s.tick();Kingdom k=s.requireOwn(p.getUUID());s.mayor(k,p.getUUID());return k;}
    private void capacity(Kingdom a,Kingdom b){s.require(a.allies.size()<s.config.maxAllies&&b.allies.size()<s.config.maxAllies,"One of these kingdoms has reached its alliance limit.");}
    public void request(ServerPlayer p,UUID id){
        Kingdom a=mayor(p),b=target(id);s.require(s.config.alliancesEnabled,"New alliances are disabled.");s.peaceful(a);s.peaceful(b);
        s.require(!a.id.equals(b.id),"Choose another kingdom.");s.require(!Rules.allied(s.state,a.id,b.id),"These kingdoms are already allies.");capacity(a,b);
        s.require(s.state.allianceRequests.stream().noneMatch(r->r.expiresAt>s.now()&&((r.from.equals(a.id)&&r.to.equals(b.id))||(r.from.equals(b.id)&&r.to.equals(a.id)))),"An alliance request already exists. Review it in Alliances.");
        State n=s.store.copy(s.state);n.allianceRequests.removeIf(r->r.expiresAt<=s.now());AllianceRequest r=new AllianceRequest();r.from=a.id;r.to=b.id;r.expiresAt=Math.addExact(s.now(),s.config.minute(s.config.allianceRequestMinutes));n.allianceRequests.add(r);s.commit(n);
        notifyKingdom(b.id,"[Alliance] "+a.display()+" requests an alliance. A mayor can review it in /kingdom → Alliances.");Ui.tell(p,"Alliance request sent to "+b.display()+".",false);s.audit(p.getUUID(),"requested alliance with "+b.name);
    }
    public void respond(ServerPlayer p,UUID from,boolean accept){
        Kingdom own=mayor(p),other=target(from);AllianceRequest request=s.state.allianceRequests.stream().filter(r->r.from.equals(from)&&r.to.equals(own.id)&&r.expiresAt>s.now()).findFirst().orElse(null);s.require(request!=null,"This alliance request expired or was withdrawn.");
        if(accept){s.require(s.config.alliancesEnabled,"New alliances are disabled.");s.peaceful(own);s.peaceful(other);capacity(own,other);}
        State n=s.store.copy(s.state);n.allianceRequests.removeIf(r->r.from.equals(from)&&r.to.equals(own.id));if(accept){n.kingdoms.get(own.id).allies.add(from);n.kingdoms.get(from).allies.add(own.id);}s.commit(n);
        String message="[Alliance] "+own.display()+(accept?" formed an alliance with ":" declined the alliance request from ")+other.display()+".";notifyKingdom(own.id,message);notifyKingdom(from,message);s.audit(p.getUUID(),message);
    }
    public void cancel(ServerPlayer p,UUID to){Kingdom own=mayor(p);State n=s.store.copy(s.state);s.require(n.allianceRequests.removeIf(r->r.from.equals(own.id)&&r.to.equals(to)),"This request no longer exists.");s.commit(n);notifyKingdom(to,"[Alliance] "+own.display()+" withdrew its alliance request.");}
    public void remove(ServerPlayer p,UUID id){
        Kingdom own=mayor(p),other=target(id);s.peaceful(own);s.peaceful(other);s.require(Rules.allied(s.state,own.id,id),"These kingdoms are not allies.");State n=s.store.copy(s.state);n.kingdoms.get(own.id).allies.remove(id);n.kingdoms.get(id).allies.remove(own.id);s.commit(n);
        String message="[Alliance] The alliance between "+own.display()+" and "+other.display()+" has ended.";notifyKingdom(own.id,message);notifyKingdom(id,message);s.audit(p.getUUID(),message);
    }
    public String joinFailure(War war,Kingdom k,boolean attacking){
        if(!s.config.jointWarsEnabled)return "Joint wars are disabled.";
        Set<UUID> online=new HashSet<>();s.server.getPlayerList().getPlayers().forEach(p->online.add(p.getUUID()));
        return Rules.warJoinFailure(s.state,war,k,attacking,s.config.maxKingdomsPerWarSide,s.config.jointWarRequiresOnlineMajority,online,s.now());
    }
    public void inviteWar(ServerPlayer p,UUID id){
        s.tick();Kingdom own=mayor(p),other=target(id);War war=Rules.conflict(s.state,own.id,s.now());s.require(war!=null,"Declare a war before recruiting allies.");s.require(Rules.allied(s.state,own.id,id),"You can recruit only your own allies.");boolean attacking=war.side(true).contains(own.id);
        String failure=joinFailure(war,other,attacking);s.require(failure==null,failure);s.require(s.state.warInvites.stream().noneMatch(i->i.war.equals(war.id)&&i.to.equals(id)),"This kingdom already has an invitation to this war.");
        State n=s.store.copy(s.state);WarInvite invite=new WarInvite();invite.war=war.id;invite.from=own.id;invite.to=id;invite.attacking=attacking;n.warInvites.add(invite);s.commit(n);
        notifyKingdom(id,"[War] "+own.display()+" invited your kingdom to fight "+names(war.side(!attacking))+". A mayor must approve in Alliances before preparation ends. Your claims will become vulnerable.");Ui.tell(p,"War invitation sent to "+other.display()+".",false);
    }
    public void recruitAll(ServerPlayer p){Kingdom own=mayor(p);for(Kingdom ally:allies(own))try{inviteWar(p,ally.id);}catch(IllegalArgumentException e){Ui.tell(p,ally.display()+": "+e.getMessage(),true);}}
    public void answerWar(ServerPlayer p,UUID warId,boolean accept){
        s.tick();Kingdom own=mayor(p);WarInvite invite=s.state.warInvites.stream().filter(i->i.war.equals(warId)&&i.to.equals(own.id)).findFirst().orElse(null);s.require(invite!=null,"This war invitation expired or was withdrawn.");War war=s.state.wars.stream().filter(w->w.id.equals(warId)).findFirst().orElseThrow();
        if(accept){s.require(Rules.allied(s.state,own.id,invite.from),"The inviting kingdom is no longer your ally.");String failure=joinFailure(war,own,invite.attacking);s.require(failure==null,failure);}
        State n=s.store.copy(s.state);n.warInvites.removeIf(i->i.war.equals(warId)&&i.to.equals(own.id));
        if(accept){War changed=n.wars.stream().filter(w->w.id.equals(warId)).findFirst().orElseThrow();(invite.attacking?changed.firstAllies:changed.secondAllies).add(own.id);enroll(n,changed,own.id);n.warInvites.removeIf(i->i.to.equals(own.id));n.allianceRequests.removeIf(r->r.from.equals(own.id)||r.to.equals(own.id));}
        s.commit(n);
        if(accept){s.announce("[War] "+own.display()+" joined "+names(war.side(invite.attacking))+" against "+names(war.side(!invite.attacking))+". Claims become vulnerable in "+Ui.clock(war.startedAt-s.now())+".");s.feedback.warJoined(s.state.wars.stream().filter(w->w.id.equals(warId)).findFirst().orElseThrow(),own.id);}
        else {notifyKingdom(invite.from,"[War] "+own.display()+" declined your war invitation.");Ui.tell(p,"War invitation declined.",false);}s.audit(p.getUUID(),(accept?"joined":"declined")+" allied war "+warId);
    }
    public static void enroll(State n,War war,UUID id){Kingdom k=n.kingdoms.get(id);long cooldown=Math.addExact(war.endsAt,war.cooldownMillis);k.cooldownUntil=cooldown;for(UUID member:k.members.keySet())n.playerCooldowns.put(member,cooldown);}
    public void endWar(UUID id){
        s.tick();Kingdom k=target(id);War war=Rules.conflict(s.state,id,s.now());s.require(war!=null,"No preparation or active war.");State n=s.store.copy(s.state);n.wars.removeIf(w->w.id.equals(war.id));n.warInvites.removeIf(i->i.war.equals(war.id));long cooldown=Math.addExact(s.now(),war.cooldownMillis);
        for(UUID participant:war.participants()){Kingdom member=n.kingdoms.get(participant);member.cooldownUntil=cooldown;for(UUID player:member.members.keySet())n.playerCooldowns.put(player,cooldown);}s.commit(n);s.feedback.warEnded(war,s.state.kingdoms.get(war.first),s.state.kingdoms.get(war.second));s.audit(null,"administrator ended coalition war involving "+k.name);
    }
    public static void removeKingdom(State n,UUID id){for(Kingdom k:n.kingdoms.values())k.allies.remove(id);n.allianceRequests.removeIf(r->r.from.equals(id)||r.to.equals(id));n.warInvites.removeIf(i->i.from.equals(id)||i.to.equals(id));}
}

package com.celik.kingdoms;

import eu.pb4.placeholders.api.PlaceholderResult;
import eu.pb4.placeholders.api.Placeholders;
import eu.pb4.placeholders.api.ServerPlaceholderContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;
import java.util.function.BiFunction;
import static com.celik.kingdoms.Rules.*;

/** Placeholder API integration. Icon placeholders return a real object text component. */
public final class KingdomPlaceholders {
    private static boolean registered;

    public static void register() {
        if (registered) return; registered=true;
        value("player_kingdom", (c,a)->name(own(c)));
        value("kingdom_name", (c,a)->name(own(c)));
        value("kingdom_display_name", (c,a)->own(c)==null?"":own(c).display());
        result("kingdom_styled", (c,a)->PlaceholderResult.value(KingdomIcons.styled(own(c))));
        result("kingdom_icon", (c,a)->own(c)==null?PlaceholderResult.value(""):PlaceholderResult.value(KingdomIcons.component(own(c))));
        value("kingdom_icon_material", (c,a)->own(c)==null?"":KingdomIcons.material(own(c)));
        value("kingdom_treasury", (c,a)->own(c)==null?"0":service().money(own(c).treasury));
        value("kingdom_treasury_raw", (c,a)->own(c)==null?"0":Long.toString(own(c).treasury));
        value("kingdom_rank", (c,a)->rank(c));
        value("kingdom_member_count", (c,a)->own(c)==null?"0":Integer.toString(own(c).members.size()));
        value("kingdom_claim_count", (c,a)->own(c)==null?"0":Integer.toString(own(c).claims.size()));
        value("kingdom_claim_slots", (c,a)->own(c)==null?"0":Integer.toString(service().claimSlots(own(c))));
        value("kingdom_outpost_count", (c,a)->own(c)==null?"0":Integer.toString(own(c).outposts.size()));
        value("kingdom_at_war", (c,a)->bool(own(c)!=null&&Rules.war(service().state,own(c).id,service().now())!=null));
        value("war_preparation_remaining", (c,a)->preparation(c));
        value("wartime_remaining", (c,a)->wartime(c));
        value("enemy_kingdom", (c,a)->enemy(c));
        value("territory_kingdom", (c,a)->territory(c)==null?"":territory(c).display());
        result("territory_icon", (c,a)->territory(c)==null?PlaceholderResult.value(""):PlaceholderResult.value(KingdomIcons.component(territory(c))));
        value("inside_own_kingdom", (c,a)->bool(own(c)!=null&&territory(c)!=null&&own(c).id.equals(territory(c).id)));
        value("inside_enemy_territory", (c,a)->bool(player(c)!=null&&territory(c)!=null&&Rules.enemy(service().state,territory(c),player(c).getUUID(),service().now())));
        value("in_wilderness", (c,a)->bool(territory(c)==null));
        value("leaderboard_position", (c,a)->Integer.toString(position(a)));
        value("leaderboard_name", (c,a)->leaderboard(a)==null?"":leaderboard(a).display());
        value("leaderboard_treasury", (c,a)->leaderboard(a)==null?"":service().money(leaderboard(a).treasury));
        value("leaderboard_treasury_raw", (c,a)->leaderboard(a)==null?"":Long.toString(leaderboard(a).treasury));
        value("leaderboard_icon_material", (c,a)->leaderboard(a)==null?"":KingdomIcons.material(leaderboard(a)));
        result("leaderboard_icon", (c,a)->leaderboard(a)==null?PlaceholderResult.value(""):PlaceholderResult.value(KingdomIcons.component(leaderboard(a))));
        result("leaderboard_entry", (c,a)->{Kingdom k=leaderboard(a);if(k==null)return PlaceholderResult.value("");return PlaceholderResult.value(Component.literal("#"+position(a)+" ").append(KingdomIcons.styled(k)).append(Component.literal(Ui.caps(" — "+service().money(k.treasury)))));});
    }

    private static void value(String id, BiFunction<ServerPlaceholderContext,String,String> handler) { Placeholders.registerServer(Identifier.parse("kingdoms:"+id),(context,arg)->PlaceholderResult.value(handler.apply(context,arg))); }
    private static void result(String id, BiFunction<ServerPlaceholderContext,String,PlaceholderResult> handler) { Placeholders.registerServer(Identifier.parse("kingdoms:"+id),handler::apply); }
    private static KingdomService service(){return KingdomsMod.service;}
    private static ServerPlayer player(ServerPlaceholderContext c){return c.hasServerPlayer()?c.serverPlayer():null;}
    private static Kingdom own(ServerPlaceholderContext c){ServerPlayer p=player(c);return p==null||service()==null?null:service().own(p.getUUID());}
    private static Kingdom territory(ServerPlaceholderContext c){ServerPlayer p=player(c);return p==null||service()==null?null:service().land(p.level(),p.blockPosition());}
    private static String name(Kingdom k){return k==null?"":k.name;}
    private static String bool(boolean value){return Boolean.toString(value);}
    private static String rank(ServerPlaceholderContext c){Kingdom k=own(c);ServerPlayer p=player(c);return Ui.caps(k==null||p==null?"none":k.mayor(p.getUUID())?"mayor":"member");}
    private static War conflict(ServerPlaceholderContext c){Kingdom k=own(c);return k==null?null:Rules.conflict(service().state,k.id,service().now());}
    private static String preparation(ServerPlaceholderContext c){War w=conflict(c);return w!=null&&w.preparing(service().now())?Ui.clock(w.startedAt-service().now()):"0:00";}
    private static String wartime(ServerPlaceholderContext c){War w=conflict(c);return w!=null&&w.active(service().now())?Ui.clock(w.endsAt-service().now()):"0:00";}
    private static String enemy(ServerPlaceholderContext c){Kingdom k=own(c);War w=conflict(c);if(k==null||w==null)return "";Kingdom enemy=service().state.kingdoms.get(w.opponent(k.id));return enemy==null?"":enemy.display();}
    private static int position(String argument){try{return Math.max(1,Integer.parseInt(argument==null||argument.isBlank()?"1":argument.trim()));}catch(Exception e){return 1;}}
    private static Kingdom leaderboard(String argument){int index=position(argument)-1;List<Kingdom> all=service()==null?List.of():service().leaderboard();return index>=0&&index<all.size()?all.get(index):null;}
    private KingdomPlaceholders() {}
}

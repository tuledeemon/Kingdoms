package com.celik.kingdoms;

import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.*;
import net.minecraft.server.dialog.*;
import net.minecraft.server.dialog.action.CommandTemplate;
import net.minecraft.server.dialog.action.ParsedTemplate;
import net.minecraft.server.dialog.body.PlainMessage;
import net.minecraft.server.dialog.input.TextInput;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemLore;
import java.util.*;
import java.util.function.Consumer;
import static com.celik.kingdoms.Rules.*;

public final class Ui {
    private static KingdomService s(){return KingdomsMod.service;}
    public static String caps(String value) {
        if(value==null)return "";
        String normal="abcdefghijklmnopqrstuvwxyz",small="ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘǫʀꜱᴛᴜᴠᴡxʏᴢ";StringBuilder b=new StringBuilder();
        for(char c:value.toCharArray()){int i=normal.indexOf(Character.toLowerCase(c));b.append(i<0?c:small.charAt(i));}return b.toString();
    }
    public static Component text(String value){return Component.literal(caps(s()==null?value:s().config.message(value,value))).withStyle(ChatFormatting.GOLD);}
    public static void tell(ServerPlayer p,String message,boolean error){String translated=s()==null?message:s().config.message(message,message);p.sendSystemMessage(Component.literal(caps(translated)).withStyle(color(error?s().config.errorColor:s().config.secondaryColor,ChatFormatting.AQUA)));}
    static ChatFormatting color(String name,ChatFormatting fallback){ChatFormatting c=java.util.Arrays.stream(ChatFormatting.values()).filter(v -> v.name().equalsIgnoreCase(name)).findFirst().orElse(null);return c==null?fallback:c;}
    public static String time(long ms){if(ms<=0)return caps("inactive");long seconds=(ms+999)/1000;return caps(seconds/3600+"h "+seconds%3600/60+"m "+seconds%60+"s");}
    public static String clock(long ms){if(ms<=0)return "0:00";long seconds=(ms+999)/1000,h=seconds/3600,m=seconds%3600/60,s=seconds%60;return h>0?String.format(Locale.ROOT,"%d:%02d:%02d",h,m,s):String.format(Locale.ROOT,"%d:%02d",m,s);}
    private static Item item(String key,Item fallback){try{return BuiltInRegistries.ITEM.getValue(Identifier.parse(s().config.menuItems.getOrDefault(key,"minecraft:air"))) instanceof Item i&&i!=Items.AIR?i:fallback;}catch(Exception e){return fallback;}}
    public static void run(ServerPlayer p,Runnable action){try{action.run();}catch(IllegalArgumentException e){tell(p,e.getMessage(),true);}catch(Exception e){KingdomsMod.LOG.error("Kingdom action failed",e);tell(p,"Action failed safely. Ask an administrator to check the server log.",true);}}
    public static void open(ServerPlayer p,String title,Consumer<Menu> render,boolean warning){p.openMenu(new SimpleMenuProvider((id,inv,player)->new Menu(id,inv,p,title,render,warning),text(title)));}
    public static void confirm(ServerPlayer p,String title,List<String> details,Runnable action){open(p,title,m->{m.button(13,Items.PAPER,title,details,null);m.button(29,Items.EMERALD_BLOCK,"Confirm",List.of("Apply this action"),()->{action.run();home(p);});m.button(33,Items.REDSTONE_BLOCK,"Cancel",List.of(),()->home(p));},false);}
    public static void warning(ServerPlayer p){open(p,"upkeep overdue",m->{Kingdom k=s().own(p.getUUID());if(k==null||!s().needsWarning(p)){home(p);return;}m.button(13,Items.BARRIER,"Kingdom disband warning",wrap(s().config.overdueWarning),null);m.button(22,Items.CLOCK,"Time remaining: "+time(k.overdueSince+s().config.minute(s().config.gracePeriodMinutes)-s().now()),List.of("Unpaid: "+s().money(k.debt),"Treasury: "+s().money(k.treasury),"One mayor's acknowledgment covers the kingdom."),null);m.button(40,Items.EMERALD_BLOCK,s().config.acknowledgeLabel,List.of("Acknowledgment does not pay the debt.","Deposit money after reading this warning."),()->{s().acknowledge(p);treasury(p);});},true);}
    private static List<String> wrap(String text){List<String> result=new ArrayList<>();String line="";for(String word:text.split(" ")){if(line.length()+word.length()>48){result.add(line);line="";}line+=word+" ";}if(!line.isBlank())result.add(line.trim());return result;}
    public static void home(ServerPlayer p){
        if(s().needsWarning(p)){warning(p);return;}
        open(p,s().config.title,m->{Kingdom k=s().own(p.getUUID());m.root=true;if(k==null){
                m.button(13,Items.NETHER_STAR,"Your first kingdom",List.of("Creation fee: "+s().money(s().config.creationFee),"Your current chunk becomes the first claim.","Spawn claim radius: "+s().config.spawnClaimProtectionChunks+" chunks.","Starter protection: "+time(s().config.minute(s().config.newKingdomProtectionMinutes))),null);
                m.section(18,Items.STAINED_GLASS_PANE.pick(DyeColor.LIGHT_BLUE),"Getting started");
                m.button(20,Items.NETHER_STAR,"Create your kingdom",List.of("Choose a name in the input dialog."),()->kingdomNameDialog(p));
                boolean invited=s().state.invitations.containsKey(p.getUUID());
                m.button(22,invited?Items.WRITABLE_BOOK:Items.PAPER,"Accept invitation",List.of(invited?"You have a pending kingdom invitation.":"Ask a mayor to invite you first."),invited?()->{s().join(p);home(p);}:null);
                m.button(24,Items.MAP,"Browse kingdoms",List.of("View kingdoms and their status."),()->browse(p,0));
                m.section(36,Items.STAINED_GLASS_PANE.pick(DyeColor.CYAN),"Server activities");
                AirDropUi.homeButton(p,m);
                m.button(m.actionSlot(40),Items.DIAMOND_PICKAXE,"Artifacts",List.of("Inspect and upgrade rare artifacts."),()->s().artifacts.menu(p));
                m.button(42,Items.BOOK,"Help",List.of("Commands and kingdom rules."),()->help(p));
                m.button(51,Items.EMERALD,"Treasury leaderboard",List.of("Compare kingdom balances."),()->leaderboard(p));
                m.button(49,Items.BARRIER,"Close",List.of(),p::closeContainer);return;
            }
            War war=Rules.conflict(s().state,k.id,s().now());String warState=war==null?"peace":war.preparing(s().now())?"preparing "+clock(war.startedAt-s().now()):"vulnerable "+clock(war.endsAt-s().now());m.button(4,KingdomIcons.stack(k),k.display(),List.of("Members: "+k.members.size()+" / "+s().memberCap(k),"Claim slots: "+s().claimSlots(k)+" / "+s().claimCap(k),"Treasury: "+s().money(k.treasury),"War: "+warState,"Cooldown: "+time(k.cooldownUntil-s().now())),null);
            m.button(slot("close",49),Items.BARRIER,"Close",List.of(),p::closeContainer);
            m.section(18,Items.STAINED_GLASS_PANE.pick(DyeColor.LIGHT_BLUE),"Kingdom management");m.section(27,Items.STAINED_GLASS_PANE.pick(DyeColor.RED),"War and powers");m.section(36,Items.STAINED_GLASS_PANE.pick(DyeColor.CYAN),"Activities and settings");
            m.button(slot("claims",20),item("claims",Items.GRASS_BLOCK),"Claims",List.of("Expand, inspect or release land"),()->claims(p));
            m.button(slot("treasury",22),item("treasury",Items.GOLD_INGOT),"Treasury",List.of("Deposit and withdraw with permission"),()->treasury(p));
            m.button(slot("members",24),item("members",Items.PLAYER_HEAD),"Members",List.of("Mayors and individual permissions"),()->members(p,0));
            m.button(slot("settings",42),item("settings",Items.COMPARATOR),"Settings",List.of("Kingdom flags and default permissions"),()->settings(p));
            m.button(slot("war",29),item("war",Items.IRON_SWORD),"War",List.of("Duration: "+s().config.warMinutes+" minutes","Both sides lose protection against each other.","All kingdom flags are overridden during war."),()->browse(p,0));
            m.button(slot("protection",31),item("protection",Items.SHIELD),"War protection",List.of("Price: "+s().money(s().config.protectionFee),"Duration: "+time(s().config.minute(s().config.protectionMinutes)),"Remaining: "+time(k.protectedUntil-s().now())),()->confirm(p,"Buy war protection",List.of("Cost: "+s().money(s().config.protectionFee),"Paid from the kingdom treasury."),()->s().protect(p)));
            m.button(slot("help",45),item("help",Items.BOOK),"Help",List.of("Commands and kingdom rules"),()->help(p));m.button(slot("leaderboard",51),item("leaderboard",Items.EMERALD),"Treasury leaderboard",List.of("Compare kingdom treasury balances"),()->leaderboard(p));m.button(slot("beacon",33),item("beacon",Items.BEACON),"Kingdom beacon",List.of("Buy 24-hour powers from the treasury.","Powers work only inside claimed chunks.","Multiple different powers can run together.","Active entries: "+s().beaconEffects(k.id).size()),()->beacon(p));AirDropUi.homeButton(p,m);m.button(m.actionSlot(40),Items.DIAMOND_PICKAXE,"Artifacts",List.of("Inspect and upgrade rare artifacts"),()->s().artifacts.menu(p));
        },false);
    }
    private static int slot(String key,int fallback){return s().config.menuSlots.getOrDefault(key,fallback);}
    public static void claims(ServerPlayer p){open(p,"claims",m->{Kingdom k=s().requireOwn(p.getUUID());String key=KingdomService.key(p.level(),p.blockPosition());Kingdom at=s().land(p.level(),p.blockPosition());m.button(13,Items.MAP,"Current chunk",List.of(key,"Owner: "+(at==null?"wilderness":at.name),"Claimed chunks: "+k.claims.size(),"Claim slots: "+s().claimSlots(k)+" / "+s().claimCap(k),"Outposts: "+k.outposts.size()+" / "+s().config.maxOutposts),null);m.button(20,Items.GRASS_BLOCK,"Claim this chunk",List.of("Treasury fee: "+s().money(s().config.expansionFee),s().config.adjacentExpansion?"Must touch existing land.":"Disconnected expansion is enabled."),()->{s().claim(p);claims(p);});m.button(22,Items.COMPASS,"Claim outpost",List.of("Treasury fee: "+s().money(s().config.outpostFee),"Uses "+s().config.outpostClaimWeight+" claim slots.","Does not need to connect to existing land."),()->confirm(p,"Claim outpost",List.of(key,"Cost: "+s().money(s().config.outpostFee),"Claim slots: "+s().config.outpostClaimWeight),()->s().outpost(p)));m.button(24,Items.REDSTONE_BLOCK,"Unclaim this chunk",List.of("No refund. This land becomes unprotected.",k.outposts.contains(key)?"This is an outpost anchor.":""),()->confirm(p,"Unclaim land",List.of(key,"Stand still: this releases your current chunk."),()->s().unclaim(p)));m.back(p);},false);}
    public static void treasury(ServerPlayer p){open(p,"treasury",m->{Kingdom k=s().requireOwn(p.getUUID());m.button(4,Items.GOLD_BLOCK,"Treasury: "+s().money(k.treasury),List.of("Your balance: "+s().money(s().api().balances().getBalance(p.getUUID())),"Daily cycle: "+s().money(s().config.upkeepBase+s().config.upkeepPerMember*k.members.size()),"Cycle length: "+s().config.upkeepIntervalMinutes+" minutes","Debt: "+s().money(k.debt),"Next cycle: "+time(k.nextUpkeep-s().now())),null);m.section(18,Items.STAINED_GLASS_PANE.pick(DyeColor.LIME),"Deposit");m.section(27,Items.STAINED_GLASS_PANE.pick(DyeColor.ORANGE),"Withdraw");for(int i=0;i<3;i++){long amount=s().config.treasuryPresets[i];m.button(20+i*2,Items.GOLD_INGOT,"Deposit "+s().money(amount),List.of(),()->{s().treasury(p,amount,false);treasury(p);});m.button(29+i*2,Items.IRON_INGOT,"Withdraw "+s().money(amount),List.of("Requires withdrawal permission"),()->confirm(p,"Withdraw treasury money",List.of(s().money(amount)),()->s().treasury(p,amount,true)));}m.button(40,Items.PAPER,"Custom amount",List.of("Enter any positive amount.","Choose deposit or withdraw in the dialog."),()->treasuryAmountDialog(p));m.back(p);},false);}
    public static void beacon(ServerPlayer p){open(p,"kingdom beacon",m->{Kingdom k=s().requireOwn(p.getUUID());War war=Rules.war(s().state,k.id,s().now());m.button(4,Items.BEACON,"Kingdom beacon",List.of("Treasury: "+s().money(k.treasury),"Duration: "+time(s().config.minute(s().config.beaconDurationMinutes)),"Works only inside claimed chunks.","Level II member powers cost "+s().config.beaconLevel2Multiplier+"x.","Mayor purchase required."),null);m.button(20,Items.EMERALD,"Member powers",List.of("Positive effects for kingdom members.","Active inside any claimed chunk.","Choose Level I or Level II.","Different effects can run together."),()->beaconPowers(p,true));m.button(24,Items.FERMENTED_SPIDER_EYE,"Opponent powers",List.of("Slowness, blindness and mining fatigue.","Active inside any claimed chunk.",war==null?"Requires active wartime.":"Current enemy: "+s().state.kingdoms.get(war.opponent(k.id)).display(),"Opponent powers are Level I."),()->beaconPowers(p,false));m.button(31,Items.CLOCK,"Active powers",List.of("View member powers, attacks and incoming effects.","Active entries: "+s().beaconEffects(k.id).size()),()->beaconActive(p));m.back(p);},false);}
    private static void beaconPowers(ServerPlayer p,boolean beneficial){open(p,beneficial?"member beacon powers":"opponent beacon powers",m->{Kingdom k=s().requireOwn(p.getUUID());War war=Rules.war(s().state,k.id,s().now());int[] slots=beneficial?new int[]{20,22,24,29,31,33}:new int[]{20,22,24};int index=0;for(BeaconPower power:BeaconPower.values())if(power.beneficial()==beneficial&&s().config.beaconEffectFees.containsKey(power.key())){long base=s().beaconCost(power,1);List<String> lore=new ArrayList<>();lore.add(KingdomBeacon.description(power));lore.add("Level I: "+s().money(base));if(beneficial)lore.add("Level II: "+s().money(s().beaconCost(power,2)));else lore.add(war==null?"Locked until wartime is active.":"Target: "+s().state.kingdoms.get(war.opponent(k.id)).display());BeaconPower chosen=power;m.button(slots[index++],KingdomBeacon.icon(power),KingdomBeacon.label(power),lore,war==null&&!beneficial?null:()->{if(beneficial)beaconLevels(p,chosen);else beaconConfirm(p,chosen,1);});}if(index==0)m.button(22,Items.BARRIER,beneficial?"No member powers enabled":"No opponent powers available",List.of(beneficial?"Enable prices in beaconEffectFees.":"Declare war and wait for wartime to begin."),null);m.button(49,Items.ARROW,"Back",List.of(),()->beacon(p));},false);}
    private static void beaconLevels(ServerPlayer p,BeaconPower power){open(p,KingdomBeacon.label(power),m->{m.button(4,KingdomBeacon.icon(power),KingdomBeacon.label(power),List.of(KingdomBeacon.description(power),"Applies to members inside claimed chunks.","Lasts "+time(s().config.minute(s().config.beaconDurationMinutes))+"."),null);m.button(20,Items.EMERALD,"Level I",List.of("Cost: "+s().money(s().beaconCost(power,1))),()->beaconConfirm(p,power,1));m.button(24,Items.DIAMOND,"Level II",List.of("Cost: "+s().money(s().beaconCost(power,2)),s().config.beaconLevel2Multiplier+"x the Level I price."),()->beaconConfirm(p,power,2));m.button(49,Items.ARROW,"Back",List.of(),()->beaconPowers(p,true));},false);}
    private static void beaconConfirm(ServerPlayer p,BeaconPower power,int level){long cost=s().beaconCost(power,level);open(p,"confirm beacon power",m->{Kingdom own=s().requireOwn(p.getUUID());Kingdom target=own;if(!power.beneficial()){War war=Rules.war(s().state,own.id,s().now());if(war!=null)target=s().state.kingdoms.get(war.opponent(own.id));}m.button(13,KingdomBeacon.icon(power),KingdomBeacon.label(power)+(level==2?" II":" I"),List.of(KingdomBeacon.description(power),"Target: "+(power.beneficial()?"all kingdom members":target==null?"no active enemy":target.display()),"Duration: "+time(s().config.minute(s().config.beaconDurationMinutes)),"Scope: any claimed chunk","Treasury cost: "+s().money(cost)),null);m.button(29,Items.BEACON,"Activate power",List.of("Spend "+s().money(cost)+" from the kingdom treasury."),()->{s().buyBeaconEffect(p,power,level);beacon(p);});m.button(33,Items.REDSTONE_BLOCK,"Cancel",List.of(),()->beacon(p));},false);}
    private static void beaconActive(ServerPlayer p){beaconActive(p,0);}
    private static void beaconActive(ServerPlayer p,int page){open(p,"active beacon powers",m->{Kingdom own=s().requireOwn(p.getUUID());List<BeaconEffect> effects=s().beaconEffects(own.id);int currentPage=MenuLayout.page(page,effects.size());for(int i=currentPage*MenuLayout.PAGE_SIZE;i<Math.min((currentPage+1)*MenuLayout.PAGE_SIZE,effects.size());i++){BeaconEffect effect=effects.get(i);Kingdom source=s().state.kingdoms.get(effect.sourceKingdom),target=s().state.kingdoms.get(effect.targetKingdom);String direction=!effect.hostile?"Members":effect.sourceKingdom.equals(own.id)?"Against "+target.display():"Incoming from "+source.display();m.button(MenuLayout.slot(i-currentPage*MenuLayout.PAGE_SIZE),KingdomBeacon.icon(effect.power),KingdomBeacon.label(effect.power)+(effect.level==2?" II":" I"),List.of(direction,"Remaining: "+time(effect.expiresAt-s().now())),null);}if(effects.isEmpty())m.button(22,Items.PAPER,"No active beacon powers",List.of("Purchased powers will appear here."),null);m.pages(p,currentPage,effects.size(),q->beaconActive(p,q));m.button(49,Items.ARROW,"Back",List.of(),()->beacon(p));},false);}
    public static void settings(ServerPlayer p){open(p,"kingdom settings",m->{
        Kingdom k=s().requireOwn(p.getUUID());
        m.button(4,Items.COMPARATOR,"Land flags",List.of("Choose which activities your kingdom allows.","Mayors can toggle flags; active war overrides them."),null);
        int index=0;for(Flag flag:Flag.values()){
            boolean on=k.flags.contains(flag);
            m.button(MenuLayout.TOGGLES[index++],on?Items.DYE.pick(DyeColor.LIME):Items.DYE.pick(DyeColor.RED),flag.name().replace('_',' ')+": "+(on?"enabled":"disabled"),List.of(flagDescription(flag),"Click to toggle. Mayor only.","Wartime overrides this setting."),()->{s().setFlag(p,flag,!on);settings(p);});
        }
        m.section(31,Items.STAINED_GLASS_PANE.pick(DyeColor.LIGHT_BLUE),"Permissions and identity");
        m.button(37,Items.PLAYER_HEAD,"Member defaults",List.of("Default access for kingdom members."),()->defaults(p,false));
        m.button(39,Items.OAK_DOOR,"Public permissions",List.of("Visitor access outside wartime."),()->defaults(p,true));
        m.button(41,KingdomIcons.stack(k),"Kingdom icon",List.of("Current: "+KingdomIcons.material(k),"Hold a non-block item and click to set it.","Blocks are not supported.","Mayor only; unavailable during preparation or wartime."),()->{s().setIcon(p);settings(p);});
        m.button(43,Items.NETHER_STAR,"Town center",List.of("Manage your core and captured cores.","Choose Change block, then right-click a block."),()->core(p));
        m.button(45,Items.TNT,"Disband kingdom",List.of("Permanent: every claim becomes unprotected.","Treasury money is forfeited.","A separate confirmation is required."),()->confirm(p,"Disband "+k.name,List.of("This releases every claim.","Remaining treasury is forfeited."),()->s().disband(p)));
        m.back(p);
    },false);}
    public static void core(ServerPlayer p){open(p,"town center",m->{
        Kingdom own=s().requireOwn(p.getUUID());Core core=own.core;
        Kingdom captor=core==null||core.tributeTo==null?null:s().state.kingdoms.get(core.tributeTo);
        List<String> details=new ArrayList<>();details.add(captor==null?"Independent":"Pays "+s().config.coreTaxPercent+"% tribute to "+captor.display());
        if(core==null)details.add("Place a breakable block at your founding chunk's center.");else{details.add("Block: "+core.block);details.add("Location: "+core.dimension+" "+core.x+", "+core.y+", "+core.z);details.add(core.carried?"Core in transit. Returns in "+time(core.takenAt+s().config.minute(s().config.coreReturnMinutes)-s().now()):"Right-click your recovered core item on your land for independence.");}
        m.button(4,Items.NETHER_STAR,"Your kingdom core",details,null);
        m.button(20,Items.COMPASS,"Change block",List.of("Mayor only; unavailable during war.","Right-click an existing breakable block in your kingdom.","Your own core must be at home."),()->s().cores.beginSelection(p));
        List<Kingdom> captives=heldCores(own);
        m.button(24,Items.GOLD_INGOT,"Captured kingdoms: "+captives.size(),List.of("Tribute: "+s().config.coreTaxPercent+"% of each incoming credit.","Sales, rewards, payments and withdrawals are taxed.","War protection cannot be bought while holding a foreign core.","Unplaced cores return after "+s().config.coreReturnMinutes+" minutes.","Click to manage captured core locations."),()->capturedCores(p,0));
        for(int i=0;i<Math.min(3,captives.size());i++){Kingdom k=captives.get(i);m.button(38+i*2,KingdomIcons.stack(k),k.display(),List.of(k.core.carried?"Core in transit":"Core secured",s().config.coreTaxPercent+"% tribute","Click to view or change its block."),()->capturedCore(p,k.id,0));}
        m.button(49,Items.ARROW,"Back",List.of(),()->settings(p));
    },false);}
    private static List<Kingdom> heldCores(Kingdom holder){return s().state.kingdoms.values().stream().filter(k->k.core!=null&&holder.id.equals(k.core.tributeTo)).sorted(Comparator.comparing(k->k.name,String.CASE_INSENSITIVE_ORDER)).toList();}
    public static void capturedCores(ServerPlayer p,int page){open(p,"captured cores",m->{
        List<Kingdom> all=heldCores(s().requireOwn(p.getUUID()));int currentPage=MenuLayout.page(page,all.size());
        for(int i=currentPage*MenuLayout.PAGE_SIZE;i<Math.min(all.size(),currentPage*MenuLayout.PAGE_SIZE+MenuLayout.PAGE_SIZE);i++){Kingdom k=all.get(i);Core core=k.core;m.button(MenuLayout.slot(i-currentPage*MenuLayout.PAGE_SIZE),KingdomIcons.stack(k),k.display(),List.of(core.carried?"Core in transit":"Core secured","Location: "+core.dimension+" "+core.x+", "+core.y+", "+core.z,"Click to view or change its block."),()->capturedCore(p,k.id,currentPage));}
        if(all.isEmpty())m.button(22,Items.PAPER,"No captured cores",List.of(),null);
        m.pages(p,currentPage,all.size(),q->capturedCores(p,q));m.button(49,Items.ARROW,"Back",List.of(),()->core(p));
    },false);}
    public static void capturedCore(ServerPlayer p,UUID ownerId,int page){open(p,"captured core",m->{
        Kingdom own=s().requireOwn(p.getUUID()),owner=s().state.kingdoms.get(ownerId);
        if(owner==null||owner.core==null||!own.id.equals(owner.core.tributeTo))m.button(22,Items.BARRIER,"Core no longer held",List.of("Your kingdom no longer holds this core."),null);
        else{Core core=owner.core;m.button(4,KingdomIcons.stack(owner),owner.display()+" core",List.of(core.carried?"Core in transit":"Core secured","Block: "+core.block,"Location: "+core.dimension+" "+core.x+", "+core.y+", "+core.z,"Tribute: "+s().config.coreTaxPercent+"%"),null);
            m.button(20,Items.COMPASS,"Change block",List.of("Mayor only; this core must be secured.","Both kingdoms must be outside war preparation and wartime.","Right-click an existing breakable block in your kingdom.","The original owner and tribute remain unchanged."),()->s().cores.beginSelection(p,ownerId));}
        m.button(49,Items.ARROW,"Back",List.of(),()->capturedCores(p,page));
    },false);}
    private static String flagDescription(Flag flag){return switch(flag){case AUTOMATION->"Allows hoppers, droppers, dispensers and similar machines.";case PISTONS->"Allows pistons to move blocks inside this kingdom.";case EXPLOSIONS->"Allows explosion damage when the actor also has permission.";case FIRE->"Allows ignition, fire spread and fire damage.";case FLUIDS->"Allows water and lava placement and flow.";case MOBS->"Allows unapproved mobs to remain in claimed land.";case MOB_GRIEFING->"Allows mob-caused block and environmental damage.";case PVP->"Allows player combat when territory permissions also allow it.";};}
    public static void defaults(ServerPlayer p,boolean publicAccess){open(p,publicAccess?"public permissions":"member permissions",m->{Kingdom k=s().requireOwn(p.getUUID());int i=0;for(Permission perm:Permission.values()){boolean on=(publicAccess?k.publicPermissions:k.memberPermissions).contains(perm);m.button(MenuLayout.TOGGLES[i++],on?Items.DYE.pick(DyeColor.LIME):Items.DYE.pick(DyeColor.RED),perm.name().replace('_',' ')+": "+(on?"enabled":"disabled"),List.of("Grants are additive; individual grants add access."),()->{s().setDefaultPermission(p,publicAccess,perm,!on);defaults(p,publicAccess);});}m.button(49,Items.ARROW,"Back",List.of(),()->settings(p));},false);}
    public static void members(ServerPlayer p,int page){open(p,"members",m->{Kingdom k=s().requireOwn(p.getUUID());List<UUID> people=new ArrayList<>(k.members.keySet());int currentPage=MenuLayout.page(page,people.size());for(int i=currentPage*MenuLayout.PAGE_SIZE;i<Math.min(people.size(),currentPage*MenuLayout.PAGE_SIZE+MenuLayout.PAGE_SIZE);i++){UUID id=people.get(i);m.button(MenuLayout.slot(i-currentPage*MenuLayout.PAGE_SIZE),Items.PLAYER_HEAD,k.members.get(id),List.of(k.mayor(id)?"Mayor":"Member","Click for permissions"),()->person(p,id,currentPage));}m.button(45,Items.WRITABLE_BOOK,"Invite a player",List.of("Enter an online player's name."),()->inviteDialog(p));m.pages(p,currentPage,people.size(),q->members(p,q));m.back(p);},false);}
    public static void person(ServerPlayer p,UUID target){person(p,target,0);}
    private static void person(ServerPlayer p,UUID target,int memberPage){open(p,"player permissions",m->{Kingdom k=s().requireOwn(p.getUUID());m.button(4,Items.PLAYER_HEAD,k.members.getOrDefault(target,"Player"),List.of(k.mayor(target)?"Mayor":"Member","Personal grants add to default access."),null);int i=0;for(Permission perm:Permission.values()){boolean on=k.grants.getOrDefault(target,Set.of()).contains(perm);m.button(MenuLayout.TOGGLES[i++],on?Items.DYE.pick(DyeColor.LIME):Items.DYE.pick(DyeColor.RED),perm.name().replace('_',' ')+": "+(on?"enabled":"disabled"),List.of("Personal grant. Member defaults still apply."),()->{s().grant(p,target,perm,!on);person(p,target,memberPage);});}boolean mayor=k.mayor(target);m.button(38,Items.GOLDEN_HELMET,mayor?"Remove mayor":"Appoint mayor",List.of("Mayors can manage everything and appoint mayors."),()->confirm(p,"Change mayor",List.of(target.toString()),()->s().setMayor(p,target,!mayor)));m.button(42,Items.BARRIER,"Remove member",List.of(),()->confirm(p,"Remove member",List.of(target.toString()),()->s().removeMember(p,target)));m.button(49,Items.ARROW,"Back",List.of(),()->members(p,memberPage));},false);}
    public static void leaderboard(ServerPlayer p){leaderboard(p,0);}
    private static void leaderboard(ServerPlayer p,int page){open(p,"treasury leaderboard",m->{
        List<Kingdom> all=s().leaderboard();int currentPage=MenuLayout.page(page,all.size());
        for(int i=currentPage*MenuLayout.PAGE_SIZE;i<Math.min(all.size(),(currentPage+1)*MenuLayout.PAGE_SIZE);i++){
            Kingdom k=all.get(i);
            String name=(s().config.leaderboardShowPosition?"#"+(i+1)+" ":"")+(s().config.leaderboardShowName?k.display():"Kingdom");
            List<String> lore=new ArrayList<>();
            if(s().config.leaderboardShowTreasury)lore.add("Treasury: "+s().money(k.treasury));
            lore.add("Members: "+k.members.size());
            lore.add("Claims: "+k.claims.size());
            if(s().config.leaderboardShowCenter){
                Core core=k.core;
                if(core==null)lore.add("Town center: Not set");
                else{
                    if(core.carried)lore.add("Town center: In transit");
                    lore.add((core.carried?"Last placed: ":"Town center: ")+"X "+core.x+", Y "+core.y+", Z "+core.z);
                    lore.add("Dimension: "+core.dimension.replace("minecraft:","").replace('_',' '));
                }
            }
            m.button(MenuLayout.slot(i-currentPage*MenuLayout.PAGE_SIZE),s().config.leaderboardShowIcon?KingdomIcons.stack(k):new ItemStack(Items.PAPER),name,lore,null);
        }
        if(all.isEmpty())m.button(22,Items.PAPER,"No kingdoms yet",List.of(),null);
        m.pages(p,currentPage,all.size(),q->leaderboard(p,q));m.back(p);
    },false);}
    public static void browse(ServerPlayer p,int page){open(p,"kingdom directory",m->{List<Kingdom> all=s().state.kingdoms.values().stream().sorted(Comparator.comparing(Kingdom::display,String.CASE_INSENSITIVE_ORDER)).toList();int currentPage=MenuLayout.page(page,all.size());for(int i=currentPage*MenuLayout.PAGE_SIZE;i<Math.min(all.size(),currentPage*MenuLayout.PAGE_SIZE+MenuLayout.PAGE_SIZE);i++){Kingdom k=all.get(i);m.button(MenuLayout.slot(i-currentPage*MenuLayout.PAGE_SIZE),KingdomIcons.stack(k),k.display(),List.of("Members: "+k.members.size(),"Claims: "+k.claims.size(),"Protection: "+time(k.protectedUntil-s().now()),"Cooldown: "+time(k.cooldownUntil-s().now()),"Click to review a war declaration."),()->confirm(p,"Declare war: "+k.name,List.of("Both kingdoms become vulnerable.","All flags are overridden for "+s().config.warMinutes+" minutes.","Logouts do not stop war.","Membership and claims are frozen."),()->s().declare(p,k.id)));}m.pages(p,currentPage,all.size(),q->browse(p,q));m.back(p);},false);}
    private static Component dialogText(String value){return Component.literal(caps(value)).withStyle(ChatFormatting.GOLD);}
    private static CommandTemplate commandTemplate(String command){return new CommandTemplate(ParsedTemplate.CODEC.parse(JsonOps.INSTANCE,new JsonPrimitive(command)).getOrThrow());}
    private static ActionButton dialogButton(String label,String tooltip,String command){return new ActionButton(new CommonButtonData(dialogText(label),Optional.of(dialogText(tooltip)),140),Optional.of(commandTemplate(command)));}
    private static ActionButton cancelButton(){return new ActionButton(new CommonButtonData(dialogText("Cancel"),140),Optional.empty());}
    private static void inputDialog(ServerPlayer p,String title,String description,String key,String label,int maxLength,List<ActionButton> actions){
        CommonDialogData common=new CommonDialogData(dialogText(title),Optional.empty(),true,false,DialogAction.CLOSE,List.of(new PlainMessage(dialogText(description),300)),List.of(new net.minecraft.server.dialog.Input(key,new TextInput(300,dialogText(label),true,"",maxLength,Optional.empty()))));
        p.closeContainer();
        p.openDialog(Holder.direct(new MultiActionDialog(common,actions,Optional.of(cancelButton()),actions.size())));
    }
    private static void kingdomNameDialog(ServerPlayer p){inputDialog(p,"Create kingdom","Enter a kingdom name. The creation fee and distance checks are shown before final confirmation.","name","Kingdom name",Math.max(3,s().config.maxNameLength),List.of(dialogButton("Continue","Review and confirm kingdom creation.","/kingdom create $(name)")));}
    private static void inviteDialog(ServerPlayer p){inputDialog(p,"Invite player","Enter the exact name of an online player.","player","Player name",16,List.of(dialogButton("Invite","Send this player a kingdom invitation.","/kingdom invite $(player)")));}
    private static void treasuryAmountDialog(ServerPlayer p){inputDialog(p,"Custom treasury amount","Enter an amount such as 1k, 1.5m or 2 billion.","amount","Amount",19,List.of(dialogButton("Deposit","Move money from your balance into the kingdom treasury.","/kingdom deposit $(amount)"),dialogButton("Withdraw","Review and confirm a treasury withdrawal.","/kingdom withdraw $(amount)")));}
    public static void help(ServerPlayer p){open(p,"kingdom help",m->{m.button(13,Items.BOOK,"Commands",List.of("/airdrop | /kingdom airdrops","/kingdom create <name>","/kingdom invite <player> | join | leave","/kingdom deposit <amount> | withdraw <amount>","/kingdom beacon","/kingdom grant <player/UUID> <permission> <true/false>","/kingdom mayor <player/UUID> <true/false>","/kingdom flag <flag> <true/false>","/kingdomadmin reload | set <key> <value>","Sneak-right-click a mob to allow it as mayor.","Approve mobs outside claims before bringing them in."),null);m.back(p);},false);}
    public static class Menu extends ChestMenu {
        private final ServerPlayer viewer;private final String title;private final Consumer<Menu> render;public final boolean warning;private final Map<Integer,Runnable> actions=new HashMap<>();private final Set<Integer> occupied=new HashSet<>();private boolean root;private int ticks;
        Menu(int id,Inventory inventory,ServerPlayer viewer,String title,Consumer<Menu> render,boolean warning){super(MenuType.GENERIC_9x6,id,inventory,new SimpleContainer(54),6);this.viewer=viewer;this.title=title;this.render=render;this.warning=warning;refresh();}
        void refresh(){
            actions.clear();occupied.clear();root=false;getContainer().clearContent();
            if(s().config.menuDecorations){
                ItemStack inside=decoration(item("background",Items.STAINED_GLASS_PANE.pick(DyeColor.GRAY))),frame=decoration(warning?item("warning_frame",Items.STAINED_GLASS_PANE.pick(DyeColor.RED)):item("frame",Items.STAINED_GLASS_PANE.pick(DyeColor.BLACK)));
                for(int slot=0;slot<54;slot++)getContainer().setItem(slot,(slot<9||slot>=45||slot%9==0||slot%9==8?frame:inside).copy());
            }
            button(4,warning?Items.BARRIER:Items.PAPER,title,List.of(),null);
            render.accept(this);
            if(!warning&&!root)button(53,Items.BARRIER,"Close",List.of("Return to the game."),viewer::closeContainer);
        }
        private static ItemStack decoration(Item item){ItemStack stack=new ItemStack(item);stack.set(DataComponents.CUSTOM_NAME,Component.literal(" "));return stack;}
        public boolean available(int slot){return slot>=0&&slot<54&&!occupied.contains(slot);}
        public int actionSlot(int preferred){if(available(preferred))return preferred;for(int i=MenuLayout.CONTENT.length-1;i>=0;i--)if(available(MenuLayout.CONTENT[i]))return MenuLayout.CONTENT[i];throw new IllegalArgumentException("No free menu slot for this action. Adjust menuSlots.");}
        void section(int slot,Item item,String name){if(available(slot))button(slot,item,name,List.of(),null);}
        public void button(int slot,Item item,String name,List<String> lore,Runnable action){button(slot,new ItemStack(item),name,lore,action);}
        public void button(int slot,ItemStack stack,String name,List<String> lore,Runnable action){stack=stack.copyWithCount(1);stack.set(DataComponents.CUSTOM_NAME,Component.literal(caps(s().config.message(name,name))).withStyle(color(s().config.primaryColor,ChatFormatting.GOLD)).withStyle(style->style.withItalic(false)));stack.set(DataComponents.LORE,new ItemLore(lore.stream().map(line->Component.literal(caps(s().config.message(line,line))).withStyle(ChatFormatting.GRAY).withStyle(style->style.withItalic(false))).map(c->(Component)c).toList()));getContainer().setItem(slot,stack);occupied.add(slot);actions.remove(slot);if(action!=null)actions.put(slot,action);}
        void back(ServerPlayer p){button(49,Items.ARROW,"Back",List.of(),()->home(p));}
        void pages(ServerPlayer p,int page,int total,Consumer<Integer> navigate){button(46,Items.PAPER,"Page "+(page+1)+" / "+Math.max(1,(total+MenuLayout.PAGE_SIZE-1)/MenuLayout.PAGE_SIZE),List.of(total+" entries"),null);if(page>0)button(48,Items.ARROW,"Previous",List.of(),()->navigate.accept(page-1));if((page+1)*MenuLayout.PAGE_SIZE<total)button(50,Items.ARROW,"Next",List.of(),()->navigate.accept(page+1));}
        @Override public boolean stillValid(Player p){return p==viewer;}
        @Override public ItemStack quickMoveStack(Player p,int slot){return ItemStack.EMPTY;}
        @Override public void clicked(int slot,int button,ContainerInput type,Player p){if(p!=viewer||type!=ContainerInput.PICKUP||button!=0)return;if(s().needsWarning(viewer)&&!warning){Ui.warning(viewer);return;}Runnable action=actions.get(slot);if(action!=null)run(viewer,action);broadcastFullState();}
        @Override public void broadcastChanges(){if(++ticks%s().config.menuRefreshTicks==0)run(viewer,this::refresh);super.broadcastChanges();}
    }
}

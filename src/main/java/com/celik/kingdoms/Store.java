package com.celik.kingdoms;

import com.google.gson.*;
import java.nio.file.*;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.*;
import static com.celik.kingdoms.Rules.*;

public final class Store {
    public static final Gson JSON = new GsonBuilder().setPrettyPrinting().registerTypeAdapterFactory(new ArtifactBalanceMigration()).create();
    public final Path dir;
    public Store(Path dir) { this.dir = dir; }
    public <T> T read(String name, Class<T> type, T fallback) {
        try { Path file=dir.resolve(name); if(!Files.exists(file)) return fallback; T value=JSON.fromJson(Files.readString(file),type); if(value==null) throw new IOException("Empty file"); return value; }
        catch(Exception e) { throw new IllegalStateException("Cannot safely load " + dir.resolve(name),e); }
    }
    public void write(String name, Object value) {
        try {
            Files.createDirectories(dir);
            Path dest=dir.resolve(name), tmp=dir.resolve(name+".tmp");
            byte[] bytes=JSON.toJson(value).getBytes(StandardCharsets.UTF_8);
            try(FileChannel ch=FileChannel.open(tmp,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING,StandardOpenOption.WRITE)) { ByteBuffer b=ByteBuffer.wrap(bytes); while(b.hasRemaining()) ch.write(b); ch.force(true); }
            Files.move(tmp,dest,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
        } catch(IOException e) { throw new IllegalStateException("Cannot safely save " + dir.resolve(name),e); }
    }
    public State copy(State state) { return JSON.fromJson(JSON.toJson(state),State.class); }
    public void validate(State s) {
        if(s.airDropMoneyReceipts==null)s.airDropMoneyReceipts=new HashSet<>();
        if(s.rejoinCooldowns==null)s.rejoinCooldowns=new HashMap<>();
        if(s.rejoinCooldowns.values().stream().anyMatch(m->m==null||m.values().stream().anyMatch(t->t==null||t<0)))throw new IllegalStateException("Invalid rejoin cooldown");
        if(s.beaconEffects==null)s.beaconEffects=new ArrayList<>();
        if(s.schemaVersion != 1 || s.kingdoms == null || s.wars == null || s.invitations == null || s.playerCooldowns == null) throw new IllegalStateException("Invalid kingdoms data schema");
        Set<UUID> members=new HashSet<>(); Set<String> claims=new HashSet<>(); Set<String> names=new HashSet<>(); Set<UUID> atWar=new HashSet<>();
        for(var entry:s.kingdoms.entrySet()) {
            Kingdom k=entry.getValue();
            if(k!=null){if(k.displayName==null||k.displayName.isBlank())k.displayName=k.name;if(k.iconItem==null||k.iconItem.isBlank())k.iconItem="minecraft:paper";if(k.outposts==null)k.outposts=new HashSet<>();}
            if(k==null || !entry.getKey().equals(k.id) || k.name==null || !names.add(k.name.toLowerCase(Locale.ROOT)) || k.mayors.isEmpty() || !k.members.keySet().containsAll(k.mayors) || k.treasury<0 || k.debt<0 || k.claims.isEmpty()) throw new IllegalStateException("Invalid kingdom record");
            if(k.highlightColor==-1)k.highlightColor=Rules.highlightColor(k.id);
            if(k.highlightColor<0||k.highlightColor>0xFFFFFF)throw new IllegalStateException("Invalid kingdom highlight color");
            if(!k.claims.containsAll(k.outposts))throw new IllegalStateException("Outpost anchor is not claimed");
            for(UUID p:k.members.keySet()) if(!members.add(p)) throw new IllegalStateException("Duplicate kingdom membership");
            for(String c:k.claims) if(!claims.add(c) || !c.matches("[^|]+\\|-?\\d+\\|-?\\d+")) throw new IllegalStateException("Invalid / overlapping claim");
        }
        for(War w:s.wars) {
            if(w.id==null)w.id=UUID.randomUUID();if(w.declaredAt==0)w.declaredAt=w.startedAt;if(w.announcedMilestones==null)w.announcedMilestones=new HashSet<>();
            if(!s.kingdoms.containsKey(w.first) || !s.kingdoms.containsKey(w.second) || !atWar.add(w.first) || !atWar.add(w.second) || w.endsAt<=w.startedAt || w.declaredAt>w.startedAt) throw new IllegalStateException("Invalid war record");
        }
        Set<String> beaconKeys=new HashSet<>();
        Set<String> corePositions=new HashSet<>();
        for(Kingdom k:s.kingdoms.values())if(k.core!=null){
            Core core=k.core;
            if(core.taxRemainders==null||core.taxRemainders.values().stream().anyMatch(v->v==null||v<0||v>=100))throw new IllegalStateException("Invalid core tax remainder");
            UUID host=core.tributeTo==null?k.id:core.tributeTo;
            if(core.nonce==null||core.dimension==null||core.block==null||core.tributeTo!=null&&(!s.kingdoms.containsKey(core.tributeTo)||core.tributeTo.equals(k.id))||core.carried&&(core.carriedByKingdom==null||core.takenAt<=0))throw new IllegalStateException("Invalid kingdom core");
            if(!core.carried&&(!s.kingdoms.get(host).claims.contains(core.claimKey())||!corePositions.add(core.dimension+"|"+core.x+"|"+core.y+"|"+core.z)))throw new IllegalStateException("Core outside holder's claim or duplicate core location");
        }
        for(BeaconEffect e:s.beaconEffects){
            if(e==null||e.id==null||e.sourceKingdom==null||e.targetKingdom==null||e.power==null||e.expiresAt<=0||e.level<1||e.level>2||(!e.power.beneficial()&&e.level!=1)||e.hostile==e.power.beneficial()||!s.kingdoms.containsKey(e.sourceKingdom)||!s.kingdoms.containsKey(e.targetKingdom)||(!e.hostile&&!e.sourceKingdom.equals(e.targetKingdom))||(e.hostile&&e.sourceKingdom.equals(e.targetKingdom))||!beaconKeys.add(e.sourceKingdom+"|"+e.targetKingdom+"|"+e.power))throw new IllegalStateException("Invalid kingdom beacon effect");
        }
    }
    public static class Journal {
        public UUID player;
        public long before, after;
        public State result;
        public String reason;
    }
    public void clearJournal() { try { Files.deleteIfExists(dir.resolve("transaction.json")); } catch(IOException e) { throw new IllegalStateException(e); } }
}

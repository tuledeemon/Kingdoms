package com.celik.kingdoms;

import com.flowpowered.math.vector.Vector2d;
import com.flowpowered.math.vector.Vector3d;
import de.bluecolored.bluemap.api.*;
import de.bluecolored.bluemap.api.markers.*;
import de.bluecolored.bluemap.api.math.Color;
import de.bluecolored.bluemap.api.math.Shape;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import static com.celik.kingdoms.Rules.*;

/** Reads Minecraft state on the server thread; builds and publishes through BlueMap's thread-safe API. */
final class BlueMapIntegration implements KingdomMaps {
    private static final String LAYER="kingdoms:territories";
    private final KingdomService service;
    private final Object lock=new Object();
    private final ExecutorService worker=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"kingdoms-bluemap");t.setDaemon(true);return t;});
    private final Map<BlueMapMap,MarkerSet> published=new IdentityHashMap<>();
    // Accessed only by the single worker, never by an asynchronous BlueMap lifecycle callback.
    private final Map<RegionKey,Cached> geometry=new HashMap<>();
    private BlueMapAPI api;
    private long generation;
    private volatile long lastFailure;
    private boolean stopped,refresh=true,busy;
    private int ticks;
    private BlueMapConfig previousConfig;
    private record RegionKey(UUID kingdom,String dimension){}
    private record Cached(Set<ClaimGeometry.Chunk> claims,List<ClaimGeometry.Region> regions){}
    private record CorePoint(int x,int y,int z){}
    private record View(UUID kingdom,String dimension,Set<ClaimGeometry.Chunk> claims,String label,String detail,int color,CorePoint core){}
    private record Target(BlueMapMap map,List<View> kingdoms){}
    private final Consumer<BlueMapAPI> enabled=this::enabled;
    private final Consumer<BlueMapAPI> disabled=this::disabled;

    BlueMapIntegration(KingdomService service){
        this.service=service;BlueMapAPI.onDisable(disabled);BlueMapAPI.onEnable(enabled);
        KingdomsMod.LOG.info("BlueMap kingdom territory integration registered (API {})",BlueMapAPI.getInstance().map(BlueMapAPI::getAPIVersion).orElse("waiting for BlueMap"));
    }
    private void enabled(BlueMapAPI value){synchronized(lock){if(stopped)return;api=value;generation++;refresh=true;}}
    private void disabled(BlueMapAPI value){synchronized(lock){if(api!=value)return;api=null;generation++;refresh=true;removePublished();}}
    private void removePublished(){published.forEach((map,set)->map.getMarkerSets().remove(LAYER,set));published.clear();}
    @Override public void tick(){
        BlueMapConfig config=service.config.blueMap;BlueMapAPI current;long revision;
        synchronized(lock){
            if(stopped)return;
            if(config!=previousConfig){previousConfig=config;generation++;refresh=true;}
            if(!config.enabled){if(!published.isEmpty())removePublished();return;}
            if(api==null)return;
            if((++ticks<config.updateIntervalSeconds*20&&!refresh)||busy)return;
            ticks=0;refresh=false;busy=true;current=api;revision=generation;
        }
        try{
            List<Target> targets=snapshot(current,config);
            worker.execute(()->{
                try{
                    Map<BlueMapMap,MarkerSet> markers=build(targets,config);
                    synchronized(lock){
                        if(stopped||api!=current||generation!=revision)return;
                        published.forEach((map,set)->{if(!markers.containsKey(map))map.getMarkerSets().remove(LAYER,set);});
                        markers.forEach((map,set)->map.getMarkerSets().put(LAYER,set));
                        published.clear();published.putAll(markers);
                    }
                    lastFailure=0;
                }catch(RuntimeException|LinkageError failure){failure(failure);}
                finally{synchronized(lock){busy=false;}}
            });
        }catch(RuntimeException|LinkageError failure){synchronized(lock){busy=false;}failure(failure);}
    }
    private void failure(Throwable failure){long now=System.currentTimeMillis();if(lastFailure==0||now-lastFailure>=120_000){lastFailure=now;KingdomsMod.LOG.warn("Could not update BlueMap territories; Kingdoms gameplay is unaffected",failure);}}
    private List<Target> snapshot(BlueMapAPI api,BlueMapConfig config){
        Map<String,List<View>> dimensions=new HashMap<>();
        Set<UUID> online=new HashSet<>();service.server.getPlayerList().getPlayers().forEach(p->online.add(p.getUUID()));
        long now=service.now();
        for(Kingdom kingdom:service.state.kingdoms.values()){
            int color=kingdom.highlightColor<0?Rules.highlightColor(kingdom.id):kingdom.highlightColor;
            String label=Ui.caps(kingdom.display()),detail=detail(kingdom,color,online,now,config);
            var claims=ClaimGeometry.dimensions(kingdom.claims);
            if(config.showCoreMarkers&&kingdom.core!=null&&!kingdom.core.carried)claims.computeIfAbsent(kingdom.core.dimension,k->new HashSet<>());
            for(var dimension:claims.entrySet()){
                Core core=kingdom.core;CorePoint point=config.showCoreMarkers&&core!=null&&!core.carried&&dimension.getKey().equals(core.dimension)?new CorePoint(core.x,core.y,core.z):null;
                dimensions.computeIfAbsent(dimension.getKey(),k->new ArrayList<>()).add(new View(kingdom.id,dimension.getKey(),Set.copyOf(dimension.getValue()),label,detail,color,point));
            }
        }
        List<Target> targets=new ArrayList<>();
        for(var level:service.server.getAllLevels()){
            // Resolve actual platform worlds, not map names such as "world" or "overworld".
            var world=api.getWorld(level).or(()->api.getWorld(level.dimension()));
            if(world.isEmpty())continue;
            List<View> views=List.copyOf(dimensions.getOrDefault(KingdomService.dimension(level),List.of()));
            for(BlueMapMap map:world.get().getMaps())if(config.mapIds.isEmpty()||config.mapIds.contains(map.getId()))targets.add(new Target(map,views));
        }
        return List.copyOf(targets);
    }
    private Map<BlueMapMap,MarkerSet> build(List<Target> targets,BlueMapConfig config){
        Map<BlueMapMap,MarkerSet> result=new IdentityHashMap<>();Set<RegionKey> live=new HashSet<>();
        for(Target target:targets){
            MarkerSet set=new MarkerSet(Ui.caps(config.layerLabel),true,config.defaultHidden);
            for(View view:target.kingdoms){
                if(Thread.currentThread().isInterrupted())return Map.of();
                RegionKey key=new RegionKey(view.kingdom,view.dimension);live.add(key);Cached cached=geometry.get(key);
                if(cached==null||!cached.claims.equals(view.claims)){cached=new Cached(view.claims,ClaimGeometry.regions(view.claims));geometry.put(key,cached);}
                int region=0;
                for(ClaimGeometry.Region outline:cached.regions){
                    ShapeMarker marker=new ShapeMarker(view.label,shape(outline.outline()),config.markerHeight);
                    outline.holes().forEach(hole->marker.getHoles().add(shape(hole)));
                    marker.setDepthTestEnabled(config.depthTest);marker.setLineWidth(config.lineWidth);
                    marker.setColors(new Color(view.color,config.borderOpacityPercent/100f),new Color(view.color,config.fillOpacityPercent/100f));
                    marker.setDetail(view.detail);marker.setMaxDistance(config.maxDistance);
                    set.getMarkers().put(view.kingdom+":region:"+region++,marker);
                }
                if(view.core!=null){
                    var core=view.core;POIMarker marker=new POIMarker(Ui.caps("core • ")+view.label,new Vector3d(core.x+.5,core.y+1,core.z+.5));
                    marker.setDetail(view.detail+row("core",core.x+", "+core.y+", "+core.z));marker.setMaxDistance(config.maxDistance);
                    set.getMarkers().put(view.kingdom+":core",marker);
                }
            }
            result.put(target.map,set);
        }
        geometry.keySet().retainAll(live);return result;
    }
    private static Shape shape(List<ClaimGeometry.Point> points){return new Shape(points.stream().map(p->new Vector2d(p.x()*16d,p.z()*16d)).toList());}
    private String detail(Kingdom kingdom,int color,Set<UUID> online,long now,BlueMapConfig config){
        StringBuilder html=new StringBuilder("<div style=\"min-width:220px;border-top:4px solid #").append(String.format(Locale.ROOT,"%06x",color&0xffffff)).append(";padding:10px\"><h3 style=\"margin:0 0 8px\">").append(escape(Ui.caps(kingdom.display()))).append("</h3>");
        if(config.showMayors)html.append(row("mayors",String.join(", ",kingdom.mayors.stream().map(id->kingdom.members.getOrDefault(id,"unknown")).sorted().toList())));
        if(config.showMemberCount)html.append(row("members",kingdom.members.size()+" • "+kingdom.members.keySet().stream().filter(online::contains).count()+" online"));
        if(config.showMemberNames)html.append(row("roster",String.join(", ",kingdom.members.values().stream().sorted().toList())));
        html.append(row("claims",kingdom.claims.size()+" chunks • "+kingdom.outposts.size()+" outposts"));
        if(config.showTreasury)html.append(row("treasury",service.money(kingdom.treasury)));
        if(config.showWarStatus){
            War war=Rules.conflict(service.state,kingdom.id,now);String status="peace";
            if(war!=null){String enemies=service.diplomacy.names(war.opponents(kingdom.id));status=(war.preparing(now)?"preparing • "+Ui.clock(war.startedAt-now):"war • "+Ui.clock(war.endsAt-now))+" • "+enemies;}
            html.append(row("status",status));
        }
        return html.append("</div>").toString();
    }
    private static String row(String name,String value){return "<div style=\"margin:4px 0\"><b>"+escape(Ui.caps(name))+"</b>: "+escape(Ui.caps(value))+"</div>";}
    static String escape(String text){return text.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");}
    @Override public void stop(){
        synchronized(lock){stopped=true;generation++;api=null;removePublished();}
        BlueMapAPI.unregisterListener(enabled);BlueMapAPI.unregisterListener(disabled);worker.shutdownNow();
    }
}

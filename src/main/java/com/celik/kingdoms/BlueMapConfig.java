package com.celik.kingdoms;

import java.util.HashSet;
import java.util.Set;

/** Optional web-map presentation. Existing configs receive these defaults on load. */
public final class BlueMapConfig {
    public boolean enabled = true;
    public int updateIntervalSeconds = 10;
    public String layerLabel = "kingdoms";
    public boolean defaultHidden = false;
    public boolean showCoreMarkers = true;
    public boolean showMayors = true;
    public boolean showMemberCount = true;
    public boolean showMemberNames = false;
    public boolean showTreasury = true;
    public boolean showWarStatus = true;
    public boolean depthTest = false;
    public int lineWidth = 3;
    public int borderOpacityPercent = 90;
    public int fillOpacityPercent = 18;
    public int markerHeight = 64;
    public int maxDistance = 100_000_000;
    /** Empty means every BlueMap map belonging to a loaded server dimension. */
    public Set<String> mapIds = new HashSet<>();

    public void validate() {
        if(updateIntervalSeconds<1||updateIntervalSeconds>3600)throw new IllegalArgumentException("BlueMap updateIntervalSeconds must be 1..3600");
        if(layerLabel==null||layerLabel.isBlank()||layerLabel.length()>80||mapIds==null||mapIds.stream().anyMatch(x->x==null||x.isBlank()))throw new IllegalArgumentException("Invalid BlueMap layer label or map IDs");
        if(lineWidth<0||lineWidth>20||borderOpacityPercent<0||borderOpacityPercent>100||fillOpacityPercent<0||fillOpacityPercent>100)throw new IllegalArgumentException("Invalid BlueMap border or fill settings");
        if(markerHeight<-2048||markerHeight>2048||maxDistance<1)throw new IllegalArgumentException("Invalid BlueMap marker height or viewing distance");
    }
}

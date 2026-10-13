package com.celik.kingdoms;

import java.util.*;

/** Shared geometry for six-row inventory menus; the outer frame and footer stay free. */
final class MenuLayout {
    static final int[] CONTENT={10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34,37,38,39,40,41,42,43};
    static final int[] TOGGLES={10,12,14,16,19,21,23,25};
    static final int PAGE_SIZE=CONTENT.length;
    static int page(int requested,int total){return Math.max(0,Math.min(requested,Math.max(0,(total-1)/PAGE_SIZE)));}
    static int slot(int index){return CONTENT[index];}
    static Map<String,Integer> home(){return new LinkedHashMap<>(Map.of("claims",20,"treasury",22,"members",24,"settings",42,"war",29,"protection",31,"help",45,"leaderboard",51,"beacon",33,"close",49));}
    static Map<String,Integer> legacy(){return Map.of("claims",10,"treasury",12,"members",14,"settings",16,"war",28,"protection",30,"help",32,"leaderboard",34,"beacon",36,"close",49);}
    private MenuLayout(){}
}

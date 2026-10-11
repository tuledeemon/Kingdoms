package com.celik.kingdoms;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.tree.RootCommandNode;

/** Filters client suggestions without changing the server's executable commands. */
public final class KingdomCommandVisibility {
    private KingdomCommandVisibility() {}

    public static <S> RootCommandNode<S> clientTree(RootCommandNode<S> original) {
        RootCommandNode<S> visible=new RootCommandNode<>();
        for(var child:original.getChildren()) {
            if(child.getName().equals("k"))continue;
            visible.addChild(child.getName().equals("kingdom")?child.createBuilder().build():child);
        }
        return visible;
    }

    public static Suggestions suggestions(String input,Suggestions original) {
        String command=input.startsWith("/")?input.substring(1):input;
        int end=0;while(end<command.length()&&!Character.isWhitespace(command.charAt(end)))end++;
        String root=command.substring(0,end);
        if(end<command.length()&&(root.equals("kingdom")||root.equals("k")))
            return new Suggestions(original.getRange(),java.util.List.of());
        if(end==command.length())
            return new Suggestions(original.getRange(),original.getList().stream().filter(s->!s.getText().equals("k")).toList());
        return original;
    }
}

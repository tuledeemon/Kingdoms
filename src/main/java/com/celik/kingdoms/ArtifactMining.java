package com.celik.kingdoms;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import java.util.*;

/** A mining square viewed from the struck face. Down-facing squares use the player's heading. */
final class ArtifactMining {
    static List<BlockPos> area(BlockPos origin,Direction face,Direction heading,boolean upgraded){
        Direction right=face.getAxis().isVertical()?heading.getClockWise():face.getCounterClockWise();
        Direction down=face.getAxis().isVertical()?heading.getOpposite():Direction.DOWN;
        List<BlockPos> result=new ArrayList<>();int min=upgraded?-1:0,max=1;
        for(int y=min;y<=max;y++)for(int x=min;x<=max;x++)if(x!=0||y!=0)result.add(origin.relative(right,x).relative(down,y));
        return result;
    }
    private ArtifactMining(){}
}

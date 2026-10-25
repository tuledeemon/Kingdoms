package com.celik.kingdoms;

import net.minecraft.core.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.piston.*;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

/** Validate a whole piston event before allowing its native, multi-step state changes. */
public final class PistonMoves {
    private record Move(Level level,Set<BlockPos> positions){}
    private static final ThreadLocal<Move> CURRENT=new ThreadLocal<>();
    public static boolean approved(Level level,BlockPos pos){Move move=CURRENT.get();return move!=null&&move.level==level&&move.positions.contains(pos);}
    public static Scope scope(Level level,Set<BlockPos> positions){return new Scope(new Move(level,Set.copyOf(positions)));}
    public static final class Scope implements AutoCloseable {
        private final Move previous;
        private Scope(Move move){previous=CURRENT.get();CURRENT.set(move);}
        public void close(){if(previous==null)CURRENT.remove();else CURRENT.set(previous);}
    }
    public static Set<BlockPos> prepare(Level level,BlockPos pos,BlockState state,int event){
        Direction direction=state.getValue(PistonBaseBlock.FACING);
        Set<BlockPos> positions=new HashSet<>(Set.of(pos.immutable(),pos.relative(direction)));
        if(event==0||event==1&&state.is(Blocks.STICKY_PISTON)){
            boolean extending=event==0;
            PistonStructureResolver resolver=new PistonStructureResolver(level,pos,direction,extending);
            if(resolver.resolve()){
                Direction motion=extending?direction:direction.getOpposite();
                for(BlockPos from:resolver.getToPush()){
                    BlockPos to=from.relative(motion);positions.add(from.immutable());positions.add(to);
                    if(!WarRestrictions.placement(level,to,level.getBlockState(to),level.getBlockState(from)))return null;
                }
                for(BlockPos from:resolver.getToDestroy())positions.add(from.immutable());
            }
        }
        // Retraction can also finish an extending piston two blocks in front.
        BlockPos two=pos.relative(direction,2);
        if(event!=0&&level.getBlockState(two).is(Blocks.MOVING_PISTON))positions.add(two);
        for(BlockPos target:positions)if(!Protection.piston(level,pos,target,event==0))return null;
        return positions;
    }
    private PistonMoves(){}
}

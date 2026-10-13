package com.celik.kingdoms;

import java.util.*;

/** Exact chunk outlines, without shared edges or fills over unclaimed holes. No world queries. */
final class ClaimGeometry {
    record Chunk(int x,int z) {}
    record Point(long x,long z) implements Comparable<Point> {
        public int compareTo(Point p){int order=Long.compare(x,p.x);return order==0?Long.compare(z,p.z):order;}
    }
    record Region(List<Point> outline,List<List<Point>> holes) {}

    static Map<String,Set<Chunk>> dimensions(Set<String> claims){
        Map<String,Set<Chunk>> dimensions=new TreeMap<>();
        for(String claim:claims){String[] parts=claim.split("\\|");if(parts.length!=3)throw new IllegalArgumentException("Invalid kingdom claim key");dimensions.computeIfAbsent(parts[0],k->new HashSet<>()).add(new Chunk(Integer.parseInt(parts[1]),Integer.parseInt(parts[2])));}
        return dimensions;
    }
    static List<Region> regions(Set<Chunk> chunks){
        // All directed edges keep claimed land on the same side. Remove shared edges first.
        TreeMap<Point,List<Point>> edges=new TreeMap<>();
        for(Chunk c:chunks){
            long x=c.x,z=c.z;Point a=new Point(x,z),b=new Point(x+1,z),d=new Point(x,z+1),e=new Point(x+1,z+1);
            if(!contains(chunks,x,z-1))edge(edges,a,b);
            if(!contains(chunks,x+1,z))edge(edges,b,e);
            if(!contains(chunks,x,z+1))edge(edges,e,d);
            if(!contains(chunks,x-1,z))edge(edges,d,a);
        }
        List<List<Point>> outers=new ArrayList<>(),holes=new ArrayList<>();
        int remaining=edges.values().stream().mapToInt(List::size).sum();
        while(!edges.isEmpty()){
            Point start=edges.firstKey(),current=start,previous=null;List<Point> ring=new ArrayList<>();
            do{
                ring.add(current);List<Point> choices=edges.get(current);
                if(choices==null||choices.isEmpty()||remaining--<=0)throw new IllegalStateException("Open kingdom claim boundary");
                Point next=choose(previous,current,choices);choices.remove(next);if(choices.isEmpty())edges.remove(current);
                previous=current;current=next;
            }while(!current.equals(start));
            ring=simplify(ring);if(area(ring)>0)outers.add(ring);else holes.add(ring);
        }
        List<List<List<Point>>> assigned=new ArrayList<>();for(var ignored:outers)assigned.add(new ArrayList<>());
        for(var hole:holes){
            Point a=hole.get(0),b=hole.get(1);double dx=b.x-a.x,dz=b.z-a.z,length=Math.hypot(dx,dz);
            // A point just inside the unclaimed side also handles holes touching an outer corner.
            double x=(a.x+b.x)/2d+dz/length*.125,z=(a.z+b.z)/2d-dx/length*.125;
            int owner=-1;double smallest=Double.POSITIVE_INFINITY;
            for(int i=0;i<outers.size();i++){var outer=outers.get(i);double size=area(outer);if(size<smallest&&inside(outer,x,z)){owner=i;smallest=size;}}
            if(owner<0)throw new IllegalStateException("Uncontained kingdom claim hole");assigned.get(owner).add(hole);
        }
        List<Region> result=new ArrayList<>();double covered=0;
        for(int i=0;i<outers.size();i++){covered+=area(outers.get(i));for(var hole:assigned.get(i))covered+=area(hole);result.add(new Region(outers.get(i),List.copyOf(assigned.get(i))));}
        if(covered!=chunks.size())throw new IllegalStateException("Kingdom map outline does not match claimed chunk area");
        return List.copyOf(result);
    }
    private static boolean contains(Set<Chunk> chunks,long x,long z){return x>=Integer.MIN_VALUE&&x<=Integer.MAX_VALUE&&z>=Integer.MIN_VALUE&&z<=Integer.MAX_VALUE&&chunks.contains(new Chunk((int)x,(int)z));}
    private static void edge(Map<Point,List<Point>> edges,Point from,Point to){edges.computeIfAbsent(from,k->new ArrayList<>(2)).add(to);}
    private static int direction(Point from,Point to){return to.x>from.x?0:to.z>from.z?1:to.x<from.x?2:3;}
    private static Point choose(Point previous,Point current,List<Point> choices){
        if(previous==null)return choices.stream().min(Comparator.comparingInt(p->direction(current,p))).orElseThrow();
        int incoming=direction(previous,current);
        // At diagonal contacts, turn around the same region rather than joining two territories.
        return choices.stream().min(Comparator.comparingInt(p->switch((direction(current,p)-incoming+4)%4){case 1->0;case 0->1;case 3->2;default->3;})).orElseThrow();
    }
    private static List<Point> simplify(List<Point> ring){
        List<Point> result=new ArrayList<>();for(int i=0;i<ring.size();i++){Point a=ring.get((i+ring.size()-1)%ring.size()),b=ring.get(i),c=ring.get((i+1)%ring.size());if(direction(a,b)!=direction(b,c))result.add(b);}return List.copyOf(result);
    }
    static double area(List<Point> ring){double area=0;for(int i=0;i<ring.size();i++){Point a=ring.get(i),b=ring.get((i+1)%ring.size());area+=(double)a.x*b.z-(double)b.x*a.z;}return area/2;}
    private static boolean inside(List<Point> ring,double x,double z){boolean inside=false;for(int i=0,j=ring.size()-1;i<ring.size();j=i++){Point a=ring.get(i),b=ring.get(j);if((a.z>z)!=(b.z>z)&&x<(b.x-a.x)*(z-a.z)/(b.z-a.z)+a.x)inside=!inside;}return inside;}
    private ClaimGeometry(){}
}

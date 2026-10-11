package com.celik.kingdoms;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Converted, bundled GLB geometry. Loading never contacts a model/texture service. */
final class ParachuteModel {
    record Part(String role, double[] offset, float[] scale, float[] rotation) {}
    private static final class Model { int schemaVersion; List<Part> parts; }
    private static final Set<String> ROLES=Set.of("primary","secondary","lining","seam","trim","rope");
    private static final List<Part> PARTS=load();
    static List<Part> parts(){return PARTS;}
    private static List<Part> load(){
        try(var stream=ParachuteModel.class.getResourceAsStream("/data/kingdoms/airdrop/parachute.json")){
            if(stream==null)throw new IllegalStateException("Missing bundled AirDrop parachute model");
            Model model=Store.JSON.fromJson(new InputStreamReader(stream,StandardCharsets.UTF_8),Model.class);
            if(model==null||model.schemaVersion!=1||model.parts==null||model.parts.isEmpty()||model.parts.size()>256)throw new IllegalStateException("Invalid parachute model");
            for(Part p:model.parts){
                if(p==null||!ROLES.contains(p.role)||p.offset==null||p.offset.length!=3||p.scale==null||p.scale.length!=3||p.rotation==null||p.rotation.length!=4)throw new IllegalStateException("Invalid parachute part");
                for(double value:p.offset)if(!Double.isFinite(value)||Math.abs(value)>32)throw new IllegalStateException("Invalid parachute offset");
                for(float value:p.scale)if(!Float.isFinite(value)||value<=0||value>32)throw new IllegalStateException("Invalid parachute scale");
                double norm=0;for(float value:p.rotation){if(!Float.isFinite(value))throw new IllegalStateException("Invalid parachute rotation");norm+=value*value;}
                if(Math.abs(norm-1)>.0001)throw new IllegalStateException("Unnormalized parachute rotation");
            }
            return List.copyOf(model.parts);
        }catch(java.io.IOException e){throw new IllegalStateException("Cannot load bundled parachute model",e);}
    }
    static Part scaled(Part part,float factor){
        if(factor==1)return part;
        double[] offset={.5+(part.offset[0]-.5)*factor,1+(part.offset[1]-1)*factor,.5+(part.offset[2]-.5)*factor};
        float[] scale={part.scale[0]*factor,part.scale[1]*factor,part.scale[2]*factor};
        if(!part.role.equals("rope"))return new Part(part.role,offset,scale,part.rotation);
        // Keep rope roots attached to the unscaled one-block crate.
        Quaternionf old=new Quaternionf(part.rotation[0],part.rotation[1],part.rotation[2],part.rotation[3]);
        Vector3f start=new Vector3f((float)part.offset[0],(float)part.offset[1],(float)part.offset[2]).add(old.transform(new Vector3f(part.scale[0]/2,0,part.scale[2]/2)));
        Vector3f end=new Vector3f(start).add(old.transform(new Vector3f(0,part.scale[1],0)));
        end.set(.5f+(end.x-.5f)*factor,1+(end.y-1)*factor,.5f+(end.z-.5f)*factor);
        Vector3f direction=new Vector3f(end).sub(start);scale[1]=direction.length();
        Quaternionf rotation=new Quaternionf().rotationTo(new Vector3f(0,1,0),direction.normalize());
        Vector3f corner=new Vector3f(start).sub(rotation.transform(new Vector3f(scale[0]/2,0,scale[2]/2)));
        return new Part(part.role,new double[]{corner.x,corner.y,corner.z},scale,new float[]{rotation.x,rotation.y,rotation.z,rotation.w});
    }
    static String block(Part part,AirDropConfig config){return switch(part.role){case "primary"->config.parachutePrimaryBlock;case "secondary","lining"->config.parachuteSecondaryBlock;case "seam"->config.parachuteSeamBlock;case "trim"->config.parachuteTrimBlock;case "rope"->config.parachuteRopeBlock;default->throw new IllegalArgumentException("Unknown parachute material role");};}
    private ParachuteModel(){}
}

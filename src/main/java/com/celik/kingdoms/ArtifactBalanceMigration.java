package com.celik.kingdoms;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
/** One-time catalog replacement, including identities inside pending transactions. */
final class ArtifactBalanceMigration implements TypeAdapterFactory {
    @Override public <T> TypeAdapter<T> create(Gson gson,TypeToken<T> type){
        boolean config=type.getRawType()==ArtifactConfig.class;
        if(!config&&type.getRawType()!=ArtifactRules.Record.class)return null;
        TypeAdapter<T> delegate=gson.getDelegateAdapter(this,type);
        return new TypeAdapter<T>(){
            @Override public void write(JsonWriter out,T value)throws IOException{delegate.write(out,value);}
            @Override public T read(JsonReader in)throws IOException{
                JsonElement tree=JsonParser.parseReader(in);
                if(tree.isJsonObject()){
                    JsonObject obj=tree.getAsJsonObject();
                    if(!obj.has("abilityBalanceVersion")||obj.get("abilityBalanceVersion").getAsInt()<2){
                        if(config)obj.add("types",gson.toJsonTree(ArtifactConfig.defaults()));
                        else{
                            String key=obj.get("type").getAsString();ArtifactConfig.Type replacement=ArtifactConfig.defaults().get(key);
                            if(replacement==null)obj.addProperty("retired",true);
                            else{
                                int oldMax=obj.has("maxUses")?obj.get("maxUses").getAsInt():0,oldUses=obj.has("uses")?obj.get("uses").getAsInt():oldMax;
                                obj.addProperty("legacyRemainingFraction",oldMax>0?(double)oldUses/oldMax:1);
                                obj.addProperty("name",replacement.name);obj.addProperty("rarity",replacement.rarity);obj.addProperty("baseItem",replacement.baseItem);
                                obj.addProperty("maxUses",replacement.maxUses);obj.addProperty("uses",replacement.maxUses);
                                obj.addProperty("upgradedMaxUses",replacement.upgradedMaxUses);obj.addProperty("upgradedValue",replacement.upgradedValue);obj.addProperty("upgradedDurationSeconds",replacement.upgradedDurationSeconds);
                                obj.addProperty("chance",1);obj.addProperty("value",replacement.variants.getFirst().value);obj.addProperty("durationSeconds",replacement.variants.getFirst().durationSeconds);
                                obj.addProperty("cooldownSeconds",0);obj.addProperty("readyAt",0);
                            }
                            obj.addProperty("revision",obj.has("revision")?Math.incrementExact(obj.get("revision").getAsInt()):1);
                        }
                        obj.addProperty("abilityBalanceVersion",2);
                    }
                    if(!obj.has("durabilityBalanceVersion")){
                        if(config){
                            JsonObject types=obj.has("types")?obj.getAsJsonObject("types"):null;
                            JsonObject scythe=types!=null&&types.has("void_scythe")?types.getAsJsonObject("void_scythe"):null;
                            if(scythe!=null&&scythe.has("maxUses")&&scythe.get("maxUses").getAsInt()==10)scythe.addProperty("maxUses",1000);
                        }else if(obj.has("type")&&obj.get("type").getAsString().equals("void_scythe")&&obj.has("maxUses")&&obj.get("maxUses").getAsInt()==10){
                            int uses=obj.has("uses")?obj.get("uses").getAsInt():10;
                            obj.addProperty("maxUses",1000);obj.addProperty("uses",uses*100);
                            obj.addProperty("revision",obj.has("revision")?Math.incrementExact(obj.get("revision").getAsInt()):1);
                        }
                        obj.addProperty("durabilityBalanceVersion",1);
                    }
                }
                return delegate.fromJsonTree(tree);
            }
        };
    }
}

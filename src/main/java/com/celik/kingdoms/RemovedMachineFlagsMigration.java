package com.celik.kingdoms;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.*;
import java.io.IOException;

/** Read existing configs, kingdoms and transaction snapshots without the removed toggles. */
final class RemovedMachineFlagsMigration implements TypeAdapterFactory {
    @Override public <T> TypeAdapter<T> create(Gson gson,TypeToken<T> type){
        String field=type.getRawType()==Config.class?"defaultFlags":type.getRawType()==Rules.Kingdom.class?"flags":null;
        if(field==null)return null;
        TypeAdapter<T> delegate=gson.getDelegateAdapter(this,type);
        return new TypeAdapter<T>(){
            public void write(JsonWriter out,T value)throws IOException{delegate.write(out,value);}
            public T read(JsonReader in)throws IOException{
                JsonElement tree=JsonParser.parseReader(in);
                if(tree.isJsonObject()&&tree.getAsJsonObject().get(field) instanceof JsonArray flags){
                    JsonArray kept=new JsonArray();
                    for(JsonElement flag:flags)if(!flag.isJsonPrimitive()||!flag.getAsJsonPrimitive().isString()||!java.util.Set.of("PISTONS","AUTOMATION","FLUIDS").contains(flag.getAsString()))kept.add(flag);
                    tree.getAsJsonObject().add(field,kept);
                }
                return delegate.fromJsonTree(tree);
            }
        };
    }
}

package com.example.bookcase;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.firebase.Timestamp;
import java.io.*;
import java.util.*;

/** Strict, bounded JSON with explicit value types, including nanosecond timestamps. */
public final class BackupJson {
    private BackupJson() {}
    private static final Gson GSON=new GsonBuilder().disableHtmlEscaping().serializeNulls().create();
    public static void write(Writer out, LibraryBackup backup) throws IOException {
        backup.validate(); JsonObject root=new JsonObject();
        root.addProperty("application","Bookcase"); root.addProperty("formatVersion",2);
        root.addProperty("exportedAtEpochMillis",backup.exportedAt);
        JsonObject collections=new JsonObject();
        for (String name:LibraryBackup.COLLECTIONS) {
            JsonArray docs=new JsonArray();
            for (Map.Entry<String,Map<String,Object>> entry:backup.collections.get(name).entrySet()) {
                JsonObject doc=new JsonObject(); doc.addProperty("id",entry.getKey()); doc.add("fields",encode(entry.getValue(),0)); docs.add(doc);
            }
            collections.add(name,docs);
        }
        root.add("collections",collections); JsonObject settings=new JsonObject();
        settings.add("names",GSON.toJsonTree(backup.cubeNames)); settings.addProperty("columns",backup.columns); root.add("cubeSettings",settings);
        String json=GSON.toJson(root); LibraryBackup.require(json.length()<20*1024*1024,"Backup exceeds the supported file size"); out.write(json); out.write('\n');
    }
    public static LibraryBackup read(Reader input) throws IOException {
        try {
            JsonReader reader=new JsonReader(new FilterReader(input) {
                int used;
                public int read(char[] b,int o,int n) throws IOException { int count=super.read(b,o,n); if(count>0 && (used+=count)>20*1024*1024) throw new IOException("Backup exceeds 20 MB"); return count; }
            });
            reader.setLenient(false); JsonObject root=parse(reader,0).getAsJsonObject();
            LibraryBackup.require(reader.peek()==JsonToken.END_DOCUMENT,"Unexpected content after backup");
            if (root.has("formatVersion") && root.get("formatVersion").getAsInt()==1)
                throw new IllegalArgumentException("This is an older catalogue export. Create a new full backup before restoring.");
            LibraryBackup.require(root.get("formatVersion").getAsBigDecimal().intValueExact()==2 && "Bookcase".equals(root.get("application").getAsString()), "Unsupported backup format");
            LibraryBackup backup=new LibraryBackup(); backup.exportedAt=root.get("exportedAtEpochMillis").getAsLong();
            JsonObject collections=root.getAsJsonObject("collections");
            LibraryBackup.require(collections.keySet().equals(new HashSet<>(LibraryBackup.COLLECTIONS)),"Missing or unsupported collections");
            int count=0;
            for (String name:LibraryBackup.COLLECTIONS) for (JsonElement element:collections.getAsJsonArray(name)) {
                LibraryBackup.require(++count<=10000,"Backup exceeds 10,000 records");
                JsonObject doc=element.getAsJsonObject(); String id=doc.get("id").getAsString();
                LibraryBackup.require(!backup.collections.get(name).containsKey(id),"Duplicate record ID");
                Object fields=decode(doc.get("fields"),0);
                LibraryBackup.require(fields instanceof Map,"Invalid record fields");
                backup.collections.get(name).put(id,(Map<String,Object>)fields);
            }
            JsonObject settings=root.getAsJsonObject("cubeSettings"); JsonArray names=settings.getAsJsonArray("names");
            LibraryBackup.require(names.size()==8,"Backup must contain eight cube names");
            for(int i=0;i<8;i++) backup.cubeNames[i]=names.get(i).getAsString();
            backup.columns=settings.get("columns").getAsBigDecimal().intValueExact(); backup.validate(); return backup;
        } catch (IllegalArgumentException error) { throw error; }
          catch (RuntimeException error) { throw new IllegalArgumentException("Invalid or incomplete backup file",error); }
    }
    private static JsonElement parse(JsonReader in,int depth) throws IOException {
        LibraryBackup.require(depth<=48,"Backup nesting is too deep");
        switch(in.peek()) {
            case BEGIN_OBJECT:
                JsonObject object=new JsonObject(); in.beginObject();
                while(in.hasNext()) { String key=in.nextName(); LibraryBackup.require(!object.has(key),"Duplicate JSON field"); object.add(key,parse(in,depth+1)); }
                in.endObject(); return object;
            case BEGIN_ARRAY:
                JsonArray array=new JsonArray(); in.beginArray(); while(in.hasNext()) { LibraryBackup.require(array.size()<10000,"Too many backup entries"); array.add(parse(in,depth+1)); } in.endArray(); return array;
            case STRING: return new JsonPrimitive(in.nextString());
            case NUMBER: return new JsonPrimitive(new java.math.BigDecimal(in.nextString()));
            case BOOLEAN: return new JsonPrimitive(in.nextBoolean());
            case NULL: in.nextNull(); return JsonNull.INSTANCE;
            default: throw new IllegalArgumentException("Invalid JSON value");
        }
    }
    static JsonElement encode(Object value,int depth) {
        LibraryBackup.require(depth<=20,"Record nesting is too deep"); JsonObject out=new JsonObject();
        if(value==null) out.addProperty("type","null");
        else if(value instanceof String) { out.addProperty("type","string"); out.addProperty("value",(String)value); }
        else if(value instanceof Boolean) { out.addProperty("type","boolean"); out.addProperty("value",(Boolean)value); }
        else if(value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long) { out.addProperty("type","integer"); out.addProperty("value",value.toString()); }
        else if(value instanceof Float || value instanceof Double) { double n=((Number)value).doubleValue(); LibraryBackup.require(Double.isFinite(n),"Unsupported non-finite number"); out.addProperty("type","double"); out.addProperty("value",n); }
        else if(value instanceof Timestamp) { Timestamp time=(Timestamp)value; LibraryBackup.require(time.getNanoseconds()%1000==0,"Date precision cannot be restored exactly"); out.addProperty("type","timestamp"); out.addProperty("seconds",Long.toString(time.getSeconds())); out.addProperty("nanoseconds",time.getNanoseconds()); }
        else if(value instanceof Map) { out.addProperty("type","map"); JsonObject map=new JsonObject(); for(Object key:new TreeSet<>(((Map<?,?>)value).keySet())) map.add((String)key,encode(((Map<?,?>)value).get(key),depth+1)); out.add("value",map); }
        else if(value instanceof List) { out.addProperty("type","list"); JsonArray list=new JsonArray(); for(Object item:(List<?>)value) list.add(encode(item,depth+1)); out.add("value",list); }
        else throw new IllegalArgumentException("Backup contains an unsupported field type; no complete backup was created");
        return out;
    }
    private static Object decode(JsonElement element,int depth) {
        LibraryBackup.require(depth<=20,"Record nesting is too deep"); JsonObject object=element.getAsJsonObject();
        String type=object.get("type").getAsString(); JsonElement value=object.get("value");
        switch(type) {
            case "null": return null;
            case "string": LibraryBackup.require(value.isJsonPrimitive() && value.getAsJsonPrimitive().isString(),"Invalid text"); return value.getAsString();
            case "boolean": LibraryBackup.require(value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean(),"Invalid boolean"); return value.getAsBoolean();
            case "integer": return Long.parseLong(value.getAsString());
            case "double": double number=value.getAsDouble(); LibraryBackup.require(Double.isFinite(number),"Invalid number"); return number;
            case "timestamp": int nanos=object.get("nanoseconds").getAsBigDecimal().intValueExact(); LibraryBackup.require(nanos%1000==0,"Date precision cannot be restored exactly"); return new Timestamp(Long.parseLong(object.get("seconds").getAsString()),nanos);
            case "map": Map<String,Object> map=new TreeMap<>(); for(Map.Entry<String,JsonElement> entry:value.getAsJsonObject().entrySet()) { LibraryBackup.require(!entry.getKey().isEmpty() && !entry.getKey().matches("__.*__"),"Invalid field name"); map.put(entry.getKey(),decode(entry.getValue(),depth+1)); } return map;
            case "list": List<Object> list=new ArrayList<>(); for(JsonElement item:value.getAsJsonArray()) list.add(decode(item,depth+1)); return list;
            default: throw new IllegalArgumentException("Unsupported field type");
        }
    }
    static boolean same(Map<String,Object> a,Map<String,Object> b) { return a==null ? b==null : b!=null && encode(a,0).equals(encode(b,0)); }
    static String fingerprint(LibraryBackup backup) {
        StringBuilder value=new StringBuilder();
        for(String name:LibraryBackup.COLLECTIONS) for(Map.Entry<String,Map<String,Object>> doc:backup.collections.get(name).entrySet()) value.append(name).append('/').append(doc.getKey()).append(':').append(encode(doc.getValue(),0)).append('\n');
        return CatalogueKeys.hash(value.toString());
    }
}

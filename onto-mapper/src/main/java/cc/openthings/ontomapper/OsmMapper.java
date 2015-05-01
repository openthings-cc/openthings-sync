package cc.openthings.ontomapper;

import cc.openthings.ontomapper.model.OsmMap;
import cc.openthings.ontomapper.model.OsmMapElement;
import org.djodjo.json.JsonArray;
import org.djodjo.json.JsonElement;
import org.djodjo.json.JsonObject;
import org.openstreetmap.osmosis.core.domain.v0_6.*;

import java.io.IOException;
import java.util.*;

public class OsmMapper extends Mapper {

    public static final String osmMapJson = "osmmap.json";


    HashMap<String, String> unmappedTags = new HashMap<>();


    private OsmMap osmMap = new OsmMap();
    //main osm kes aka main OpenThings types
    //*value is null
    private HashMap<String, JsonObject> mainOsmKeys = new HashMap<>();
    //known fixed mappings for osm tags k:v vs oth @type:@value
    //*k:v are strictly defined
    private HashMap<String, JsonObject> fixedOsmKV = new HashMap<>();

    //known keys where values can ve multiple but mapped in predictable pattern
    //*regex=true
    //v is regex and match es are substituted into @value and/or @language if available

    //map key is the tag key
    private HashMap<String, JsonObject> regexKeys = new HashMap<>();

    public OsmMapper() {
        super(osmMapJson);
        osmMap.wrap(map);

        for (OsmMapElement el : osmMap) {
            if (el.isMain()) {
                mainOsmKeys.put(el.getTagKey(), new JsonObject().put("@type", el.getType()).put("@value", el.getValue()));
            } else if (el.isRegex()) {
                regexKeys.put(el.getTagKey(),
                        new JsonObject()
                                .put("k", el.getTagKey())
                                .put("v", el.getTagValue())
                                .put("@type", el.getType())
                                .put("@value", el.getValue())
                                .put("@language", el.getLanguage())
                );
            } else {
                fixedOsmKV.put(getTagId(el.getTagKey(), el.getTagValue()),
                        new JsonObject()
                                .put("k", el.getTagKey())
                                .put("v", el.getTagValue())
                                .put("@type", el.getType())
                                .put("@value", el.getValue())
                                .put("@language", el.getLanguage())
                );
            }

        }
    }

    public OsmMap getOsmMap() {
        return osmMap;
    }

    private static String getTagId(String tagKey, String tagValue) {
        return "k:" + tagKey + " :-: " + "v:" + tagValue;
    }

    @Override
    public Object getThing(Object otherThing) {
        //accepts Entity records which should include list of tags (key:value)
        //either in json or in osm v6 Entity
        JsonObject thing = new JsonObject();

        if (otherThing instanceof String || otherThing instanceof JsonObject) {
            String otherString = otherThing.toString();
            if (!otherString.trim().startsWith("{")) {
                throw new RuntimeException("Cannot convert from non json object string yet");
            }
            JsonObject element = null;
            try {
                element = JsonElement.readFrom(otherString).asJsonObject();
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
            if(!element.has("type") || !element.has("tags")) {
                return null;
            }
            String osmType = element.getString("type");
            JsonObject tags = element.getJsonObject("tags");
            if (osmType.equals("node")) {
                thing.put("loc", new JsonArray().put(element.get("lat")).put(element.get("lon")));


                JsonArray type = new JsonArray();
                thing.put("@type", type);
                type.put("http://openthings.cc/ontology/GeoThing");
                //get maintypes


                Iterator<Map.Entry<String, JsonElement>> tagIterator = tags.iterator();
                while (tagIterator.hasNext()) {
                    Map.Entry<String, JsonElement> tag = tagIterator.next();
                    processMainTypes(type, tag.getKey());
                }

                //getother types or props
                HashMap<String, String> otherTags = new HashMap<>();

                tagIterator = tags.iterator();
                while (tagIterator.hasNext()) {
                    Map.Entry<String, JsonElement> tag = tagIterator.next();
                    if (!processOtherTags(thing, type, tag.getKey(), tag.getValue().toString())) {
                        otherTags.put(tag.getKey(), tag.getValue().toString());
                    }
                }


                //get regex mappings
                for (Map.Entry<String, String> tag : otherTags.entrySet()) {
                    //  System.out.println("tag-->> " + tag);
                    processRegexTags(thing, tag.getKey(), tag.getValue());
                }

                thing.put("osm:id", element.get("id"));


            } else if (osmType.equals("way")) {

            } else if (osmType.equals("relation")) {

            }


        } else if (otherThing instanceof Entity) {
            Entity entity = (Entity) otherThing;
            if (entity instanceof Node) {
                Node node = (Node) entity;


                thing.put("loc", new JsonArray().put(node.getLatitude()).put(node.getLongitude()));

                JsonArray type = new JsonArray();
                thing.put("@type", type);
                type.put("http://openthings.cc/ontology/GeoThing");
                //get maintypes
                Collection<Tag> tags = node.getTags();
                for (Tag tag : tags) {
                    processMainTypes(type, tag.getKey());
                }

                //getother types or props
                Collection<Tag> otherTags = new ArrayList<>();
                for (Tag tag : tags) {
                    if (!processOtherTags(thing, type, tag.getKey(), tag.getValue())) {
                        otherTags.add(tag);
                    }
                }

                //get regex mappings
                for (Tag tag : otherTags) {
                    //  System.out.println("tag-->> " + tag);
                    processRegexTags(thing, tag.getKey(), tag.getValue());
                }

                thing.put("osm:id", node.getId())
                        .put("osm:timestamp", node.getTimestamp());

                //OSM meta data
//                thing.put("osmMeta", new JsonObject()
//                                .put("osmId", node.getId())
//                                .put("osmUsername", node.getUser().getName())
//                                .put("osmChangeset", node.getChangesetId())
//                                .put("osmTimestamp", node.getTimestamp())
//                                .put("osmVer", node.getVersion())
//                );
            } else if (entity instanceof Way) {
                //TODO
            } else if (entity instanceof Relation) {
                //TODO
            }

        }


        // System.out.println("-->> " + thing);
        return thing;
    }

    @Override
    public Object getOtherThing(Object thing) {
        throw new RuntimeException("TODO or not TODO");
    }


    public HashMap<String, String> getUnmappedTags() {
        return unmappedTags;
    }


    private void processMainTypes(JsonArray type, String tagKey) {
        tagKey = tagKey.toLowerCase();
        if (mainOsmKeys.containsKey(tagKey)) {
            type.addAll(mainOsmKeys.get(tagKey).getJsonArray("@value"));
        }
    }

    //returns if processed or not
    private boolean processOtherTags(JsonObject thing, JsonArray type, String tagKey, String tagValue) {
        tagKey = tagKey.toLowerCase();
        tagValue = tagValue.toLowerCase();
        if(tagValue.contains(";")) {
           boolean res =true;
            String[] tagValues = tagValue.split(";");
            for(String singleTagValue:tagValues) {
                if(!processOtherTags(thing, type, tagKey, singleTagValue)) {
                    res = false;
                }
            }
            return res;
        } else {
            String tagId = getTagId(tagKey, tagValue);
            if (fixedOsmKV.containsKey(tagId)) {
                JsonObject mapping = fixedOsmKV.get(tagId);
                String prop = mapping.getString("@type");
                JsonArray values = mapping.getJsonArray("@value");
                if (prop.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")) {
                    type.addAll(values);
                } else {
                    for (JsonElement je : values) {
                        JsonObject propToPut = new JsonObject().put("@value",
                                je.asString());

                        if (mapping.optString("@language") != null) {
                            propToPut.put("@language", mapping.optString("@language"));
                        }
                        if (!thing.has(prop)) {
                            thing.put(prop, new JsonArray().put(propToPut));
                        } else {
                            thing.getJsonArray(prop).put(propToPut);
                        }
                    }
                }
                return true;
            } else {
                return false;
            }
        }
    }

    private void processRegexTags(JsonObject thing, String tagKey, String tagValue) {
        tagKey = tagKey.toLowerCase();
        tagValue = tagValue.toLowerCase();
        if(tagValue.contains(";")) {
            String[] tagValues = tagValue.split(";");
            for(String singleTagValue:tagValues) {
                processRegexTags(thing, tagKey, singleTagValue);
            }
        } else {
            if (regexKeys.containsKey(tagKey)) {
                JsonObject mapping = regexKeys.get(tagKey);
                String prop = mapping.getString("@type");
                //For now we assume we don't have 1-to-many for regex mappings as those are not types
                String value = tagValue.replaceAll(mapping.getString("v"), mapping.getJsonArray("@value").getString(0));
                JsonObject propToPut = new JsonObject().put("@value", value);
                if (mapping.optString("@language") != null) {
                    String language = tagValue.replaceAll(mapping.getString("v"), mapping.getString("@language"));
                    propToPut.put("@language", language);
                }
                if (!thing.has(prop)) {
                    thing.put(prop, new JsonArray().put(propToPut));
                } else {
                    thing.getJsonArray(prop).put(propToPut);
                }
            } else {
                unmappedTags.put(tagKey, tagValue);
            }
        }
    }


}

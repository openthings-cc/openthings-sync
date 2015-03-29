package cc.openthings.ontomapper;

import cc.openthings.ontomapper.model.OsmMap;
import cc.openthings.ontomapper.model.OsmMapElement;
import org.djodjo.json.JsonArray;
import org.djodjo.json.JsonElement;
import org.djodjo.json.JsonObject;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.*;
import org.openstreetmap.osmosis.core.task.v0_6.RunnableSource;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.openstreetmap.osmosis.xml.common.CompressionMethod;
import org.openstreetmap.osmosis.xml.v0_6.XmlReader;

import java.io.*;
import java.util.*;

public class OsmMapper extends Mapper {

    public static final String osmMapJson = "osmmap.json";
    static Set<Tag> unmappedTags = new HashSet<>();

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
                fixedOsmKV.put(new Tag(el.getTagKey(), el.getTagValue()).toString(),
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


    @Override
    public Object getThing(Object otherThing) {
        //accepts Entity records which should include list of tags (key:value)
        //either in json or in osm v6 Entity
        JsonObject thing = new JsonObject();

        if (otherThing instanceof String || otherThing instanceof JsonObject) {
            String otherString = otherThing.toString();
            if (!otherString.trim().startsWith("{")) {
                throw new RuntimeException("Cannot convert from non json yet");
            }
            JsonObject elTags = JsonElement.wrap(otherString).asJsonObject().getJsonObject("tags");

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
                    if (mainOsmKeys.containsKey(tag.getKey())) {
                        type.put(mainOsmKeys.get(tag.getKey()).getString("@value"));
                    }
                }

                //getother types or props
                Collection<Tag> otherTags = new ArrayList<>();
                for (Tag tag : tags) {

                    if (fixedOsmKV.containsKey(tag.toString())) {
                        JsonObject mapping = fixedOsmKV.get(tag.toString());
                        String prop = mapping.getString("@type");
                        if (prop.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")) {
                            type.put(mapping.getString("@value"));
                        } else {
                            JsonObject propToPut = new JsonObject().put("@value",
                                    mapping.getString("@value"));
                            if(mapping.optString("@language")!=null) {
                                propToPut.put("@language",mapping.optString("@language"));
                            }
                            if (!thing.has(prop)) {
                                thing.put(prop, new JsonArray().put(propToPut));
                            } else {
                                thing.getJsonArray(prop).put(propToPut);
                            }
                        }
                    } else {
                        otherTags.add(tag);
                    }
                }


                //get regex mappings
                for (Tag tag : otherTags) {
                   //  System.out.println("tag-->> " + tag);
                    if(regexKeys.containsKey(tag.getKey())) {
                        JsonObject mapping = regexKeys.get(tag.getKey());
                        String prop = mapping.getString("@type");
                        String value = tag.getValue().replaceAll(mapping.getString("v"), mapping.getString("@value"));
                        JsonObject propToPut = new JsonObject().put("@value", value);
                        if(mapping.optString("@language")!=null) {
                            String language = tag.getValue().replaceAll(mapping.getString("v"), mapping.getString("@language"));
                            propToPut.put("@language",language);
                        }
                        if (!thing.has(prop)) {
                            thing.put(prop, new JsonArray().put(propToPut));
                        } else {
                            thing.getJsonArray(prop).put(propToPut);
                        }
                    }
                    else {
                        unmappedTags.add(tag);
                    }
                }


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


    //todo move
    public void readOsm(final File file) throws FileNotFoundException {
        Sink sinkImplementation = new Sink() {
            volatile int cntN = 0;
            volatile int cntW = 0;
            volatile int cntR = 0;
            JsonArray nodes = new JsonArray();

            @Override
            public void initialize(Map<String, Object> map) {

            }

            public void process(EntityContainer entityContainer) {
                Entity entity = entityContainer.getEntity();
                if (entity instanceof Node) {
                    cntN++;
                    if(entity.getTags()!=null && entity.getTags().size()>0) {
                        nodes.put(getThing(entity));
                    }
                    //System.out.println("NODE-->> " + entity);
                } else if (entity instanceof Way) {
                    cntW++;
                    //System.out.println("WAY-->> " + entity);
                } else if (entity instanceof Relation) {
                    cntR++;
                    //System.out.println("REL-->> " + entity);

                }


            }

            public void release() {
            }

            public void complete() {
                System.out.println("NODE-->> " + cntN);
                System.out.println("WAY-->> " + cntW);
                System.out.println("REL-->> " + cntR);
                System.out.println("UNMAPPED-->> " + unmappedTags);
                try {
                    File outJson = new File(file.getPath() + ".json");
                    Writer wr = new FileWriter(outJson.getPath());
                    System.out.println("writing to -->> " + outJson.getPath());
                    nodes.writeTo(wr);
                    wr.flush();
                    wr.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }
        };

        boolean pbf = false;
        CompressionMethod compression = CompressionMethod.None;

        if (file.getName().endsWith(".pbf")) {
            pbf = true;
        } else if (file.getName().endsWith(".gz")) {
            compression = CompressionMethod.GZip;
        } else if (file.getName().endsWith(".bz2")) {
            compression = CompressionMethod.BZip2;
        }

        RunnableSource reader;

        if (pbf) {
            reader = new crosby.binary.osmosis.OsmosisReader(
                    new FileInputStream(file));
        } else {
            reader = new XmlReader(file, false, compression);
        }

        reader.setSink(sinkImplementation);

        Thread readerThread = new Thread(reader);
        readerThread.start();

        while (readerThread.isAlive()) {
            try {
                readerThread.join();
            } catch (InterruptedException e) {
        /* do nothing */
            }
        }

    }

}

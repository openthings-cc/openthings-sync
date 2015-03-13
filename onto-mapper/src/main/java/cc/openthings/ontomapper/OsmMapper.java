package cc.openthings.ontomapper;

import cc.openthings.ontomapper.model.OsmMap;
import cc.openthings.ontomapper.model.OsmMapElement;
import org.djodjo.json.JsonObject;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.task.v0_6.RunnableSource;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.openstreetmap.osmosis.xml.common.CompressionMethod;
import org.openstreetmap.osmosis.xml.v0_6.XmlReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

public class OsmMapper extends Mapper {

    public static final String osmMapJson = "osmmap.json";

    private OsmMap osmMap = new OsmMap();
    //main osm kes aka main OpenThings types
    //*value is null
    private HashMap<String, JsonObject> mainOsmKeys = new HashMap<>();
    //known fixed mappings for osm tags k:v vs oth @type:@value
    //*k:v are strictly defined
    private HashMap<Map.Entry<String, String>, JsonObject> fixedOsmKV = new HashMap<>();

    //known keys where values can ve multiple but mapped in predictable pattern
    //*regex=true
    //v is regex and match es are substituted into @value and/or @language if available

    private HashMap<Map.Entry<String, String>, JsonObject> regexKeys = new HashMap<>();
    
    public OsmMapper() {
        super(osmMapJson);
        osmMap.wrap(map);

        for (OsmMapElement el : osmMap) {
            if (el.isMain()) {
                mainOsmKeys.put(el.getTagKey(), new JsonObject().put("@type", el.getType()).put("@value", el.getValue()));
            } else if (el.isRegex()) {
                regexKeys.put(new AbstractMap.SimpleEntry(el.getTagKey(), el.getTagValue()), new JsonObject().put("@type", el.getType()).put("@value", el.getValue()).put("@language", el.getLanguage()));
            } else {
                fixedOsmKV.put(new AbstractMap.SimpleEntry(el.getTagKey(), el.getTagValue()), new JsonObject().put("@type", el.getType()).put("@value", el.getValue()).put("@language", el.getLanguage()));
            }

        }
    }


    @Override
    public Object getThing(Object otherThing) {
        String otherString = otherThing.toString();
        if(!otherString.trim().startsWith("{")) {
            throw new RuntimeException("Cannot convert from non json yet");
        }

        JsonObject thing =  new JsonObject();
        
        
        
        return thing;
    }

    @Override
    public Object getOtherThing(Object thing) {
        return null;
    }
    
    
    //todo move
    public void readOsm(File file) throws FileNotFoundException {
        Sink sinkImplementation = new Sink() {
            @Override
            public void initialize(Map<String, Object> map) {

            }

            public void process(EntityContainer entityContainer) {
                Entity entity = entityContainer.getEntity();
                if (entity instanceof Node) {
                    //do something with the node
                } else if (entity instanceof Way) {
                    //do something with the way
                } else if (entity instanceof Relation) {
                    //do something with the relation
                }
            }
            public void release() { }
            public void complete() { }
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

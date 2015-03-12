package cc.openthings.ontomapper;

import cc.openthings.ontomapper.model.OsmMap;
import cc.openthings.ontomapper.model.OsmMapElement;
import org.djodjo.json.JsonObject;

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
}

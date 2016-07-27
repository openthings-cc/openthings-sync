package cc.openthings.sync;


import cc.openthings.ontomapper.OsmMapper;
import cc.openthings.ontomapper.model.OsmMap;
import cc.openthings.ontomapper.model.OsmMapElement;
import io.apptik.json.JsonArray;
import io.apptik.json.JsonElement;
import io.apptik.json.JsonObject;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Resource;

import java.util.*;

import static cc.openthings.sync.Common.getOntModel;

public class AssetsGenerator {

    OntModel model;
    private int maxLevel = 2;
    private static final int ALL_TYPES = 0;
    private static final int NODE  = 1;
    private static final int ROUTE  = 2;


    public AssetsGenerator() {
        model = getOpenThingsModel();
    }

    public static void main(String[] args) {
        AssetsGenerator assetsGenerator = new AssetsGenerator();
        //assetsGenerator.analyseTypes();
        //assetsGenerator.genFullTypesMenu();
        //assetsGenerator.genNodeTypesMenu();
        assetsGenerator.genRouteTypesMenu();
        //assetsGenerator.genType_KeyValueMap();

    }

    private void genFullTypesMenu() {
        ontoClassesToTypeTree(model, ALL_TYPES);
    }

    private void genNodeTypesMenu() {
        ontoClassesToTypeTree(model, NODE);
    }

    private void genRouteTypesMenu() {
        ontoClassesToTypeTree(model, ROUTE);
    }

    private void analyseTypes() {
        Common.ontoClassesToJson(model);
    }

    public OntModel getOpenThingsModel() {

        return getOntModel(Common.openThingsOnto);

    }

    public JsonObject genType_KeyValueMap() {
        return genType_KeyValueMap(model);
    }

    public JsonObject genType_KeyValueMap(OntModel ontModel) {
        JsonArray notMappedTypes = new JsonArray();
        OntClass ontClass;
        int counter = 0;
        JsonObject typeKeyValueMap = new JsonObject();
        Iterator<OntClass> itrClass = ontModel.listClasses();
        OsmMapper osmMapper = new OsmMapper();
        OsmMap osmMap = osmMapper.getOsmMap();
        while (itrClass.hasNext()) {
            ontClass = itrClass.next();
            String name = ontClass.getLocalName();
            ArrayList<OsmMapElement> mappings = findMappings(name, osmMap);
            if (mappings.size() < 1) {
                notMappedTypes.put(name);
            } else {
                typeKeyValueMap.put(name, genKeyValues(mappings));
            }
        }
        System.out.println(typeKeyValueMap.toString());
        System.out.println("not mapped types: " + notMappedTypes.toString());

        return typeKeyValueMap;
    }

    private static ArrayList<OsmMapElement> findMappings(String typeLocalName, OsmMap osmMap) {
        ArrayList<OsmMapElement> res = new ArrayList<>();
        for (OsmMapElement el : osmMap) {
            if (el.getType().equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")) {
                for (String val : el.getValue()) {
                    if (val.equals("http://openthings.cc/ontology/" + typeLocalName)) {
                        res.add(el);
                        break;
                    }
                }
            }
        }
        return res;
    }

    private static JsonArray genKeyValues(ArrayList<OsmMapElement> mappings) {
        Set<String> res = new HashSet<>();
        for (OsmMapElement el : mappings) {
            if (el.isMain()) {
                res.add("[" + el.getTagKey() + "]");
            } else {
                res.add("[" + el.getTagKey() + "=" + el.getTagValue() + "]");
            }
        }

        return new JsonArray(res);
    }

    public JsonObject ontoClassesToTypeTree(OntModel ontModel, int type) {
        Set<String> visited = new HashSet<>();
        Set<String> similar = new HashSet<>();
        OntClass ontClass;
        int counter = 0;
        JsonObject typesTree = new JsonObject();
        Iterator<OntClass> itrClass = ontModel.listClasses();
        while (itrClass.hasNext()) {
            ontClass = itrClass.next();
            String name = ontClass.getLocalName();
            if (visited.contains(name)) {
                similar.add(name);
            } else {
                visited.add(name);
            }

            counter++;
            System.out.println("------- NO." + counter + " -------");
// Parse the class
            System.out.println("----------" + ontClass.toString() + "-----------");

            switch(type) {
                case ALL_TYPES: typesTree.merge(ontoClassesToTypeTree(ontClass, new JsonObject(), 0, type)); break;
                case NODE:
                    if(!hasSuperClass(ontClass, "http://openthings.cc/ontology/RouteThing")) {
                        typesTree.merge(ontoClassesToTypeTree(ontClass, new JsonObject(), 0, type));
                    } else {
                        System.out.println("----------" + ontClass.toString() + "is not NODE -----------");
                    }
                    break;
                case ROUTE:
                    if(hasSuperClass(ontClass, "http://openthings.cc/ontology/RouteThing")) {
                        typesTree.merge(ontoClassesToTypeTree(ontClass, new JsonObject(), 0, type));
                    } else {
                        System.out.println("----------" + ontClass.toString() + "is not ROUTE -----------");
                    }
                    break;
            }



        }

        System.out.println("======= " + counter + " =======");

        shapeLeafs(typesTree);

        typesTree = finishItUp(deepSortTypeTree(typesTree));



        System.out.println(typesTree.toString());
        System.out.println("======= " + Common.countElements(typesTree) + " =======");

        System.out.println("similar: " + JsonElement.wrap(similar));

        return typesTree;
    }


    public JsonObject ontoClassesToTypeTree(OntClass ontClass, JsonObject subJson, int level, int type) {
        if (level >= maxLevel) return subJson;
        JsonObject res = new JsonObject();
        String name = ontClass.getLocalName();
        //.asComplementClass().getRDFType().getLocalName();
        //System.out.println("\t>>>>\t" + name);

        Resource superClass = null;
        NodeIterator superClasses = ontClass.listPropertyValues(ontClass.getProfile().SUB_CLASS_OF());

        if (!superClasses.hasNext()) {
            return new JsonObject().put(name, subJson);
        }

        level++;
        while (superClasses.hasNext()) {
            try {
                //superClass = superClasses.next().asResource();
                OntClass sClass = superClasses.next().as(OntClass.class);

                //if(superClass.getNameSpace().equals("http://schema.org/")) break;
                System.out.println("\t\t\t -- " + sClass);
                res.merge(ontoClassesToTypeTree((OntClass) sClass, new JsonObject().put(name, subJson), level, type));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return res;

    }

    private boolean hasSuperClass(OntClass ontClass, String className) {
        NodeIterator superClasses = ontClass.listPropertyValues(ontClass.getProfile().SUB_CLASS_OF());

        System.out.println("======= CHECKING: " + ontClass.toString() + " =======");
        if(ontClass.toString().equals(className)) return true;

        while (superClasses.hasNext()) {
            if(hasSuperClass(superClasses.next().as(OntClass.class), className)) {
                return true;
            }
        }

        return false;
    }


    //convert empty leaf json objects into string and container json object into json array
    public JsonArray shapeLeafs(JsonObject job) {
        JsonObject newJob = new JsonObject();
        Iterator<Map.Entry<String, JsonElement>> iterator = job.iterator();
        boolean isLeafContainer = true;
        while (iterator.hasNext()) {
            Map.Entry<String, JsonElement> entry = iterator.next();
            if (entry.getValue() != null && entry.getValue().isJsonObject()) {
                if (entry.getValue().asJsonObject().length() > 0) {
                    isLeafContainer = false;
                    JsonArray leafs = shapeLeafs(entry.getValue().asJsonObject());
                    if (leafs != null) {
                        iterator.remove();
                        newJob.put(entry.getKey(), leafs);
                    }
                }
            }
        }
        job.merge(newJob);
        if (isLeafContainer) {
            return job.names();
        } else {
            return null;
        }

    }

    public static JsonObject finishItUp(JsonObject job) {
        JsonObject res = new JsonObject();
        res.put("MainTypes", job.opt("GeoThing"));
        res.merge(job);
        res.remove("Thing");
        res.remove("GeoThing");
        return res;
    }

    public static JsonObject deepSortTypeTree(JsonObject job) {
        if (job == null || job.length() < 1) return job;
        TreeMap<String, JsonElement> sortedMap = new TreeMap<>();
        Iterator<Map.Entry<String, JsonElement>> iterator = job.iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonElement> el = iterator.next();
            JsonElement val = el.getValue();

            if (val.isJsonObject()) {
                val = deepSortTypeTree(val.asJsonObject());
            } else if (val.isJsonArray()) {
                ArrayList<String> nList = val.asJsonArray().toArrayList();

                Collections.sort(nList, new Comparator<String>() {
                    @Override
                    public int compare(String o1, String o2) {
                        return o1.compareTo(o2);
                    }
                });
                val = new JsonArray(nList);
            }
            sortedMap.put(el.getKey(), val);
        }
        return JsonElement.wrap(sortedMap).asJsonObject();
    }


}

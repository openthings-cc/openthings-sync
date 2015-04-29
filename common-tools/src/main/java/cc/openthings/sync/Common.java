package cc.openthings.sync;


import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.*;
import org.apache.http.client.utils.URIBuilder;
import org.djodjo.json.JsonElement;
import org.djodjo.json.JsonObject;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;

public class Common {
    public final static String openThingsOnto = "https://raw.githubusercontent.com/openthings-cc/openthings-onto/master/OpenThingsOnto.jsonld";
    public final static String gnOnto = "geonames-ontology_v3.1.rdf";
    public final static String gnMappings = "mappings_geonames3.01-all.rdf";
    public final static String mappingGeOntoGeonames = "mapping-geOnto-geonames.nt";
    public final static String mappingGeOntoDbpedia = "mapping_geOnto-dbpedia.rdf";
    public final static String mappingGeOntoLgdo = "mapping_geOnto-lgdo.nt";
    public final static String lovOnto = "lov.rdf";
    public final static String lgdOnto = "lgd_2014-09-09-ontology.sorted.nt";
    public final static String dbpediaOnto = "dbpedia_2014.owl";
    public final static String schemaorgOnto = "schemaorg.nt";
    public final static String biboOnto = "biboOnto.rdf";
    public final static String skosOnto = "skos.rdf";
    public final static String dulOnto = "dulOnto.owl";
    public final static String cidoccrmOnto = "cidoc-crmOnto.owl";
    public final static String bioOnto = "bioOnto.owl";
    public final static String d0Onto = "d0Onto.owl";
    public final static String dcTermsOnto = "dc-termsOnto.owl";
    public final static String dcElemOnto = "dc-elemOnto.owl";
    public final static String moOnto = "moOnto.rdfs";
    public final static String relationshipOnto = "relationshipOnto.owl";

    public static String getPrefix(URI resUri) throws URISyntaxException {
        String res;
        if(resUri.getFragment()!=null && !resUri.getFragment().isEmpty()) {
            res = new URIBuilder(resUri).setFragment("").build().toString();
        } else {
            res = new URIBuilder(resUri).setPath(resUri.getPath().substring(0, resUri.getPath().lastIndexOf("/"))).build().toString();
        }
        //System.out.println("prefix for uri: " + resUri  + " :: " + res);
        return res;
    }

    public static String getGeoType(OntClass ontClass) {
        if(ontClass.isRestriction()) {
            return ontClass.asRestriction().asHasValueRestriction().getHasValue().toString();
        } else {
            return ontClass.toString();
        }
    }

    public static Model getModel(File modelFile) {
        Model model= ModelFactory.createDefaultModel();
        model= model.read(modelFile.getAbsolutePath(), "RDF");
        InfModel rdfs= ModelFactory.createRDFSModel(model);

        return rdfs;
    }

    public static OntModel getOntModel(File modelFile) {

        return getOntModel(modelFile.getAbsolutePath());


    }

    public static OntModel getOntModel(String path) {

        OntModel ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);

        ontModel.read( path);
        Model baseModel = ontModel.getBaseModel();

        // System.out.println("baseModel Graph=" + baseModel.getGraph().toString());

        System.out.println( "======= Ont model created from: "+ path+" =======");

        return ontModel;

    }

    public static int countElements(JsonObject job) {
        int jobs = 0;
        for(JsonElement je:job.valuesSet()) {
            jobs++;
            if(je.isJsonObject()) {
                jobs += countElements(je.asJsonObject());
            }
        }
        return jobs;
    }

    public static JsonObject ontoClassesToJson(OntModel ontModel) {
        OntClass ontClass;
        int counter = 0;
        JsonObject typesTree =  new JsonObject();
        Iterator<OntClass> itrClass = ontModel.listClasses();
        while ( itrClass.hasNext()) {
            ontClass = itrClass.next();
            counter++;
            System.out.println( "------- NO." + counter + " -------");
// Parse the class
            System.out.println( "----------"+ontClass.toString()+"-----------");

            typesTree.merge(ontoClassToJson(ontClass, new JsonObject()));

        }
        System.out.println( "======= " + counter + " =======");
        System.out.println(typesTree.toString());
        System.out.println( "======= " + countElements(typesTree) + " =======");

        return typesTree;
    }


    public static JsonObject ontoClassToJson(OntClass ontClass, JsonObject subJson) {
        JsonObject res = new JsonObject();
        String name =  ontClass.toString();
        //.asComplementClass().getRDFType().getLocalName();
        //System.out.println("\t>>>>\t" + name);

        Resource superClass = null;
        NodeIterator superClasses = ontClass.listPropertyValues(ontClass.getProfile().SUB_CLASS_OF());

        if(!superClasses.hasNext()) {
            return new JsonObject().put(ontClass.toString(), subJson);
        }

        while(superClasses.hasNext()) {
            try {
                //superClass = superClasses.next().asResource();
                OntClass sClass = superClasses.next().as(OntClass.class);

                //if(superClass.getNameSpace().equals("http://schema.org/")) break;
                System.out.println("\t\t\t -- " + sClass);
                res.merge(ontoClassToJson((OntClass) sClass, new JsonObject().put(ontClass.toString(), subJson)));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return res;

    }


    //todo should consider the @context

    /**
     * Get JsonElement from json-ld model with specific resource id and property key
     * @param id resource id
     * @param prop property key
     * @param jsonLdModel json-ld model
     * @return Json element
     */
    public static JsonElement getElementFromModelJsonLd(String id, String prop, JsonObject jsonLdModel) {
        JsonElement je =  null;
        if("http://www.geonames.org/ontology#T.VAL".equals(id)) {
            //System.out.println("test");
        }
        org.djodjo.json.JsonArray modelItems = jsonLdModel.getJsonArray("@graph");
        for(JsonElement mi:modelItems) {
                if ("gn:Code".equals(mi.asJsonObject().optString("@type")) && id.endsWith(mi.asJsonObject().getString("@id").replace("gn:",""))) {
                    return mi.asJsonObject().opt(prop);
                }

        }
        return je;
    }



    //property should be language specific value either a single JsonObject or JsonArray
    // ex: {"@language":"en","@value":"bank"}
    public static JsonElement getElementForLanguage(JsonElement property, String language) {
        if(property.isJsonArray()) {
            for (JsonElement jo : property.asJsonArray()) {
                if (language.equals(jo.asJsonObject().getString("@language"))) {
                    return jo.asJsonObject().get("@value");
                }
            }
        } else if (language.equals(property.asJsonObject().getString("@language"))) {
            return property.asJsonObject().get("@value");
        }
        return null;
    }
    /**
     * GeoNames feature codes contain really specific feature codes which may result in matching several feature codes to one class from other ontology.
     * This method is trying to identify which is the most probable match by matching the resource id with the value of prefLabel for a specific language
     * Note that the default langunage is english but in case the ontology uses resource identifiers with different language other language must be specified
     *
     * @param gnIds list of the probable matching GN resources
     * @param expectedLabel expected lable based on the Resource Id
     * @param jsonLdModel geonames json-ld ontology model
     * @return geonames ontology resource id that best matches
     */
    public static String getBestGNEquivFromModelJsonLd(ArrayList<String> gnIds, String expectedLabel, JsonObject jsonLdModel) {
        return getBestGNEquivFromModelJsonLd(gnIds, expectedLabel, jsonLdModel, "en");
    }

    public static String getBestGNEquivFromModelJsonLd(ArrayList<String> gnIds, String expectedLabel, JsonObject jsonLdModel, String language) {
        String res = null;
        String currStr;
        int currDist;
        int bestDist = Integer.MAX_VALUE;
        for(String gnId:gnIds) {

            JsonElement labels = Common.getElementFromModelJsonLd(gnId, "prefLabel", jsonLdModel);
            if(labels!=null) {
                currStr = Common.getElementForLanguage(labels, language).asString();
                currDist = levenshtein(expectedLabel,currStr);
                if (currStr != null && currDist == 0) return gnId;
                if (bestDist > currDist) {
                    bestDist = currDist;
                    res = gnId;
                }
            }
        }

        return res;
    }

    public static int levenshtein(String a, String b) {
        a = a.toLowerCase();
        b = b.toLowerCase();
        // i == 0
        int [] costs = new int [b.length() + 1];
        for (int j = 0; j < costs.length; j++)
            costs[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            // j == 0; nw = lev(i - 1, j)
            costs[0] = i;
            int nw = i - 1;
            for (int j = 1; j <= b.length(); j++) {
                int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]), a.charAt(i - 1) == b.charAt(j - 1) ? nw : nw + 1);
                nw = costs[j];
                costs[j] = cj;
            }
        }
        return costs[b.length()];
    }
}

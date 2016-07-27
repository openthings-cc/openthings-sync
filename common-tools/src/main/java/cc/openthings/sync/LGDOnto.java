package cc.openthings.sync;

import io.apptik.json.JsonArray;
import io.apptik.json.JsonElement;
import io.apptik.json.JsonObject;
import io.apptik.json.JsonString;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;

import static cc.openthings.sync.Common.getOntModel;
import static cc.openthings.sync.Common.ontoClassesToJson;

public class LGDOnto {

    String queryString= "select * where { ?x <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2002/07/owl#Class> }";


    public static void main(String[] args) {
        LGDOnto lgdOnto = new LGDOnto();
       //   lgdOnto.analyseLGDtypes();
        lgdOnto.printLGDJsonLd();
        //lgdOnto.qTest();
      //  lgdOnto.genOTOnto();
    }

    public OntModel getLGDModel() {
        return getOntModel(new File(getClass().getClassLoader().getResource(Common.lgdOnto).getFile()), OntModelSpec.OWL_MEM);
    }

    private void analyseLGDtypes() {


        OntModel model =  getLGDModel();

        ontoClassesToJson(model);

    }

    private void printLGDJsonLd() {
        OntModel model =  getLGDModel();
        JsonObject je = printJsonLd(model);
        try {

            Writer fw = new FileWriter("lgd-onto-jld.json");
            je.writeTo(fw);
            fw.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private JsonObject printJsonLd(Model model) {
        /////////
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        model.write(os, "JSON-LD");
        InputStream is = new ByteArrayInputStream(os.toByteArray());
        //not need to expand just keep common context for geoThings
        //Object jObj = JsonUtils.fromInputStream(is);
        //Object expjObj = JsonLdProcessor.expand(jObj);
        //System.out.println("Expanded:" + expjObj);
        InputStreamReader isr = new InputStreamReader(is);

        try {
            JsonObject je = JsonObject.readFrom(isr).asJsonObject();
            System.out.println(je.toString());
            is.close();
            return je;

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }

    public void qTest() {
        int types = 0;
        Query query= QueryFactory.create(queryString);
        QueryExecution qe= QueryExecutionFactory.create(query, getLGDModel());
        ResultSet results= qe.execSelect();

        while (results.hasNext()) {
            QuerySolution row= results.next();
            //  String value= row.getLiteral("name").toString();
            String value= row.toString();
            types++;
            System.out.println(value);
        }
        System.out.println("Types: " + types);
    }



    public void genOTOnto() {
        OntModel gnModel = getOntModel(new File(getClass().getClassLoader().getResource(Common.gnOnto).getFile()), OntModelSpec.OWL_MEM);
        JsonObject gnJsonLd = printJsonLd(gnModel);


        OntAnalyser ontAnalyser = new OntAnalyser();
        JsonObject allMappingJson = ontAnalyser.analyseAllMappings();
        OntModel model =  getLGDModel();
        JsonObject job = printJsonLd(model);

        job.remove("@context");
        JsonArray graph = job.getJsonArray("@graph");

        Iterator<JsonElement> jsIt = graph.iterator();

        while(jsIt.hasNext()) {
            JsonObject ontoItem = jsIt.next().asJsonObject();
            if(!ontoItem.has("@type")) {
                //this is source tag (key:value) and we dont care of this
                jsIt.remove();
            } else if(ontoItem.get("@type").isJsonArray()) {
                //this is a property definition, we used the ones defined by LGD ontology
                //after data is merged and well analysed properties can be defined
                jsIt.remove();
            } else if(!ontoItem.getString("@type").equals("owl:Class")) {
                throw new RuntimeException("type not a class what is it then ??" + ontoItem.asJsonObject().getString("@type"));
            } else {

                //we dont need this
                ontoItem.remove("sourceTag");
                ontoItem.remove("sourceKey");

                //put equivalent class
                JsonArray equivClasses = allMappingJson.optJsonArray(ontoItem.getString("@id"));
                if(equivClasses==null) equivClasses = new JsonArray();
                ontoItem.put("equivalentClass", equivClasses
                                .put(ontoItem.getString("@id"))
                );

                //add info from gn equivs have in mind to check if 2 equivs available choose the one with most @en labe similarity to resource @id
                //Common.getElementFromModelJsonLd("gn:S.ESTT","definition", gnJsonLd)
                ArrayList<String> gnEquivs = new ArrayList<>();
                for(JsonElement je:equivClasses) {
                    if(je.toString().startsWith("http://www.geonames.org/ontology#")) {
                        gnEquivs.add(je.toString());
                    }
                }
                if(gnEquivs.size()>0) {
                    if(gnEquivs.size()==1) {
                        JsonElement def = Common.getElementFromModelJsonLd(gnEquivs.get(0), "definition", gnJsonLd);
                        if(def!=null) {
                            ontoItem.put("definition", def);
                        }
                        JsonElement lbl = Common.getElementFromModelJsonLd(gnEquivs.get(0), "prefLabel", gnJsonLd);
                        if(lbl!=null) {
                            ontoItem.put("prefLabel", lbl);
                        }
                    } else {
                        //if more than one GN equiv then we need to find the best match
                        String bestId  = Common.getBestGNEquivFromModelJsonLd(gnEquivs, ontoItem.getString("@id").substring(ontoItem.getString("@id").lastIndexOf("/")+1), gnJsonLd);
                        if(bestId!=null) {
                            JsonElement def = Common.getElementFromModelJsonLd(bestId, "definition", gnJsonLd);
                            if (def != null) {
                                ontoItem.put("definition", def);
                            }
                            JsonElement lbl = Common.getElementFromModelJsonLd(bestId, "prefLabel", gnJsonLd);
                            if (lbl != null) {
                                ontoItem.put("prefLabel", lbl);
                            }
                        } else {
                            System.out.println("no real match with GN onto from" + gnEquivs + "\nwith: " + ontoItem.toString() );
                        }
                    }
                }
                //replace id
                ontoItem.put("@id", ontoItem.getString("@id").replace("http://linkedgeodata.org/", "http://openthings.cc/"));
                //replace subClassOf or make one
                if (ontoItem.has("subClassOf")) {
                    if (ontoItem.get("subClassOf").isString()) {
                        ontoItem.put("subClassOf", ontoItem.getString("subClassOf").replace("http://linkedgeodata.org/", "http://openthings.cc/"));
                    } else {
                        JsonArray parentClasses = ontoItem.getJsonArray("subClassOf");
                        Iterator<JsonElement> parentIt = parentClasses.iterator();
                        ArrayList<JsonString> newParents = new ArrayList<>();
                        while (parentIt.hasNext()) {
                            String parentClass = parentIt.next().asString();
                            parentIt.remove();
                            newParents.add(new JsonString(parentClass.replace("http://linkedgeodata.org/", "http://openthings.cc/")));
                        }
                        parentClasses.addAll(newParents);
                    }
                } else {
                    ontoItem.put("subClassOf", "http://openthings.cc/ontology/GeoThing");
                }
            }
        }

        try {
            InputStreamReader isr =  new InputStreamReader(getClass().getClassLoader().getResourceAsStream("openthings-context.json"));
            job.merge(JsonElement.readFrom(isr).asJsonObject());
            isr.close();
            Writer fw = new FileWriter("OpenThingsOnto.json");
            job.writeTo(fw);
            fw.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

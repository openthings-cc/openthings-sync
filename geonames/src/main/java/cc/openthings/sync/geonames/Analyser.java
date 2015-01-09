/*
 * Copyright (C) 2014 OpenThings Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cc.openthings.sync.geonames;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.*;
import org.djodjo.json.JsonElement;
import org.djodjo.json.JsonObject;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

public class Analyser {

    final static String gnOnto = "geonames-ontology_v3.1.rdf";
    final static String gnMappings = "mappings_geonames3.01-all.rdf";

    final static String lgdOnto = "lgd_2014-09-09-ontology.sorted.nt";


    String queryString= "select * where { ?x <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2002/07/owl#Class> }";
    String queryGNtypes= "select * where { ?x <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.geonames.org/ontology#Code> }";
    String queryGN2LGDmapping= "select * where { ?x <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2002/07/owl#Class> . " +
            // "FILTER regex(str(?x), \"linkedgeodata\") " +
            "}";



    public static void main(String[] args) {
        Analyser ontAnalyser = new Analyser();
        ontAnalyser.analyseGN();
    }

    private void analyseGN() {
         analyseGNtypes();
     //   analyseGNmappings();

        //  analyseLGDtypes();
    }

    private String getGeoType(OntClass ontClass) {
        if(ontClass.isRestriction()) {
            return ontClass.asRestriction().asHasValueRestriction().getHasValue().toString();
        } else {
            return ontClass.toString();
        }
    }

    public void collectEquivClassInfo( OntModel ontModel) {

        HashMap<String, HashSet<String>> typeMappings = new HashMap<>();
        OntClass ontClass;
        int counter = 0;
         Iterator<OntClass> itrClass = ontModel.listClasses();
        while ( itrClass.hasNext()) {

            ontClass = itrClass.next();
            counter++;
            System.out.println( "------- NO." + counter + " -------");
            System.out.println( "----------"+ontClass.toString()+"-----------");

           String mappingKey = getGeoType(ontClass);
            HashSet<String> mappingValue = typeMappings.get(mappingKey);
            if(mappingValue==null) {
                mappingValue =  new HashSet<>();
                typeMappings.put(mappingKey, mappingValue);
            }

            Iterator<OntClass> itr2Class = ontClass.listEquivalentClasses();
            OntClass equivClass;
            HashSet<String> visitedEquivs = new HashSet<>();
            //addself
            visitedEquivs.add(mappingKey);
            if(itr2Class.hasNext()) {
                while ( itr2Class.hasNext()) {

                    equivClass = itr2Class.next();
                    System.out.println("==========" + equivClass.toString() + "==========");
                   String equivType;
                    if(equivClass.isRestriction()) {
                        System.out.println("********" + equivClass.asRestriction().asHasValueRestriction().getHasValue().toString() + "********");
                        equivType = equivClass.asRestriction().asHasValueRestriction().getHasValue().toString();

                    } else {
                        equivType = equivClass.toString();
                    }
                    mappingValue.add(equivType);
                    putEquivClasses(typeMappings, mappingValue, equivType, visitedEquivs);
                    //put reverse link

                    HashSet<String> mappingEquivValue = typeMappings.get(equivType);
                    if(mappingEquivValue==null) {
                        mappingEquivValue =  new HashSet<>();
                        typeMappings.put(equivType, mappingEquivValue);
                    }

                    mappingEquivValue.add(mappingKey);
                    for(String equivMapping:mappingValue) {
                        if(!equivMapping.equals(equivType)) {
                            mappingEquivValue.add(equivMapping);
                        }
                    }

                }
                mappingValue.remove(mappingKey);
            } else {
                //  System.out.println("---------- ?!?!?! NO EQUIV CLASS ?!?!?! -----------");

            }

        }
        System.out.println( "======= " + counter + " =======");
        System.out.println( "======= \n" + new JsonObject(typeMappings).toString() + "\n =======");
    }

    private void putEquivClasses(HashMap<String, HashSet<String>> typeMappings, HashSet<String> srcEquivTypesArray, String typeKey, HashSet<String> visitedEquivs) {
        if(visitedEquivs.contains(typeKey)) return;
        visitedEquivs.add(typeKey);
        HashSet<String> typeSet = typeMappings.get(typeKey);
        if(typeSet!=null && typeSet.size()>0) {
            srcEquivTypesArray.addAll(typeSet);
            for (String equivType : typeSet) {
                putEquivClasses(typeMappings, srcEquivTypesArray, equivType, visitedEquivs);
            }
        }
    }

    private void analyseGNmappings() {
        int mappings = 0;
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(gnMappings).getFile());
        OntModel model = getOntModel(file);
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
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.exit(0);
        collectEquivClassInfo(model);

        /*
         File file = new File(classLoader.getResource(gnMappings).getFile());
        Model model =  getModel(file);

        Query query= QueryFactory.create(queryGN2LGDmapping);
        QueryExecution qe= QueryExecutionFactory.create(query, model);
        ResultSet results= qe.execSelect();

        while (results.hasNext()) {
            QuerySolution row= results.next();
            //  String value= row.getLiteral("name").toString();
            String value= row.toString();
            mappings++;
            System.out.println(value);
        }
        System.out.println("Mappings: " + mappings);
         */
    }




    private void analyseGNtypes() {
        int types = 0;
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(gnOnto).getFile());
        Model model =  getModel(file);
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
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.exit(0);
        //////////

        Query query= QueryFactory.create(queryGNtypes);
        QueryExecution qe = QueryExecutionFactory.create(query, model);
        ResultSet results = qe.execSelect();

        while (results.hasNext()) {
            QuerySolution row = results.next();
            //  String value= row.getLiteral("name").toString();
            String value = row.toString();
            types++;
            System.out.println(value);
        }
        System.out.println("Types: " + types);
    }


    private void analyseLGDtypes() {
        int types = 0;
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(lgdOnto).getFile());
        OntModel model =  getOntModel(file);


        collectClassInfo( model);
        //collectObjectInfo(model);
        System.exit(0);

        Query query= QueryFactory.create(queryString);
        QueryExecution qe= QueryExecutionFactory.create(query, model);
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



    public static void collectObjectInfo( Model model) {
        System.out.println("=======  getting subjects  =======");
        ResIterator itr = model.listSubjects();
        JsonObject currObject = null;
        Resource resource;
        int counter = 0;
        while ( itr.hasNext()) {
            System.out.println("======= " + counter + " =======");
            resource = itr.next();
            System.out.println(resource.toString());
            counter++;
        }
    }

    public Model getModel(File modelFile) {
        Model model= ModelFactory.createDefaultModel();
        model= model.read(modelFile.getAbsolutePath(), "RDF");
        InfModel rdfs= ModelFactory.createRDFSModel(model);

        return rdfs;
    }

    public OntModel getOntModel(File modelFile) {

        OntModel ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);

        ontModel.read( modelFile.getAbsolutePath());
        Model baseModel = ontModel.getBaseModel();

        // System.out.println("baseModel Graph=" + baseModel.getGraph().toString());

        System.out.println( "======= Classes =======");

        return ontModel;

    }

    public void collectClassInfo( OntModel ontModel) {
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

            typesTree.merge(parseClass(ontClass, new JsonObject()));

        }
        System.out.println( "======= " + counter + " =======");
        System.out.println(typesTree.toString());
        System.out.println( "======= " + countElements(typesTree) + " =======");
    }

    public int countElements(JsonObject job) {
        int jobs = 0;
        for(JsonElement je:job.valuesSet()) {
            jobs++;
            if(je.isJsonObject()) {
                jobs += countElements(je.asJsonObject());
            }
        }
        return jobs;
    }

    public JsonObject parseClass( OntClass ontClass, JsonObject subJson) {
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
                res.merge(parseClass((OntClass) sClass, new JsonObject().put(ontClass.toString(), subJson)));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return res;

    }





}




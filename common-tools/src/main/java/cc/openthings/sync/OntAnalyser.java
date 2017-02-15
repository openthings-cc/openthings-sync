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

package cc.openthings.sync;


import io.apptik.json.JsonObject;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import static cc.openthings.sync.Common.*;

public class OntAnalyser {


    String queryGNtypes = "select * where { ?x <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.geonames.org/ontology#Code> }";
    String queryGN2LGDmapping = "select * where { ?x <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2002/07/owl#Class> . " +
            // "FILTER regex(str(?x), \"linkedgeodata\") " +
            "}";


    public static void main(String[] args) {
        OntAnalyser ontAnalyser = new OntAnalyser();
        //ontAnalyser.analyseGN();
        //ontAnalyser.analyseGenericRemote(Common.foodRdfOnto, OntModelSpec.OWL_MEM);
        //ontAnalyser.analyseGenericRemote(Common.datex2Onto, OntModelSpec.OWL_MEM_MICRO_RULE_INF, "TURTULE");
//agrovoc too big to store in git donot use that
        ontAnalyser.analyseAgroVocTypes();
        //ontAnalyser
    }

    private void analyseGN() {
        analyseGNtypes();
        // analyseAllMappings();

        //   analyseLGDtypes();
    }

    public JsonObject analyseAllMappings() {
        JsonObject allMappingJson = null;
        int mappings = 0;
        Map<String, HashSet<String>> typeMappings = Collections.synchronizedMap(new HashMap<String, HashSet<String>>());
        ClassLoader classLoader = getClass().getClassLoader();
        OntModel gnMappingsModel = getOntModel(new File(classLoader.getResource(Common.gnMappings).getFile()), OntModelSpec.OWL_MEM);
        OntModel mappingGeOntoGeonamesModel = getOntModel(new File(classLoader.getResource(Common.mappingGeOntoGeonames).getFile()), OntModelSpec.OWL_MEM);
        OntModel mappingGeOntoDbpediaModel = getOntModel(new File(classLoader.getResource(Common.mappingGeOntoDbpedia).getFile()), OntModelSpec.OWL_MEM);
        Model mappingGeOntoLgdoModel = getModel(new File(classLoader.getResource(Common.mappingGeOntoLgdo).getFile()));

        collectEquivClassInfo(gnMappingsModel, typeMappings);
        collectEquivClassInfo2(mappingGeOntoDbpediaModel, typeMappings);

        collectSameAsStatements(mappingGeOntoGeonamesModel, typeMappings);
        collectSameAsStatements(mappingGeOntoLgdoModel, typeMappings);
//        OntModel lovModel = getOntModel(new File(classLoader.getResource(lovOnto).getFile()));
//        collectEquivClassInfo(lovModel);

//        OntModel dbpediaModel = getOntModel(new File(classLoader.getResource(dbpediaOnto).getFile()));
//        OntModel schemaorgModel = getOntModel(new File(classLoader.getResource(schemaorgOnto).getFile()));
//        OntModel biboModel = getOntModel(new File(classLoader.getResource(biboOnto).getFile()));
//        OntModel skosModel = getOntModel(new File(classLoader.getResource(skosOnto).getFile()));
//        OntModel dulModel = getOntModel(new File(classLoader.getResource(dulOnto).getFile()));
//        OntModel cidocModel = getOntModel(new File(classLoader.getResource(cidoccrmOnto).getFile()));
//        OntModel bioModel = getOntModel(new File(classLoader.getResource(bioOnto).getFile()));
//        OntModel d0Model = getOntModel(new File(classLoader.getResource(d0Onto).getFile()));
//        OntModel dcTermsModel = getOntModel(new File(classLoader.getResource(dcTermsOnto).getFile()));
//        OntModel dcElemModel = getOntModel(new File(classLoader.getResource(dcElemOnto).getFile()));
//        OntModel moModel = getOntModel(new File(classLoader.getResource(moOnto).getFile()));
//        OntModel relationshipModel = getOntModel(new File(classLoader.getResource(relationshipOnto).getFile()));

        // collectEquivClassInfo(model);


//        model.add(dbpediaModel);
//        model.add(schemaorgModel);
//        model.add(biboModel);
//        model.add(skosModel);
//        model.add(dulModel);
//        model.add(cidocModel);
//        model.add(bioModel);
//        model.add(d0Model);
//        model.add(dcTermsModel);
//        model.add(dcElemModel);
//        model.add(moModel);
//        model.add(relationshipModel);
//        collectEquivClassInfo(model);




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
        System.out.println("======= \n" + new JsonObject(typeMappings).toString() + "\n =======");
        try {
            Writer fw = new FileWriter("all-mappings.json");
            allMappingJson = new JsonObject(typeMappings);
            allMappingJson.writeTo(fw);
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        JsonObject geonamesMappings = new JsonObject();
        for (Map.Entry<String, HashSet<String>> entry : typeMappings.entrySet()) {
            if (entry.getKey().startsWith("http://www.geonames.org/ontology")) {
                geonamesMappings.put(entry.getKey(), entry.getValue());
            }
        }
        System.out.println(geonamesMappings.toString());
        return allMappingJson;
    }


    //Collect SameAs-es from model
    public void collectSameAsStatements(Model ontModel, Map<String, HashSet<String>> typeMappings) {
        System.out.println("-------get sameAs from model:" + ontModel.toString() + " -------");


        Statement statement;
        int counter = 0;
        StmtIterator itrStmt = ontModel.listStatements();
        String equivType;

        while (itrStmt.hasNext()) {

            statement = itrStmt.next();
            if (statement.getPredicate().toString().equals("http://www.w3.org/2002/07/owl#sameAs")) {
                counter++;
                System.out.println("------- NO." + counter + " -------");
                System.out.println("----------" + statement.toString() + "-----------");

                String mappingKey = statement.getSubject().getURI();
                HashSet<String> visitedEquivs = new HashSet<>();
                //addself
                visitedEquivs.add(mappingKey);

                HashSet<String> mappingValues = typeMappings.get(mappingKey);
                if (mappingValues == null) {
                    mappingValues = new HashSet<>();
                    typeMappings.put(mappingKey, mappingValues);
                }
                equivType = statement.getObject().asResource().getURI();
                System.out.println("==========" + equivType + "==========");

                //

                addMapping(mappingKey, equivType, mappingValues);
                //putEquivClasses(typeMappings, mappingValues, equivType, visitedEquivs);

                //put reverse link
                //get set for the value
                HashSet<String> visitedEquivs2 = new HashSet<>();
//addself
                visitedEquivs2.add(equivType);

                HashSet<String> mappingEquivValues = typeMappings.get(equivType);
                if (mappingEquivValues == null) {
                    mappingEquivValues = new HashSet<>();
                    typeMappings.put(equivType, mappingEquivValues);
                }

                addMapping(equivType, mappingKey, mappingEquivValues);

                for (String equivMapping : mappingValues) {
                    if (!equivMapping.equals(equivType)) {
                        addMapping(equivType, equivMapping, mappingEquivValues);
                        addMapping(equivMapping, equivType, typeMappings.get(equivMapping));
                        // putEquivClasses(typeMappings, mappingEquivValues, equivMapping, visitedEquivs2);

                    }
                }
            }

        }
        System.out.println("======= " + counter + " =======");
        System.out.println("======= \n" + new JsonObject(typeMappings).toString() + "\n =======");

    }

    private void addMapping(String key, String newMapping, Set<String> mappings) {
        final URI keyUri = URI.create(key);
        final URI newMappingUri = URI.create(newMapping);
        try {
            final String prefixKey = getPrefix(keyUri);
            final String prefixValue = getPrefix(newMappingUri);
            if (!prefixKey.equals(prefixValue)) {
                mappings.add(newMapping);
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

    }


    //collect sameAs-es from ontology model
    public void collectSameAsClasses(OntModel ontModel) {
        System.out.println("-------get sameAs from model:" + ontModel.toString() + " -------");

        HashMap<String, HashSet<String>> typeMappings = new HashMap<>();
        OntClass ontClass;
        int counter = 0;
        Iterator<OntClass> itrClass = ontModel.listClasses();
        while (itrClass.hasNext()) {

            ontClass = itrClass.next();
            counter++;
            System.out.println("------- NO." + counter + " -------");
            System.out.println("----------" + ontClass.toString() + "-----------");

            String mappingKey = getGeoType(ontClass);
            HashSet<String> mappingValue = typeMappings.get(mappingKey);
            if (mappingValue == null) {
                mappingValue = new HashSet<>();
                typeMappings.put(mappingKey, mappingValue);
            }
///
            NodeIterator sameClasses = ontClass.listPropertyValues(ontClass.getProfile().SAME_AS());
            String equivType;
            HashSet<String> visitedEquivs = new HashSet<>();
            //addself
            visitedEquivs.add(mappingKey);
            if (sameClasses.hasNext()) {
                while (sameClasses.hasNext()) {
                    equivType = sameClasses.next().asResource().getURI();
                    System.out.println("==========" + equivType + "==========");

                    //
                    mappingValue.add(equivType);
                    putEquivClasses(typeMappings, mappingValue, equivType, visitedEquivs);
                    //put reverse link

                    HashSet<String> mappingEquivValue = typeMappings.get(equivType);
                    if (mappingEquivValue == null) {
                        mappingEquivValue = new HashSet<>();
                        typeMappings.put(equivType, mappingEquivValue);
                    }

                    addMapping(equivType, mappingKey, mappingEquivValue);

                    for (String equivMapping : mappingValue) {
                        if (!equivMapping.equals(equivType)) {
                            addMapping(equivType, equivMapping, mappingEquivValue);
                            addMapping(equivMapping, equivType, typeMappings.get(equivMapping));
                            // putEquivClasses(typeMappings, mappingEquivValues, equivMapping, visitedEquivs2);

                        }
                    }


                    //
                }
                //remove self if there
                mappingValue.remove(mappingKey);
            } else {
                //  System.out.println("---------- ?!?!?! NO SameAs CLASS ?!?!?! -----------");

            }
        }
        System.out.println("======= " + counter + " =======");
        System.out.println("======= \n" + new JsonObject(typeMappings).toString() + "\n =======");

    }

    public void collectEquivClassInfo(OntModel ontModel, Map<String, HashSet<String>> typeMappings) {
        System.out.println("------- model:" + ontModel.toString() + " -------");


        OntClass ontClass;
        int counter = 0;
        Iterator<OntClass> itrClass = ontModel.listClasses();
        while (itrClass.hasNext()) {

            ontClass = itrClass.next();
            counter++;
            System.out.println("------- NO." + counter + " -------");
            System.out.println("----------" + ontClass.toString() + "-----------");

            String mappingKey = getGeoType(ontClass);
            HashSet<String> mappingValue = typeMappings.get(mappingKey);
            if (mappingValue == null) {
                mappingValue = new HashSet<>();
                typeMappings.put(mappingKey, mappingValue);
            }

            Iterator<OntClass> itr2Class = ontClass.listEquivalentClasses();
            OntClass equivClass;
            HashSet<String> visitedEquivs = new HashSet<>();
            //addself
            visitedEquivs.add(mappingKey);
            if (itr2Class.hasNext()) {
                while (itr2Class.hasNext()) {

                    equivClass = itr2Class.next();
                    System.out.println("==========" + equivClass.toString() + "==========");
                    String equivType;
                    if (equivClass.isRestriction()) {
                        if (equivClass.asRestriction().isHasValueRestriction()) {
                            System.out.println("********" + equivClass.asRestriction().asHasValueRestriction().getHasValue().toString() + "********");
                            equivType = equivClass.asRestriction().asHasValueRestriction().getHasValue().toString();
                        } else {
                            //TODO handle other restrctions (for now we care only for geonames specific using hasvalue)
                            continue;
                        }
                    } else {
                        equivType = equivClass.toString();
                    }
                    //   mappingValue.add(equivType);
                    addMapping(mappingKey, equivType, mappingValue);
                    putEquivClasses(typeMappings, mappingValue, equivType, visitedEquivs);
                    //put reverse link

                    HashSet<String> mappingEquivValue = typeMappings.get(equivType);
                    if (mappingEquivValue == null) {
                        mappingEquivValue = new HashSet<>();
                        typeMappings.put(equivType, mappingEquivValue);
                    }

                    addMapping(equivType, mappingKey, mappingEquivValue);

                    for (String equivMapping : mappingValue) {
                        if (!equivMapping.equals(equivType)) {
                            addMapping(equivType, equivMapping, mappingEquivValue);
                            addMapping(equivMapping, equivType, typeMappings.get(equivMapping));
                            // putEquivClasses(typeMappings, mappingEquivValues, equivMapping, visitedEquivs2);

                        }
                    }

                }
                //mappingValue.remove(mappingKey);
            } else {
                //  System.out.println("---------- ?!?!?! NO EQUIV CLASS ?!?!?! -----------");

            }

        }
        System.out.println("======= " + counter + " =======");
        System.out.println("======= \n" + new JsonObject(typeMappings).toString() + "\n =======");
    }

    public void collectEquivClassInfo2(OntModel ontModel, Map<String, HashSet<String>> typeMappings) {
        System.out.println("------- model:" + ontModel.toString() + " -------");


        OntClass ontClass;
        int counter = 0;
        Iterator<OntClass> itrClass = ontModel.listClasses();
        while (itrClass.hasNext()) {

            ontClass = itrClass.next();
            counter++;
            System.out.println("------- NO." + counter + " -------");
            System.out.println("----------" + ontClass.toString() + "-----------");

            String mappingKey = getGeoType(ontClass);
            HashSet<String> mappingValue = typeMappings.get(mappingKey);
            if (mappingValue == null) {
                mappingValue = new HashSet<>();
                typeMappings.put(mappingKey, mappingValue);
            }

            NodeIterator itr2Class = ontClass.listPropertyValues(ontClass.getProfile().EQUIVALENT_CLASS());

            OntClass equivClass;
            HashSet<String> visitedEquivs = new HashSet<>();
            //addself
            visitedEquivs.add(mappingKey);
            if (itr2Class.hasNext()) {
                while (itr2Class.hasNext()) {

                    String equivType = itr2Class.next().asResource().getURI();
                    System.out.println("==========" + equivType + "==========");


                    //   mappingValue.add(equivType);
                    addMapping(mappingKey, equivType, mappingValue);
                    putEquivClasses(typeMappings, mappingValue, equivType, visitedEquivs);
                    //put reverse link

                    HashSet<String> mappingEquivValue = typeMappings.get(equivType);
                    if (mappingEquivValue == null) {
                        mappingEquivValue = new HashSet<>();
                        typeMappings.put(equivType, mappingEquivValue);
                    }

                    addMapping(equivType, mappingKey, mappingEquivValue);

                    for (String equivMapping : mappingValue) {
                        if (!equivMapping.equals(equivType)) {
                            addMapping(equivType, equivMapping, mappingEquivValue);
                            addMapping(equivMapping, equivType, typeMappings.get(equivMapping));
                            // putEquivClasses(typeMappings, mappingEquivValues, equivMapping, visitedEquivs2);

                        }
                    }

                }
                //mappingValue.remove(mappingKey);
            } else {
                //  System.out.println("---------- ?!?!?! NO EQUIV CLASS ?!?!?! -----------");

            }

        }
        System.out.println("======= " + counter + " =======");
        System.out.println("======= \n" + new JsonObject(typeMappings).toString() + "\n =======");
    }


    private void putEquivClasses(Map<String, HashSet<String>> typeMappings, HashSet<String> srcEquivTypesArray, String typeKey, HashSet<String> visitedEquivs) {
        if (visitedEquivs.contains(typeKey)) return;
        visitedEquivs.add(typeKey);
        HashSet<String> typeSet = typeMappings.get(typeKey);
        if (typeSet != null && typeSet.size() > 0) {
            //srcEquivTypesArray.addAll(typeSet);
            for (String typeS : typeSet) {
                addMapping(typeKey, typeS, srcEquivTypesArray);
                //  addMapping(typeS, typeKey, typeMappings.get(typeS));
            }
            //dont deeplink too much
            for (String equivType : typeSet) {

                putEquivClasses(typeMappings, srcEquivTypesArray, equivType, visitedEquivs);
                //putEquivClasses(typeMappings, typeMappings.get(equivType), typeKey, visitedEquivs);
            }
        }
        ///then put reverse

    }

    private void analyseGenericRemote(String url, OntModelSpec spec) {
        this.analyseGenericRemote(url, spec, null);
    }

    private void analyseGenericRemote(String url, OntModelSpec spec, String lang) {
        int types = 0;
        Model model = getOntModel(url, spec, lang);
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
        // System.exit(0);
        //fetch types form json-ld
        //////////

        Query query = QueryFactory.create(queryGN2LGDmapping);
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

    private void analyseAgroVocTypes() {
        int types = 0;
        System.out.println("will read model");
        Dataset dataset = getAgrovocDataset();
        dataset.begin(ReadWrite.READ) ;
        Model model = dataset.getDefaultModel();
        System.out.println("======= Agrovoc model read from: agrovoc-tdb-store =======");
        System.out.println("model read: " +  model.getGraph().size());
        dataset.end();
        /////////

        System.exit(0);
        dataset.begin(ReadWrite.WRITE) ;
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

        dataset.end();

        System.exit(0);
        //fetch types form json-ld
        //////////

        Query query = QueryFactory.create(queryGNtypes);
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

    private void analyseGNtypes() {
        int types = 0;
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(Common.gnOnto).getFile());
        Model model = getModel(file);
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
        //fetch types form json-ld
        //////////

        Query query = QueryFactory.create(queryGNtypes);
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


    public static void collectObjectInfo(Model model) {
        System.out.println("=======  getting subjects  =======");
        ResIterator itr = model.listSubjects();
        JsonObject currObject = null;
        Resource resource;
        int counter = 0;
        while (itr.hasNext()) {
            System.out.println("======= " + counter + " =======");
            resource = itr.next();
            System.out.println(resource.toString());
            counter++;
        }
    }


    ////\


}




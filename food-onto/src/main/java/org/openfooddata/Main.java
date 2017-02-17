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

package org.openfooddata;

import io.apptik.json.JsonObject;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static cc.openthings.sync.Common.getAgrovocDataset;
import static cc.openthings.sync.Common.getOntModel;
import static org.apache.jena.query.ReadWrite.READ;

public class Main {

    static Dataset agrovocDataset;

    //http://data.lirmm.fr/ontologies/food#
    public final static String f1 = "food.ttl";
    //http://www.w3.org/TR/2004/REC-owl-guide-20040210/food.rdf (demo)
    public final static String f2 = "food2.rdf";
    //https://raw.githubusercontent.com/ailabitmo/food-ontology/master/food.owl
    // (https://fruct.org/publications/abstract13/files/Kol.pdf)
    public final static String f3 = "food3.owl";

    static String queryString= "select ?x where { ?x <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2002/07/owl#Class> }";
    static String queryStringFood1 = "select ?x where { ?x <http://www.w3.org/2000/01/rdf-schema#subClassOf> <http://data.lirmm.fr/ontologies/food#Food> }";
    static String queryStringFood2 = "select ?x where { ?x <http://www.w3.org/2000/01/rdf-schema#subClassOf> <http://data.lirmm.fr/ontologies/food#Food> }";
    static String queryStringFood3 = "select ?x where { ?x <http://www.w3.org/2000/01/rdf-schema#subClassOf> <http://purl.org/foodontology#Ingredient> }";
    static String queryStringProps1= "select ?x ?domain " +
            "where { " +
            "?x <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/1999/02/22-rdf-syntax-ns#Property> ." +
            "OPTIONAL { ?x <http://www.w3.org/2000/01/rdf-schema#domain> ?domain }" +
            "}";
    static String queryStringProps23= "select ?x ?domain " +
            "where { " +
            "?x <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2002/07/owl#ObjectProperty> ." +
            "OPTIONAL { ?x <http://www.w3.org/2000/01/rdf-schema#domain> ?domain }" +
            "}";

    static String prefixSkos = "prefix skos: <http://www.w3.org/2004/02/skos/core#> ";

    static String qAgrovocRoots = prefixSkos +
            "select ?x ?l where {" +
                "?x skos:topConceptOf <http://aims.fao.org/aos/agrovoc> ." +
                "?x skos:prefLabel ?l ." +
                "     FILTER (lang(?l) = 'en')" +
            "}";

    static String agrovocObjects = "<http://aims.fao.org/aos/agrovoc/c_330919>";
    static String agrovocSubjects = "<http://aims.fao.org/aos/agrovoc/c_330829>";
    static String agrovocEntities = "<http://aims.fao.org/aos/agrovoc/c_330892>";
    static String agrovocSystems = "<http://aims.fao.org/aos/agrovoc/c_330985>";
    static String agrovocProducts = "<http://aims.fao.org/aos/agrovoc/c_6211>";
    static String agrovocOrganisms = "<http://aims.fao.org/aos/agrovoc/c_49904>";
    static String agrovocProcesses = "<http://aims.fao.org/aos/agrovoc/c_13586>";
    static String agrovocPlants = "<http://aims.fao.org/aos/agrovoc/c_5993>";

    public static String qBroader(String parent) {
        return prefixSkos +
                "select ?x ?l where {" +
                "?x skos:broader "+parent+" ." +
                "?x skos:prefLabel ?l ." +
                "     FILTER (lang(?l) = 'en')" +
                "}";
    }

    public static String qSkosLabel(String label) {
        String q = prefixSkos +
                "select ?x ?l ?t where {" +
                "?x skos:prefLabel ?l ." +
                "?x <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?t ." +
                "     FILTER regex(?l, \""+label+"\", \"i\") " +
                "     FILTER (lang(?l) = 'en')" +
                "}";
        System.out.println("Quering: " + q);
        return q;
    }


    public static void main(String[] args) {
        Main mm = new Main();
       // Model model = mm.getFoodModel2();
        Model agrovocModel = mm.getAgrovocModel();
       // mm.printLGDJsonLd(model);
//        mm.printQuery(model, queryString);
//        mm.printQuery(model, queryStringProps23);

        agrovocDataset.begin(READ);
        //mm.printQuery(agrovocModel, qAgrovocRoots);
        mm.printQuery(agrovocModel, qSkosLabel("Vegetables"));
        agrovocDataset.end();
    }


    public OntModel getFoodModel(String file) {
        return getOntModel(new File(getClass().getClassLoader().getResource(file).getFile())
                , OntModelSpec.RDFS_MEM);
    }


    public OntModel getFoodModel1() {
        return getFoodModel(f1);
    }
    public OntModel getFoodModel2() {
        return getFoodModel(f2);
    }
    public OntModel getFoodModel3() {
        return getFoodModel(f3);
    }
    public Model getAgrovocModel() {
        int types = 0;
        System.out.println("will read model");
        agrovocDataset = getAgrovocDataset();
        agrovocDataset.begin(READ) ;
        Model model = agrovocDataset.getDefaultModel();
        System.out.println("======= Agrovoc model read from: agrovoc-tdb-store =======");
        System.out.println("model read: " +  model.getGraph().size());
        agrovocDataset.end();
        return model;
    }

    private void printLGDJsonLd(Model model) {
        JsonObject je = printJsonLd(model);
        try {

            Writer fw = new FileWriter("food.json");
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

    public List<String> printQuery(Model model, String queryStr) {
        List<String> values= new ArrayList<String>();
        Query query= QueryFactory.create(queryStr);
        QueryExecution qe= QueryExecutionFactory.create(query, model);
        ResultSet results= qe.execSelect();

        while (results.hasNext()) {
            QuerySolution row= results.next();
            //  String value= row.getLiteral("name").toString();
            String value= row.toString();
            values.add(value);
            System.out.println(value);
        }
        return values;
    }

}

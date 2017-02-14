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

import static cc.openthings.sync.Common.getOntModel;

public class Main {

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

    public static void main(String[] args) {
        Main mm = new Main();
        Model model = mm.getFoodModel2();
        mm.printLGDJsonLd(model);
        mm.printQuery(model, queryString);
        mm.printQuery(model, queryStringProps23);

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

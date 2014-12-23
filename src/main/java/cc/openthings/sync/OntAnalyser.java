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

import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import java.io.File;

public class OntAnalyser {

    final static String gnOnto = "geonames-ontology_v3.1.rdf";
    final static String gnMappings = "mappings_geonames3.01-all.rdf";


    String queryString= "select * where { ?x <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2002/07/owl#Class> }";
    String queryGNtypes= "select * where { ?x <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.geonames.org/ontology#Code> }";
    String queryGN2LGDmapping= "select * where { ?x <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2002/07/owl#Class> . " +
            "FILTER CONTAINS ( ?x,  <linkedgeodata>) " +
            "}";

    public static void main(String[] args) {
        OntAnalyser ontAnalyser = new OntAnalyser();
        ontAnalyser.analyseGN();
    }

    private void analyseGN() {
        //analyseGNtypes();
        analyseGNmappings();
    }

    private void analyseGNmappings() {
        int mappings = 0;
        ClassLoader classLoader = getClass().getClassLoader();
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
    }


    private void analyseGNtypes() {
        int types = 0;
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(gnOnto).getFile());
        Model model =  getModel(file);

        Query query= QueryFactory.create(queryGNtypes);
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




    public Model getModel(File modelFile) {
        Model model= ModelFactory.createDefaultModel();
        model= model.read(modelFile.getAbsolutePath(), "TTL");
        InfModel rdfs= ModelFactory.createRDFSModel(model);


        return rdfs;
    }

}




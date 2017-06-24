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

package cc.openthings.lodconvert;


import cc.openthings.sync.Common;
import de.uni_stuttgart.vis.vowl.owl2vowl.converter.IRIConverter;
import de.uni_stuttgart.vis.vowl.owl2vowl.export.types.FileExporter;
import io.apptik.json.JsonArray;
import io.apptik.json.JsonObject;
import io.apptik.json.JsonWriter;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.system.JenaSystem;
import org.semanticweb.owlapi.model.IRI;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class Main {

    static {
        JenaSystem.init() ;
    }

    private static String extraHeader = "";
    private static Model model;
    private static JsonArray geoFeatures = new JsonArray();

    public static void main(String[] args) throws Exception {
        if(args == null || args.length<1) {
            System.out.println("Expected at least input file and format parameters");
            System.exit(1);
        }
        String inFile = args[0];
        //parse a FILE
        if(args.length>1) {
            LODIn lodIn = new LODIn(inFile);
            String outFName = args[1];
            genLD(lodIn, outFName);
            lodIn.writeHtml(outFName, extraHeader);
            lodIn.writeGeoJson(outFName);
            lodIn.writeGeoJsonMin(outFName);
        } else {
            //parse a DIR
            model = ModelFactory.createDefaultModel();
            File rootDir = new File(inFile);
            processDir(rootDir);
            writeDump();
            writeGeoJson();
        }

    }

    private static void replaceContext(File dir) throws IOException {
        final String newC = "https://openfooddata.github.io/actors/_common/context.jsonld";
        File[] files = dir.listFiles();
        for(File file:files) {
            if(file.isDirectory()) {
                replaceContext(file);
            } else {
                String fname = file.getName();
                if (fname.endsWith(".jsonld")) {
                    FileReader fr = new FileReader(file);
                    JsonObject j = JsonObject.readFrom(fr).asJsonObject();
                    fr.close();
                    j.put("@context", newC);
                    JsonWriter jw = new JsonWriter(new FileWriter(file));
                    jw.setIndent(" ");
                    j.write(jw);
                    jw.flush();
                    jw.close();

                }
            }
        }
    }

    private static void processDir(File dir) throws IOException {
        File[] files = dir.listFiles();
        for(File file:files) {
            if(file.isDirectory()) {
                processDir(file);
            } else {
                    String fname = file.getName();
                    if (fname.endsWith(".jsonld")) {
                        fname = fname.substring(0, fname.length() - 7);
                        LODIn lodIn = new LODIn(file.getAbsolutePath());
                        model.add(lodIn.model());
                        genLD(lodIn, fname);
                        lodIn.writeHtml(fname, extraHeader);
                        lodIn.writeGeoJson(fname);
                        JsonObject feature = lodIn.writeGeoJsonMin(fname);
                        if(feature!=null) {
                            geoFeatures.add(feature);
                        }
                    }
            }
        }
    }

    public static void genLD(LODIn lodIn, String outFile) throws IOException {
        StringBuilder sb = new StringBuilder();
        StringBuilder sb2 = new StringBuilder();
        ArrayList<Lang> langs = new ArrayList<>();
        langs.add(RDFFormat.RDFXML.getLang());
        langs.add(RDFFormat.JSONLD.getLang());
        langs.add(RDFFormat.NQUADS.getLang());
        langs.add(RDFFormat.NTRIPLES.getLang());
        langs.add(RDFFormat.TURTLE.getLang());
        langs.add(RDFFormat.RDFJSON.getLang());
        langs.add(RDFFormat.TRIG.getLang());
        langs.add(RDFFormat.TRIX.getLang());

        for (Lang lang : langs) {
        //    System.out.println("generating: " + lang);

            lodIn.write(lang, outFile);

            sb.append(
                    "# Rewrite rule to serve " + lang.getLabel() + " content if requested\n" +
                            "RewriteCond %{HTTP_ACCEPT} " + lang.getContentType().toHeaderString().replace("+", "\\+") + "\n" +
                            "RewriteRule ^$ /" + outFile + "." + lang.getFileExtensions().get(0) + " [R=303,L]\n\n");
            sb2.append("<link rel=\"alternate\" href=\"" + outFile + "." + lang.getFileExtensions().get(0) + "\" " +
                    "type=\"" + lang.getContentType().toHeaderString() + "\"/>\n");
        }

        extraHeader = sb2.toString();
       // System.out.print(sb);
       // System.out.print(extraHeader);
    }

    public static void genVowl(String file, String outFile) throws Exception {
        IRI ontologyIri = IRI.create(new File(file));
        try {
            IRIConverter converter = new IRIConverter(ontologyIri);
            //Converter converter = new Converter(ontologyIri);
            converter.convert();
            converter.export(new FileExporter(new File(".out/vowl-"+outFile+".json")));
        } catch (Exception e) {
            System.err.println(e.getClass().getName());
            throw e;
        }
    }


    public static void writeDump() throws IOException {
        File ff = new File(".out/_all.ttl");
        ff.getParentFile().mkdirs();
        FileWriter fw = new FileWriter(ff);
        model.write(fw, RDFFormat.TURTLE.getLang().getName());
        fw.flush();
        fw.close();
    }

    private static void writeGeoJson() throws IOException {
        File ff = new File(".out/_all.geojson");
        ff.getParentFile().mkdirs();
        FileWriter fw = new FileWriter(ff);
        JsonObject geoJson = new JsonObject()
                .put("type", "FeatureCollection")
                .put("features", geoFeatures);
        geoJson.write(new JsonWriter(fw));
        fw.flush();
        fw.close();
    }

    public static void getGeoSparql() throws IOException {
        model = Common.getModel(new File(".out/_all.ttl"), RDFFormat.TURTLE.getLang().getName());

        Query query = QueryFactory.create("select * where { " +
                "?x <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://schema.org/GeoCoordinates> ." +
                "?x <https://schema.org/longitude> ?lon ." +
                "?x <https://schema.org/latitude> ?lat ." +
                "?a <https://schema.org/geo> ?x " +
                "}");
        QueryExecution qe = QueryExecutionFactory.create(query, model);
        ResultSet results = qe.execSelect();

        while (results.hasNext()) {
            QuerySolution row = results.next();
            //  String value= row.getLiteral("name").toString();
            System.out.println("-------------"
                    + row.getResource("a") + ": "
                    + row.getLiteral("lat") + ", " + row.getLiteral("lon")
            );
//            row.get("x").asResource().listProperties()
//                    .forEachRemaining(statement -> System.out.println(statement));


        }
    }

}

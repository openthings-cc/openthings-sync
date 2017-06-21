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
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.adapters.RDFReaderRIOT;
import org.apache.jena.system.JenaSystem;
import org.semanticweb.owlapi.model.IRI;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class Main {

    static {
        JenaSystem.init() ;
    }

    private static String extraHeader = "";
    private static Model model;

    public static void main(String[] args) throws Exception {
        model = Common.getModel(new File(".out/_all.ttl"), RDFFormat.TURTLE.getLang().getName());

        Query query = QueryFactory.create("select * where { ?x <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://schema.org/GeoCoordinates> }");
        QueryExecution qe = QueryExecutionFactory.create(query, model);
        ResultSet results = qe.execSelect();

        while (results.hasNext()) {
            QuerySolution row = results.next();
            //  String value= row.getLiteral("name").toString();
            String value = row.toString();

            System.out.println(value);
        }

        System.exit(0);
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
        } else {
            //parse a DIR
            model = ModelFactory.createDefaultModel();
            File rootDir = new File(inFile);
            processDir(rootDir);
            writeDump();
        }

    }

    private static void processDir(File dir) throws IOException {
        File[] files = dir.listFiles();
        for(File file:files) {
            if(file.isDirectory()) {
                processDir(file);
            } else {
                //TODO DO NOT CATCH fail. remove when initial uri errors are fixed.
                try {
                    String fname = file.getName();
                    if (fname.endsWith(".jsonld")) {
                        fname = fname.substring(0, fname.length() - 7);
                        LODIn lodIn = new LODIn(file.getAbsolutePath());
                        model.add(lodIn.model());
                        genLD(lodIn, fname);
                        lodIn.writeHtml(fname, extraHeader);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
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
            System.out.println("generating: " + lang);

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

    public static void writeGeoJson() throws IOException {
        //model.query("");
    }

}

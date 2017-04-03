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


import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;
import de.uni_stuttgart.vis.vowl.owl2vowl.converter.IRIConverter;
import de.uni_stuttgart.vis.vowl.owl2vowl.export.types.FileExporter;
import io.apptik.json.JsonObject;
import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.semanticweb.owlapi.model.IRI;

import java.io.*;
import java.util.ArrayList;
import java.util.Map;

public class Convert {

    public static void main(String[] args) throws Exception {
        //convert(".onto/ofd/ofd-actors.ttl", "TTL", "JSONLD");
        //  genLD(".onto/ofd-core.ttl", "TTL", "ofd-core");
          genLD(".onto/all_vf.TTL", "TTL", "vf");
        //     convertOnt(".out/all_vf.TTL", "TTL", "TTL");
        //convertOnt(".out/all_vf.TTL", "TTL", "JSONLD");
        // convertOnt2GitBookTable(".out/all_vf.TTL", "TTL");
        //  convert(".out/out.JSONLD", "JSONLD", "TTL");
        // convertToJsonDocs("GeoThingsOnto.jsonld", "JSONLD");
        //convertToVowl(".onto/ofd-core.ttl");
        //  convertToVowl(".onto/ofd/ofd-food.ttl");
       //  convertToVowl(".out/all_vf.TTL");
    }

    static Writer writer;

    public static void write(String str) {
        try {
            writer.write(str);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void genLD(String file, String fromLang, String outFile) throws IOException {
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
            convert(file, fromLang, lang, outFile);
            sb.append(
                    "# Rewrite rule to serve " + lang.getLabel() + " content if requested\n" +
                            "RewriteCond %{HTTP_ACCEPT} " + lang.getContentType().toHeaderString().replace("+", "\\+") + "\n" +
                            "RewriteRule ^$ /" + outFile + "." + lang.getFileExtensions().get(0) + " [R=303,L]\n\n");
            sb2.append("<link rel=\"alternate\" href=\"/" + outFile + "." + lang.getFileExtensions().get(0) + "\" " +
                    "type=\"" + lang.getContentType().toHeaderString() + "\"/>\n");
        }

        System.out.print(sb);
        System.out.print(sb2);
    }

    public static void convertOnt2GitBookTable(String file, String fromLang) {
        writer = new OutputStreamWriter(System.out);

        OntModel model = Common.getOntModel(new File(file).getAbsoluteFile().toString(), OntModelSpec.OWL_MEM, fromLang);
        ExtendedIterator<OntClass> classes = model.listClasses();

        classes.forEachRemaining(Convert::writeClasses);

        try {
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void writeClasses(OntClass ontClass) {
        write(String.format("| %s | %s |\n",
                ontClass.getLocalName(), ""));
        ontClass.listDeclaredProperties().forEachRemaining(Convert::writeProp);

        //ontClass.listDeclaredProperties().forEachRemaining(Convert::writeProp);
    }

    public static void writeProp(OntProperty p) {
        write(String.format("| %s | %s |\n",
                p.getLocalName(), p.getRange()));

    }

    public static void convertOnt(String file, String fromLang, String toLang) throws IOException {
        convert(file, fromLang, toLang, toLang);
    }

    public static void convertOnt(String file, String fromLang, Lang toLang) throws IOException {
        convert(file, fromLang, toLang.getName(), toLang.getFileExtensions().get(0));
    }

    public static void convertOnt(String file, String fromLang, Lang toLang, String outFile) throws IOException {
        convert(file, fromLang, toLang.getName(), toLang.getFileExtensions().get(0), outFile);
    }

    public static void convertOnt(String file, String fromLang, String toLang, String ext) throws IOException {
        convert(file, fromLang, toLang, ext, "out");
    }

    public static void convertOnt(String file, String fromLang, String toLang, String ext, String outFile) throws IOException {
        OntModel model = Common.getOntModel(new File(file).getAbsoluteFile().toString(), OntModelSpec.OWL_MEM, fromLang);
        //        ExtendedIterator<OntClass> classes = model.listClasses();
//        classes.forEachRemaining(System.out::println);
//        ExtendedIterator<OntProperty> props = model.listAllOntProperties();
//        props.forEachRemaining(Convert::unionRangeAndDomainsForProp);
        FileWriter fw = new FileWriter(new File(".out/" + outFile.replace("/", "_") + "." + ext.replace("/", "_")));
        model.write(fw, toLang);
        model.close();
    }

    public static void printOntProp(OntProperty p) {
        System.out.println("\n### " + p + ":");
        ExtendedIterator ress = p.listDomain();
        if (ress != null) {
            System.out.println("    domain: ");
            ress.forEachRemaining(r ->
                    System.out.println("           - " + r.toString()));
        }

        ress = p.listRange();
        if (ress != null) {
            System.out.println("    range: ");
            ress.forEachRemaining(r ->
                    System.out.println("           - " + r.toString()));
        }
    }

    public static void unionRangeAndDomainsForProp(OntProperty p) {
        unionRangeAndDomainsForProp(p, true);
    }

    public static void unionRangeAndDomainsForProp(OntProperty p, boolean splitProps) {
        System.out.println("\n### " + p + ":");
        ExtendedIterator<? extends OntResource> ress = p.listDomain();
        if (ress != null && ress.toSet().size() > 1) {
            ress = p.listDomain();
            ArrayList<RDFNode> nodes = new ArrayList<>();
            System.out.println("    domain: ");
            while (ress.hasNext()) {
                RDFNode r = (RDFNode) ress.removeNext();
                System.out.println("           - " + r.toString());
                nodes.add(r);
                if (splitProps) {
                    ExtendedIterator<? extends OntResource> ranges = p.listRange();
                    if (!ranges.hasNext()) {
                        OntProperty nProp = p.getOntModel().createOntProperty(p.getURI() + "_" + ((Resource) r).getLocalName());
                        nProp.addDomain((Resource) r);
                        nProp.addSuperProperty(p);
                    } else {
                        while (ranges.hasNext()) {
                            Resource rRange = ranges.removeNext();
                            OntProperty nProp = p.getOntModel().createOntProperty(p.getURI()
                                    + "_" + ((Resource) r).getLocalName()
                                    + "_" + (rRange).getLocalName());
                            nProp.addDomain((Resource) r);
                            nProp.addRange(rRange);
                            nProp.addSuperProperty(p);
                        }
                    }
                }
            }
            p.addDomain(p.getOntModel().createUnionClass(p.getURI() + "_domainuc",
                    p.getModel().createList(nodes.toArray(new RDFNode[]{}))));
        }

        ress = p.listRange();
        if (ress != null && ress.toSet().size() > 1) {
            ress = p.listRange();
            ArrayList<RDFNode> nodes = new ArrayList<>();
            System.out.println("    range: ");
            while (ress.hasNext()) {
                RDFNode r = (RDFNode) ress.removeNext();
                System.out.println("           - " + r.toString());
                nodes.add(r);
                if (splitProps) {
                    OntProperty nProp = p.getOntModel().createOntProperty(p.getURI() + "_" + ((Resource) r).getLocalName());
                    nProp.addRange((Resource) r);
                    //if still here we gace single domain
                    nProp.addDomain(p.getDomain());
                    nProp.addSuperProperty(p);
                }
            }
            p.addRange(p.getOntModel().createUnionClass(p.getURI() + "_rangeuc",
                    p.getModel().createList(nodes.toArray(new RDFNode[]{}))));
        }
    }

    public static void convert(String file, String fromLang, String toLang) throws IOException {
        convert(file, fromLang, toLang, toLang);
    }

    public static void convert(String file, String fromLang, Lang toLang) throws IOException {
        convert(file, fromLang, toLang.getName(), toLang.getFileExtensions().get(0));
    }

    public static void convert(String file, String fromLang, Lang toLang, String outFile) throws IOException {
        convert(file, fromLang, toLang.getName(), toLang.getFileExtensions().get(0), outFile);
    }

    public static void convert(String file, String fromLang, String toLang, String ext) throws IOException {
        convert(file, fromLang, toLang, ext, "out");
    }

    public static void convert(String file, String fromLang, String toLang, String ext, String outFile) throws IOException {
        Model model = Common.getModel(new File(file), fromLang);
//       InfModel inf = ModelFactory.createInfModel( ReasonerRegistry.getOWLReasoner(), model);
//       inf.validate();
        FileWriter fw = new FileWriter(new File(".out/" + outFile.replace("/", "_") + "." + ext.replace("/", "_")));
        model.write(fw, toLang);
        model.close();
    }

    public static void convertToJsonDocs(String file, String fromLang) throws IOException, JsonLdError {
        Model model = Common.getModel(new File(file), fromLang);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        model.write(os, "JSON-LD");
        InputStream is = new ByteArrayInputStream(os.toByteArray());
        Object jObj = JsonUtils.fromInputStream(is);
        ArrayList<Object> expjObj = (ArrayList<Object>) JsonLdProcessor.expand(jObj);
        for (Object el : expjObj) {
            JsonObject job = new JsonObject((Map) el);
            FileWriter fw = new FileWriter(
                    ".out/jsonld/" + UriUtils.getSchemaId(job.get("@id").asString() + ".json"));
            job.writeTo(fw);
            fw.close();
        }
    }

    public static void convertToVowl(String file) throws Exception {
        IRI ontologyIri = IRI.create(new File(file));
        try {
            IRIConverter converter = new IRIConverter(ontologyIri);
            //Converter converter = new Converter(ontologyIri);
            converter.convert();
            converter.export(new FileExporter(new File(".out/out.json")));
        } catch (Exception e) {
            System.err.println(e.getClass().getName());
            throw e;
        }
    }
}

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
import cc.openthings.sync.UriUtils;
import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;
import io.apptik.json.JsonObject;
import openllet.jena.PelletReasonerFactory;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.riot.Lang;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.io.StringDocumentTarget;
import org.semanticweb.owlapi.model.*;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

public class LODIn {

    final Model model;

    public LODIn(String file, String fromLang) {
        this(file, fromLang, false);
    }


    public LODIn(String file, String fromLang, boolean isOntology) {
        if (isOntology) {
            model = Common.getOntModel(file, OntModelSpec.OWL_MEM, fromLang);
        } else {
            model = Common.getModel(new File(file), fromLang);
        }
        if (model == null) {
            throw new RuntimeException("Error parsing");
        }
    }

    public Model model() {
        return model;
    }

    public void write(String toLang) throws IOException {
        write(toLang, toLang);
    }

    public void write(Lang toLang) throws IOException {
        write(toLang.getName(), toLang.getFileExtensions().get(0));
    }

    public void write(Lang toLang, String outFile) throws IOException {
        write(toLang.getName(), toLang.getFileExtensions().get(0), outFile);
    }

    public void write(String toLang, String ext) throws IOException {
        write(toLang, ext, "out");
    }

    public void write(String toLang, String ext, String outFile) throws IOException {
//       InfModel inf = ModelFactory.createInfModel( ReasonerRegistry.getOWLReasoner(), model);
//       inf.validate();
        FileWriter fw = new FileWriter(new File(".out/" + outFile.replace("/", "_") + "." + ext.replace("/", "_")));
        model.write(fw, toLang);
        fw.flush();
        fw.close();
    }

    public void writeToJsonDocs() throws IOException, JsonLdError {
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


    public void writeOntologyHtml(String outFile, String ontologyDocUrl) {
        String inn;

        try {
            //inn = getOntoWithJenaPellet();
            inn = getOntoWithOWLAPI();
            applyXSLTTransformation(inn, ontologyDocUrl, "en", outFile);

        } catch (Exception e) {
            throw new RuntimeException("Error creating HTML", e);
        }
    }

    // from https://github.com/essepuntato/LODE
    private void applyXSLTTransformation(String source, String ontologyUrl, String lang, String outFile) throws TransformerException, IOException {
        String xsltURL = "https://rawgit.com/essepuntato/LODE/master/src/main/webapp/extraction.xsl";
        String cssLocation = "https://rawgit.com/essepuntato/LODE/master/src/main/webapp/";

        TransformerFactory tfactory = new net.sf.saxon.TransformerFactoryImpl();

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        Transformer transformer = tfactory.newTransformer(new StreamSource(xsltURL));

        transformer.setParameter("css-location", cssLocation);
        transformer.setParameter("lang", lang);
        transformer.setParameter("ontology-url", ontologyUrl);
        transformer.setParameter("source", cssLocation + "source");

        StreamSource inputSource = new StreamSource(new StringReader(source));

        FileWriter fw = new FileWriter(new File(".out/" + outFile.replace("/", "_") + ".html"));

        transformer.transform(inputSource, new StreamResult(fw));
    }

    private String getOntoWithJenaPellet() {
        String result;
        Writer stringWriter = new StringWriter();

        final Reasoner reasoner = PelletReasonerFactory.theInstance().create();

        InfModel infModel = ModelFactory.createInfModel(reasoner, model);

        // Create the result stream for the transform

        infModel.write(stringWriter, "RDFXML");

        try {
            stringWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            result = stringWriter.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error creating HTML", e);
        }
        return result;
    }

    private String getOntoWithOWLAPI() throws OWLOntologyStorageException, IOException, OWLOntologyCreationException {
        String result;
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

        OWLOntology ontology;

        PipedInputStream in = new PipedInputStream();
        PipedOutputStream out = new PipedOutputStream(in);

        new Thread(
                () -> {
                    model.write(out);
                    try {
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
        ).start();

        ontology = manager.loadOntologyFromOntologyDocument(in);
        //ontology = parseWithReasoner(manager, ontology);
        StringDocumentTarget parsedOntology = new StringDocumentTarget();

        manager.saveOntology(ontology, new RDFXMLOntologyFormat(), parsedOntology);
        result = parsedOntology.toString();

        return result;
    }

//    /*
//	 * private String removeImportedAxioms(String result, List<String>
//	 * removedImport) { DocumentBuilderFactory factory =
//	 * DocumentBuilderFactory.newInstance(); factory.setNamespaceAware(true);
//	 * try { DocumentBuilder builder = factory.newDocumentBuilder(); Document
//	 * document = builder.parse(new ByteArrayInputStream(result.getBytes()));
//	 *
//	 * NodeList ontologies =
//	 * document.getElementsByTagNameNS("http://www.w3.org/2002/07/owl#",
//	 * "Ontology"); for (int i = 0; i < ontologies.getLength() ; i++) { Element
//	 * ontology = (Element) ontologies.item(i);
//	 *
//	 * NodeList children = ontology.getChildNodes(); List<Element> removed = new
//	 * ArrayList<Element>(); for (int j = 0; j < children.getLength(); j++) {
//	 * Node child = children.item(j);
//	 *
//	 * if ( child.getNodeType() == Node.ELEMENT_NODE &&
//	 * child.getNamespaceURI().equals("http://www.w3.org/2002/07/owl#") &&
//	 * child.getLocalName().equals("imports")) { removed.add((Element) child); }
//	 * }
//	 *
//	 * for (Element toBeRemoved : removed) {
//	 * removedImport.add(toBeRemoved.getAttributeNS(
//	 * "http://www.w3.org/1999/02/22-rdf-syntax-ns#", "resource"));
//	 * ontology.removeChild(toBeRemoved); } }
//	 *
//	 * Transformer transformer =
//	 * TransformerFactory.newInstance().newTransformer(); StreamResult output =
//	 * new StreamResult(new StringWriter()); DOMSource source = new
//	 * DOMSource(document); transformer.transform(source, output);
//	 *
//	 * return output.getWriter().toString(); } catch
//	 * (ParserConfigurationException e) { return result; } catch (SAXException
//	 * e) { return result; } catch (IOException e) { return result; } catch
//	 * (TransformerConfigurationException e) { return result; } catch
//	 * (TransformerFactoryConfigurationError e) { return result; } catch
//	 * (TransformerException e) { return result; } }
//	 */
//
//    private OWLOntology parseWithReasoner(OWLOntologyManager manager, OWLOntology ontology) {
//        try {
//            PelletOptions.load(new URL("http://" + cssLocation + "pellet.properties"));
//            final PelletReasoner reasoner = PelletReasonerFactory.theInstance().create();
//
//            reasoner.getKB().prepare();
//            List<InferredAxiomGenerator<? extends OWLAxiom>> generators = new ArrayList<InferredAxiomGenerator<? extends OWLAxiom>>();
//            generators.add(new InferredSubClassAxiomGenerator());
//            generators.add(new InferredClassAssertionAxiomGenerator());
//            generators.add(new InferredDisjointClassesAxiomGenerator());
//            generators.add(new InferredEquivalentClassAxiomGenerator());
//            generators.add(new InferredEquivalentDataPropertiesAxiomGenerator());
//            generators.add(new InferredEquivalentObjectPropertyAxiomGenerator());
//            generators.add(new InferredInverseObjectPropertiesAxiomGenerator());
//            generators.add(new InferredPropertyAssertionGenerator());
//            generators.add(new InferredSubDataPropertyAxiomGenerator());
//            generators.add(new InferredSubObjectPropertyAxiomGenerator());
//
//            InferredOntologyGenerator iog = new InferredOntologyGenerator(reasoner, generators);
//
//            OWLOntologyID id = ontology.getOntologyID();
//            Set<OWLImportsDeclaration> declarations = ontology.getImportsDeclarations();
//            Set<OWLAnnotation> annotations = ontology.getAnnotations();
//
//            Map<OWLEntity, Set<OWLAnnotationAssertionAxiom>> entityAnnotations = new HashMap<OWLEntity, Set<OWLAnnotationAssertionAxiom>>();
//            for (OWLClass aEntity : ontology.getClassesInSignature()) {
//                entityAnnotations.put(aEntity, aEntity.getAnnotationAssertionAxioms(ontology));
//            }
//            for (OWLObjectProperty aEntity : ontology.getObjectPropertiesInSignature()) {
//                entityAnnotations.put(aEntity, aEntity.getAnnotationAssertionAxioms(ontology));
//            }
//            for (OWLDataProperty aEntity : ontology.getDataPropertiesInSignature()) {
//                entityAnnotations.put(aEntity, aEntity.getAnnotationAssertionAxioms(ontology));
//            }
//            for (OWLNamedIndividual aEntity : ontology.getIndividualsInSignature()) {
//                entityAnnotations.put(aEntity, aEntity.getAnnotationAssertionAxioms(ontology));
//            }
//            for (OWLAnnotationProperty aEntity : ontology.getAnnotationPropertiesInSignature()) {
//                entityAnnotations.put(aEntity, aEntity.getAnnotationAssertionAxioms(ontology));
//            }
//            for (OWLDatatype aEntity : ontology.getDatatypesInSignature()) {
//                entityAnnotations.put(aEntity, aEntity.getAnnotationAssertionAxioms(ontology));
//            }
//
//            manager.removeOntology(ontology);
//            OWLOntology inferred = manager.createOntology(id);
//            iog.fillOntology(manager, inferred);
//
//            for (OWLImportsDeclaration decl : declarations) {
//                manager.applyChange(new AddImport(inferred, decl));
//            }
//            for (OWLAnnotation ann : annotations) {
//                manager.applyChange(new AddOntologyAnnotation(inferred, ann));
//            }
//            for (OWLClass aEntity : inferred.getClassesInSignature()) {
//                applyAnnotations(aEntity, entityAnnotations, manager, inferred);
//            }
//            for (OWLObjectProperty aEntity : inferred.getObjectPropertiesInSignature()) {
//                applyAnnotations(aEntity, entityAnnotations, manager, inferred);
//            }
//            for (OWLDataProperty aEntity : inferred.getDataPropertiesInSignature()) {
//                applyAnnotations(aEntity, entityAnnotations, manager, inferred);
//            }
//            for (OWLNamedIndividual aEntity : inferred.getIndividualsInSignature()) {
//                applyAnnotations(aEntity, entityAnnotations, manager, inferred);
//            }
//            for (OWLAnnotationProperty aEntity : inferred.getAnnotationPropertiesInSignature()) {
//                applyAnnotations(aEntity, entityAnnotations, manager, inferred);
//            }
//            for (OWLDatatype aEntity : inferred.getDatatypesInSignature()) {
//                applyAnnotations(aEntity, entityAnnotations, manager, inferred);
//            }
//
//            return inferred;
//        } catch (FileNotFoundException e1) {
//            return ontology;
//        } catch (MalformedURLException e1) {
//            return ontology;
//        } catch (IOException e1) {
//            return ontology;
//        } catch (OWLOntologyCreationException e) {
//            return ontology;
//        }
//    }

    private void applyAnnotations(OWLEntity aEntity, Map<OWLEntity, Set<OWLAnnotationAssertionAxiom>> entityAnnotations, OWLOntologyManager manager, OWLOntology ontology) {
        Set<OWLAnnotationAssertionAxiom> entitySet = entityAnnotations.get(aEntity);
        if (entitySet != null) {
            for (OWLAnnotationAssertionAxiom ann : entitySet) {
                manager.addAxiom(ontology, ann);
            }
        }
    }

}

package cc.openthings.sync;


//import com.hp.hpl.jena.query.*;
//import com.hp.hpl.jena.rdf.model.InfModel;
//import com.hp.hpl.jena.rdf.model.Model;
//import com.hp.hpl.jena.rdf.model.ModelFactory;


import javax.management.Query;
import java.util.ArrayList;
import java.util.List;

public class PrintTtlOntology {

    String ttlUrl = "/home/djodjo/Downloads/2014-09-09-ontology.sorted.nt";
 //   String ttlUrl = "/home/djodjo/Downloads/2014-09-09-HistoricThing.node.sorted.nt";
//    String queryString= "select * where { ?x <http://www.w3.org/2000/01/rdf-schema#label> ?name }";
//<http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2002/07/owl#Class>
String queryString= "select * where { ?x <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2002/07/owl#Class> }";


    public static void main(String[] args) {
        PrintTtlOntology printTtlOntology= new PrintTtlOntology();
        Model ontologyAndData= printTtlOntology.getModel();
        System.out.println(printTtlOntology.getNames(ontologyAndData).toString());
        System.out.println(printTtlOntology.getNames(ontologyAndData).size());
    }


    public Model getModel() {
        Model model= ModelFactory.createDefaultModel();
        model= model.read(ttlUrl, "TTL");
        InfModel rdfs= ModelFactory.createRDFSModel(model);
       // OntModel rdfs= ModelFactory.createOntologyModel(OntModelSpec.assemble(model));

        return rdfs;
    }

    public List<String> getNames(Model model) {
        List<String> values= new ArrayList<String>();
        Query query= QueryFactory.create(queryString);
        QueryExecution qe= QueryExecutionFactory.create(query, model);
        ResultSet results= qe.execSelect();

        while (results.hasNext()) {
            QuerySolution row= results.next();
          //  String value= row.getLiteral("name").toString();
            String value= row.toString();
            values.add(value);
        }
        return values;
    }

}

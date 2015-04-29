package cc.openthings.sync;


import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import org.djodjo.json.JsonObject;

import java.util.Iterator;

import static cc.openthings.sync.Common.getOntModel;

public class AssetsGenerator {

    OntModel model;

    public AssetsGenerator() {
        model = getOpenThingsModel();
    }

    public static void main(String[] args) {
        AssetsGenerator assetsGenerator = new AssetsGenerator();
        //assetsGenerator.analyseTypes();
        assetsGenerator.genTypesMenu();
    }

    private void genTypesMenu() {
       ontoClassesToTypesMenu(model);
    }

    private void analyseTypes() {
        Common.ontoClassesToJson(model);
    }

    public OntModel getOpenThingsModel() {

        return getOntModel(Common.openThingsOnto);

    }


    public JsonObject ontoClassesToTypesMenu(OntModel ontModel) {
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

            typesTree.merge(ontoClassesToTypesMenu(ontClass, new JsonObject()));

        }
        System.out.println( "======= " + counter + " =======");
        System.out.println(typesTree.toString());
        System.out.println( "======= " + Common.countElements(typesTree) + " =======");

        return typesTree;
    }




    public  JsonObject ontoClassesToTypesMenu(OntClass ontClass, JsonObject subJson) {
        JsonObject res = new JsonObject();
        String name =  ontClass.getLocalName();
        //.asComplementClass().getRDFType().getLocalName();
        //System.out.println("\t>>>>\t" + name);

        Resource superClass = null;
        NodeIterator superClasses = ontClass.listPropertyValues(ontClass.getProfile().SUB_CLASS_OF());

        if(!superClasses.hasNext()) {
            return new JsonObject().put(name, subJson);
        }

        while(superClasses.hasNext()) {
            try {
                //superClass = superClasses.next().asResource();
                OntClass sClass = superClasses.next().as(OntClass.class);

                //if(superClass.getNameSpace().equals("http://schema.org/")) break;
                System.out.println("\t\t\t -- " + sClass);
                res.merge(ontoClassesToTypesMenu((OntClass) sClass, new JsonObject().put(name, subJson)));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return res;

    }

}

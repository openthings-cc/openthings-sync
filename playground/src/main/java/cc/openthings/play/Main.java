package cc.openthings.play;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.xmljson.XmlJsonDataFormat;
import org.apache.camel.impl.DefaultCamelContext;
import org.json.JSONObject;
import org.json.XML;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Scanner;


public class Main extends RouteBuilder {
    public static void main(String[] args) {
        Main mm = new Main();
        mm.xml2Json1();
       // mm.xml2Json2();

    }

    public void xml2Json1() {
        CamelContext context = new DefaultCamelContext();

        try {
            context.addRoutes(this);
            context.start();

            Thread.sleep(300000);

            context.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    public void xml2Json2() {
        String xmlString = getFile("xml/pivot_querier_result_55092b6253b33.json");
        JSONObject jsonOut = XML.toJSONObject(xmlString);
        //System.out.println(jsonOut);
        Writer fw = null;
        try {
            fw = new FileWriter("JsonOut2.json");

        jsonOut.write(fw);
        fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void configure() throws Exception {
        XmlJsonDataFormat xmlJsonFormat = new XmlJsonDataFormat();
        xmlJsonFormat.setForceTopLevelObject(true);


      //  from("file:resource/xml")
        from("file:playground/src/main/resources/xml")
                .marshal(xmlJsonFormat)
                //.unmarshal()
                //  .csv()
               //.json()
               .to("file:playground/src/main/resources/json");
       // .to("file:resource/json");
    }

    private String getFile(String fileName) {

        StringBuilder result = new StringBuilder("");

        //Get file from resources folder
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(fileName).getFile());

        try (Scanner scanner = new Scanner(file)) {

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                result.append(line).append("\n");
            }

            scanner.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return result.toString();

    }
}

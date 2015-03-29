package cc.openthings.estools;

import org.djodjo.json.JsonElement;
import org.djodjo.json.JsonObject;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStreamReader;

public class TestEsInput {

    @Test
    public void testIn() throws IOException {
        JsonObject job = JsonElement.readFrom(new InputStreamReader(getClass().getResourceAsStream("pivot1.json"))).asJsonObject();

        EsClient esClient =  new EsClient("", "");
        esClient.writeArray2es("hachkaindex", "pivot", job.getJsonObject("pivot").getJsonArray("offre"));
    }

}

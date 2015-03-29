package cc.openthings.estools;


import org.djodjo.json.JsonArray;
import org.djodjo.json.JsonElement;
import org.djodjo.json.JsonObject;
import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

import java.util.List;

public class EsClient {

    TransportClient tc = null;

    public EsClient(String clusterName, String nodeName) {
        init();
    }

    public void init() {
        tc = new TransportClient(
                ImmutableSettings.settingsBuilder()
                         .put("cluster.name", "Ardennes-Hicking")
                        //.put("node.name", "djodjo-UWS")
                        .build());
        tc.addTransportAddress(new InetSocketTransportAddress("localhost",
                9300));
        List<DiscoveryNode> nodes = tc.listedNodes();
        System.out.println("NODES: ");
        for (DiscoveryNode n : nodes) {
            System.out.println("node: " + n);
        }
        List<DiscoveryNode> nodes2 = tc.connectedNodes();
        System.out.println("CONNECTED NODES: ");
        for (DiscoveryNode n : nodes2) {
            System.out.println("node: " + n);
        }
        //  --set geo mapping
//        final IndicesExistsResponse res = tc.admin().indices().prepareExists(indexName).execute().actionGet();
//        if (!res.isExists())
//        {
//            CreateIndexRequestBuilder cirb =  tc.admin().indices().prepareCreate(indexName);
//
//                cirb.addMapping(typeName,"{\n" +
//                        "\"properties\":{" +
//                        "\"coordinates\":{\n" +
//                        "\"type\": \"geo_point\",\n" +
//                        "\"lat_lon\": true\n" +
//                        "}" +
//                        "}}");
//
//            cirb.execute().actionGet();
//        }
/////////
    }

    public void writeArray2es(String indexName, String typeName, JsonArray jsonArray) {
        if (indexName == null || indexName.isEmpty() || jsonArray == null) return;
        for (JsonElement je : jsonArray) {
            if (je.isJsonObject()) {
                writeObject2es(indexName, typeName, null, je.asJsonObject());
            }
        }
    }

    public void writeObject2es(String indexName, String typeName, String objectId, JsonObject jsonObject) {
        IndexRequestBuilder irb;
        if (objectId == null || objectId.isEmpty() || jsonObject == null) {
             irb = tc
                    .prepareIndex(indexName, typeName);
        } else {
            //
             irb = tc
                    .prepareIndex(indexName, typeName, objectId);
        }
        irb.setSource(jsonObject.toString());

        final ListenableActionFuture<IndexResponse> laf = irb.execute();
        final IndexResponse response = laf.actionGet();
        System.out.println("======= Written idx:" + response.getId() + " :: " + response.getIndex() + " :: " + response.getType() + " :: " + response.getVersion() + " =======");
    }
}

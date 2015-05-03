package cc.openthings.sync.osm;

import org.mapsforge.core.model.BoundingBox;

import java.util.ArrayList;

public class Config {

    private Config() {

    }



    public static final String osmOverpassBaseUrl2 = "http://overpass.osm.rambler.ru/cgi/interpreter?data=[out:json];";
    public static final String osmOverpassBaseUrl = "http://overpass-api.de/api/interpreter?data=[out:json];";


    public static String getBoundigboxString(double minLatitude, double minLongitude, double maxLatitude, double maxLongitude) {
        BoundingBox bounds = new BoundingBox(minLatitude, minLongitude, maxLatitude, maxLongitude);
        return getBoundigboxString(bounds);
    }
        public static String getBoundigboxString(BoundingBox bounds) {
        return "(" + bounds.minLatitude
                + "," + bounds.minLongitude
                + "," + bounds.maxLatitude
                + "," + bounds.maxLongitude + ");";
    }

    public static String buildQuery(ArrayList<String> kvs, BoundingBox bounds, Config.QueryType qType) {
       if(QueryType.node.equals(qType)) {
           return buildNodeQuery(kvs, bounds);
       } else if(QueryType.way.equals(qType)) {
           return buildWayQuery(kvs, bounds);
       }
       else return "";
    }
    public static String buildNodeQuery(ArrayList<String> kvs, BoundingBox bounds) {
        StringBuilder res = new StringBuilder();
        String bbox = getBoundigboxString(bounds);
        res.append("(");
        for (String kv : kvs) {
            res.append("node" +kv).append(bbox);
        }
        res.append(");" + "out%20500;");
        return res.toString();
    }

    public static String buildWayQuery(ArrayList<String> kvs, BoundingBox bounds) {
        StringBuilder res = new StringBuilder();
        String bbox = getBoundigboxString(bounds);
        res.append("(");
        for (String kv : kvs) {
            res.append("way" +kv).append(bbox);
        }
        res.append(");" + "out%20500;");
        return res.toString();
    }
    public enum QueryType {
        node, way, relation
    }
}

package cc.openthings.sync.osm.model;


import java.util.ArrayList;

public class OPQuery {

    private int id = -1;
    //mins
    private Coordinate southwest;
    //maxes
    private Coordinate northeast;
    ArrayList<String> kvs = new ArrayList<>();
    int maxResults = 500;
    QueryType qType = QueryType.node;

    public OPQuery(int id) {
        this.id = id;
    }

    public OPQuery(Coordinate centroid, double kilometersWidth) {
        long width = (long) (6378.1d / kilometersWidth);
        long height = width;

        double latitudeStep = 180d / (double) height;
        double longitudeStep = 360d / (double) width;

        northeast = new Coordinate(centroid.getLatitude() - latitudeStep, centroid.getLongitude() + longitudeStep);
        southwest = new Coordinate(centroid.getLatitude() + latitudeStep, centroid.getLongitude() - latitudeStep);
    }

    //todo verify 'em
    public OPQuery(Coordinate southwest, Coordinate northeast) {
        this.southwest = southwest;
        this.northeast = northeast;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OPQuery envelope = (OPQuery) o;

        if (northeast != null ? !northeast.equals(envelope.northeast) : envelope.northeast != null) return false;
        if (southwest != null ? !southwest.equals(envelope.southwest) : envelope.southwest != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = southwest != null ? southwest.hashCode() : 0;
        result = 31 * result + (northeast != null ? northeast.hashCode() : 0);
        return result;
    }

    public Coordinate getSouthwest() {
        return southwest;
    }

    public OPQuery setSouthwest(Coordinate southwest) {
        this.southwest = southwest;
        return this;
    }

    public Coordinate getNortheast() {
        return northeast;
    }

    public OPQuery setNortheast(Coordinate northeast) {
        this.northeast = northeast;
        return this;
    }

    public int getMaxResults() {
        return maxResults;
    }

    public OPQuery setMaxResults(int maxResults) {
        this.maxResults = maxResults;
        return this;
    }

    public OPQuery addKV(String kv) {
        kvs.add(kv);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder res = new StringBuilder("data=[out:json];");
        //kv should look like [prop]  or [prop=value] ...
        for (String kv : kvs) {
            res.append(qType + kv)
                    .append("(")
                    .append(southwest.getLatitude())
                    .append(southwest.getLongitude())
                    .append(northeast.getLatitude())
                    .append(northeast.getLongitude())
                    .append(");");
        }

        res.append("out " + maxResults + ";");
        return res.toString();
    }


    public enum QueryType {
        node, way, relation
    }

}

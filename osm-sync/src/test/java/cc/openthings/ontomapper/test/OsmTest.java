package cc.openthings.ontomapper.test;

import cc.openthings.sync.osm.OsmReader;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;

public class OsmTest {


    @Test
    public void testOsmPbfMapper() throws FileNotFoundException {
        File pbfFile = new File(getClass().getClassLoader().getResource("malta-latest.osm.pbf").getPath());
        System.out.println("processing: " + pbfFile.getPath());
        OsmReader reader = new OsmReader();
        reader.readOsm(pbfFile);
    }

//    @Test
//    public void testOsmBz2fMapper() throws FileNotFoundException {
//        File pbfFile = new File(getClass().getClassLoader().getResource("malta-latest.osm.bz2").getPath());
//        System.out.println("processing: " + pbfFile.getPath());
//        OsmMapper mapper = new OsmMapper();
//        mapper.readOsm(pbfFile);
//    }

}

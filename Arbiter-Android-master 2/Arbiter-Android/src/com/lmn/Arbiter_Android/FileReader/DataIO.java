package com.lmn.Arbiter_Android.FileReader;


import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FileDataStoreFactorySpi;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.SchemaException;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;


public class DataIO {

    // Read SHP File
    public SimpleFeatureCollection readSHP(String filePath) {

        File file = new File(filePath);
        Map<String, Object> map = new HashMap<String, Object>();

        DataStore dataStore;
        String typeName;

        SimpleFeatureCollection collection = null;
        try {
            map.put("url", file.toURI().toURL());
            Log.d("map?",file.toURI().toURL()+"");

            //error
            dataStore = DataStoreFinder.getDataStore(map);
            //error

            typeName = dataStore.getTypeNames()[0];

            SimpleFeatureSource source = dataStore.getFeatureSource(typeName);
            Filter filter = Filter.INCLUDE; // ECQL.toFilter("BBOX(THE_GEOM, 10,20,30,40)")
            collection = source.getFeatures(filter);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return collection;
    }

    // Write SHP File
    public void writeSHP(SimpleFeatureCollection simpleFeatureCollection, String filePath) throws IOException, SchemaException {

        FileDataStoreFactorySpi factory = new ShapefileDataStoreFactory();

        File file = new File(filePath);
        Map map = Collections.singletonMap("url", file.toURI().toURL());

        DataStore myData = factory.createNewDataStore(map);

        SimpleFeatureType featureType = simpleFeatureCollection.getSchema();
        myData.createSchema(featureType);

        Transaction transaction = new DefaultTransaction("create");

        String typeName = myData.getTypeNames()[0];
        SimpleFeatureSource featureSource = myData.getFeatureSource(typeName);

        if (featureSource instanceof SimpleFeatureStore) {
            SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
            featureStore.setTransaction(transaction);
            try {
                featureStore.addFeatures(simpleFeatureCollection);
                transaction.commit();
            } catch (Exception e) {
                e.printStackTrace();
                transaction.rollback();
            } finally {
                transaction.close();
            }
            System.out.println("Success!");
            System.exit(0);
        } else {
            System.out.println(typeName + " does not support read/write access");
            System.exit(1);
        }
    }
}
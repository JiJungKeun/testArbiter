package com.lmn.Arbiter_Android.FileReader;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geojson.geom.GeometryJSON;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class ConvertService {

    // Convert SimpleFeatureCollection To GeoJSON
    public JSONObject convertToGeoJSON(SimpleFeatureCollection simpleFeatureCollection) {

        return buildFeatureCollection(simpleFeatureCollection);

    }

    @SuppressWarnings("unchecked")
    private JSONObject buildFeatureCollection(SimpleFeatureCollection featureCollection) {

        List<JSONObject> features = new LinkedList<JSONObject>();
        JSONObject obj = new JSONObject();
        obj.put("type", "FeatureCollection");
        obj.put("features", features);
        SimpleFeatureIterator simpleFeatureIterator = featureCollection.features();

        int i = 0;
        while (simpleFeatureIterator.hasNext()) {
            SimpleFeature simpleFeature = simpleFeatureIterator.next();
            features.add(buildFeature(simpleFeature));
            if (i == 0) {
                obj.put("propertyType", buildPropertiesType(simpleFeature));
                i++;
            }
        }
        return obj;
    }

    @SuppressWarnings("unchecked")
    private JSONObject buildFeature(SimpleFeature simpleFeature) {

        JSONObject obj = new JSONObject();
        obj.put("type", "Feature");
        obj.put("id", simpleFeature.getID());
        obj.put("geometry", buildGeometry((Geometry) simpleFeature.getDefaultGeometry()));
        obj.put("properties", buildProperties(simpleFeature));
        return obj;

    }

    @SuppressWarnings("unchecked")
    private JSONObject buildProperties(SimpleFeature simpleFeature) {

        JSONObject obj = new JSONObject();
        Collection<Property> properties = simpleFeature.getProperties();

        for (Property property : properties) {
            obj.put(property.getName().toString(), property.getValue() == null ? "" : property.getValue().toString());
        }
        return obj;
    }

    private List<String> buildPropertiesType(SimpleFeature simpleFeature) {

        Collection<Property> properties = simpleFeature.getProperties();

        List<String> typeArray = new ArrayList<String>();
        for (Property property : properties) {
            String tempType = property.getType().toString();
            int firstIndex = tempType.indexOf("<");
            int lastIndex = tempType.lastIndexOf(">");
            String propertyType = tempType.substring(firstIndex + 1, lastIndex);
            typeArray.add(propertyType);
        }
        return typeArray;
    }

    private JSONObject buildGeometry(Geometry geometry) {

        GeometryJSON gjson = new GeometryJSON();
        Object obj = JSONValue.parse(gjson.toString(geometry));
        JSONObject jsonObj = (JSONObject) obj;
        return jsonObj;
    }

    // Convert GeoJSON To SimpleFeatureCollection
    public SimpleFeatureCollection converToSimpleFeatureCollection(JSONObject geo) throws SchemaException {

        return buildFeatureCollection(geo);

    }

    @SuppressWarnings("rawtypes")
    private SimpleFeatureCollection buildFeatureCollection(JSONObject geo) throws SchemaException {

        DefaultFeatureCollection defaultFeatureCollection = new DefaultFeatureCollection();
        JSONArray features = (JSONArray) geo.get("features");

        for (int i = 0; i < features.size(); i++) {

            JSONObject feature = (JSONObject) features.get(i);

            ArrayList layerKeyChain = (ArrayList) geo.get("layerKeyChain");
            ArrayList layerTypeChain = (ArrayList) geo.get("layerTypeChain");

            if (layerKeyChain != null) {
                SimpleFeature simpleFeature = buildFeature(feature, layerKeyChain, layerTypeChain);
                defaultFeatureCollection.add(simpleFeature);
            } else {
                SimpleFeature simpleFeature = buildFeature(feature);
                defaultFeatureCollection.add(simpleFeature);
            }
        }
        return defaultFeatureCollection;
    }

    @SuppressWarnings("rawtypes")
    private SimpleFeature buildFeature(JSONObject feature, ArrayList layerKeyChain, ArrayList layerTypeChain) throws SchemaException {

        Geometry geometry = buildGeometry(feature);
        JSONObject property = (JSONObject) feature.get("properties");
        String featureID = (String) feature.get("id");

        String geometryType = geometry.getGeometryType();

        // SimpleFeature
        SimpleFeatureType simpleFeatureType = null;
        SimpleFeature simpleFeature = null;

        int size = layerKeyChain.size() + 1;
        Object[] objects;
        objects = new Object[size];
        objects[0] = geometry;

        int j = 1, a = 0;
        String temp = "";
        Iterator iterator = layerKeyChain.iterator();
        while (iterator.hasNext()) {

            String key = (String) iterator.next();
            String value = (String) property.get(key);
            String valueType = (String) layerTypeChain.get(a);

            objects[j] = value;
            temp += key + ":" + valueType + ", ";
            j++;
        }

        simpleFeatureType = DataUtilities.createType(featureID, "the_geom:" + geometryType + "," + temp);
        simpleFeature = SimpleFeatureBuilder.build(simpleFeatureType, objects, featureID);

        return simpleFeature;
    }

    private SimpleFeature buildFeature(JSONObject feature) throws SchemaException {

        String featureID = (String) feature.get("id");
        Geometry geometry = buildGeometry(feature);

        String geometryType = geometry.getGeometryType();

        // SimpleFeature
        SimpleFeatureType simpleFeatureType = null;
        SimpleFeature simpleFeature = null;

        simpleFeatureType = DataUtilities.createType(featureID, "the_geom:" + geometryType);
        simpleFeature = SimpleFeatureBuilder.build(simpleFeatureType, new Object[] { geometry }, featureID);

        return simpleFeature;

    }

    private Geometry buildGeometry(JSONObject feature) {

        GeometryFactory geometryFactory = new GeometryFactory();

        JSONObject jsonGeometry = (JSONObject) feature.get("geometry");
        String jsonGeometryType = (String) jsonGeometry.get("type");

        if (jsonGeometryType.equals("Point")) {
            // Geometry
            JSONArray coordinates = (JSONArray) jsonGeometry.get("coordinates");
            Double x = (Double) coordinates.get(0);
            Double y = (Double) coordinates.get(1);
            Point point = geometryFactory.createPoint(new Coordinate(x, y));
            return point;

        } else if (jsonGeometryType.equals("LineString")) {

            JSONArray outerCoordinates = (JSONArray) jsonGeometry.get("coordinates");
            Coordinate[] coordinateArray;
            coordinateArray = new Coordinate[outerCoordinates.size()];

            for (int k = 0; k < outerCoordinates.size(); k++) {
                JSONArray innerCoordinates = (JSONArray) outerCoordinates.get(k);
                Double x = (Double) innerCoordinates.get(0);
                Double y = (Double) innerCoordinates.get(1);
                coordinateArray[k] = new Coordinate(x, y);
            }
            LineString lineString = geometryFactory.createLineString(coordinateArray);
            return lineString;

        } else if (jsonGeometryType.equals("Polygon")) {

            JSONArray outerCoordinates = (JSONArray) jsonGeometry.get("coordinates");

            Coordinate[] coordinateArray;
            LinearRing linearRing = null;
            LinearRing holes[] = null;
            Polygon polygon = null;

            for (int k = 0; k < outerCoordinates.size(); k++) {

                JSONArray innerCoordinates = (JSONArray) outerCoordinates.get(k);
                coordinateArray = new Coordinate[innerCoordinates.size()];

                for (int r = 0; r < innerCoordinates.size(); r++) {
                    JSONArray innerCoor = (JSONArray) innerCoordinates.get(r);

                    Double x = (Double) innerCoor.get(0);
                    Double y = (Double) innerCoor.get(1);
                    coordinateArray[r] = new Coordinate(x, y);
                }
                linearRing = geometryFactory.createLinearRing(coordinateArray);
            }
            polygon = geometryFactory.createPolygon(linearRing, holes);
            return polygon;

        } else if (jsonGeometryType.equals("MultiPoint")) {


            JSONArray outerCoordinates = (JSONArray) jsonGeometry.get("coordinates");
            Coordinate[] coordinateArray;
            coordinateArray = new Coordinate[outerCoordinates.size()];

            for (int k = 0; k < outerCoordinates.size(); k++) {
                JSONArray coordinates = (JSONArray) jsonGeometry.get("coordinates");
                Double x = (Double) coordinates.get(0);
                Double y = (Double) coordinates.get(1);
                coordinateArray[k] = new Coordinate(x, y);
            }
            MultiPoint multiPoint = geometryFactory.createMultiPoint(coordinateArray);
            return multiPoint;

        } else if (jsonGeometryType.equals("MultiLineString")) {

            // Geometry
            JSONArray outerCoordinates = (JSONArray) jsonGeometry.get("coordinates");
            Coordinate[] coordinateArray;
            LineString lineStrings[] = new LineString[outerCoordinates.size()];

            for (int k = 0; k < outerCoordinates.size(); k++) {
                JSONArray innerCoordinates = (JSONArray) outerCoordinates.get(k);
                coordinateArray = new Coordinate[innerCoordinates.size()];
                for (int r = 0; r < innerCoordinates.size(); r++) {
                    JSONArray innerCoor = (JSONArray) innerCoordinates.get(r);
                    Double x = (Double) innerCoor.get(0);
                    Double y = (Double) innerCoor.get(1);
                    coordinateArray[r] = new Coordinate(x, y);
                }
                lineStrings[k] = geometryFactory.createLineString(coordinateArray);
            }
            MultiLineString multiLineString = geometryFactory.createMultiLineString(lineStrings);
            return multiLineString;

        } else if (jsonGeometryType.equals("MultiPolygon")) {

            JSONArray firstOuter = (JSONArray) jsonGeometry.get("coordinates");

            Coordinate[] coordinateArray;
            LinearRing linearRing = null;
            LinearRing holes[] = null;
            Polygon[] polygons = new Polygon[firstOuter.size()];

            for (int a = 0; a < firstOuter.size(); a++) {
                JSONArray firstInnerCoor = (JSONArray) firstOuter.get(a);
                for (int k = 0; k < firstInnerCoor.size(); k++) {
                    JSONArray secondInnerCoor = (JSONArray) firstInnerCoor.get(k);
                    coordinateArray = new Coordinate[secondInnerCoor.size()];
                    for (int r = 0; r < secondInnerCoor.size(); r++) {
                        JSONArray thirdInnerCoor = (JSONArray) secondInnerCoor.get(r);

                        Double x = (Double) thirdInnerCoor.get(0);
                        Double y = (Double) thirdInnerCoor.get(1);
                        coordinateArray[r] = new Coordinate(x, y);
                    }
                    linearRing = geometryFactory.createLinearRing(coordinateArray);
                }
                polygons[a] = geometryFactory.createPolygon(linearRing, holes);
            }
            MultiPolygon multiPolygon = geometryFactory.createMultiPolygon(polygons);
            return multiPolygon;
        }
        return null;
    }
}
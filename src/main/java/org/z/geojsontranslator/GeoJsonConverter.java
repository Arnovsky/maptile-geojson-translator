package org.z.geojsontranslator;

import mil.nga.sf.geojson.Feature;
import mil.nga.sf.geojson.FeatureCollection;
import mil.nga.sf.geojson.Polygon;
import mil.nga.sf.geojson.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class GeoJsonConverter {
    private static final Logger LOGGER = LoggerFactory.getLogger(GeoJsonConverter.class);

    private GeoJsonConverter() {

    }

    public static FeatureCollection convertTilesToGeoJson(Map<String, Map<String, Object>> tileToEntityCount) {
        List<Feature> features = tileToEntityCount.entrySet().stream()
                .map(entry -> createFeature(entry.getValue(), createCoordinatesForCode(entry.getKey())))
                .collect(Collectors.toList());

        LOGGER.info("returned {} features", features.size());
        return new FeatureCollection(features);
    }

    private static List<Position> createCoordinatesForCode(String codes) {
        int[] convertedCodes = Arrays.stream(codes.split("/")).mapToInt(Integer::parseInt).toArray();
        return BoundingBox.tile2boundingBox(
                convertedCodes[1], // X
                convertedCodes[2], // Y
                convertedCodes[0]  // Zoom level
        ).convertToCoordinates();
    }

    private static Feature createFeature(Map<String, Object> tileToProperties, List<Position> coordinates) {
        Feature feature = new Feature(new Polygon(Collections.singletonList(coordinates)));
        feature.setProperties(tileToProperties);
        return feature;
    }
}

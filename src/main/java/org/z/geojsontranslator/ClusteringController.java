package org.z.geojsontranslator;

import mil.nga.sf.geojson.Feature;
import mil.nga.sf.geojson.FeatureCollection;
import mil.nga.sf.geojson.Polygon;
import mil.nga.sf.geojson.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
public class ClusteringController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusteringController.class);

    private final ElasticQueries elasticQueries;

    public ClusteringController(ElasticQueries elasticQueries) {
        this.elasticQueries = elasticQueries;
    }

    @PostMapping(path = "/query", consumes = "application/json", produces = "application/json")
    public FeatureCollection query(@RequestBody Coordinates coordinates) {
        LOGGER.info("coordinates received {}", coordinates);
        Map<String, Map<String, Object>> tileToProperties = elasticQueries.aggregateEntities(coordinates);

        return convertTilesToGeoJson(tileToProperties);
    }

    private static FeatureCollection convertTilesToGeoJson(Map<String, Map<String, Object>> tileToEntityCount) {
        List<Feature> features = new ArrayList<>();
        for (Map.Entry<String, Map<String, Object>> tileToProperties : tileToEntityCount.entrySet()) {
            int[] codes = Arrays.stream(tileToProperties.getKey().split("/")).mapToInt(Integer::parseInt).toArray();
            List<Position> coordinates =
                    BoundingBox.tile2boundingBox(codes[1], codes[2], codes[0]).convertToCoordinates();

            Feature feature = new Feature(new Polygon(Collections.singletonList(coordinates)));
            feature.setProperties(tileToProperties.getValue());

            features.add(feature);
        }

        LOGGER.info("returned {} features", features.size());
        return new FeatureCollection(features);
    }
}

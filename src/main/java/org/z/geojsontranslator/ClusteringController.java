package org.z.geojsontranslator;

import mil.nga.sf.geojson.Feature;
import mil.nga.sf.geojson.FeatureCollection;
import mil.nga.sf.geojson.Polygon;
import mil.nga.sf.geojson.Position;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
public class ClusteringController {
    private final ElasticQueries elasticQueries;

    public ClusteringController(ElasticQueries elasticQueries) {
        this.elasticQueries = elasticQueries;
    }

    @GetMapping("/query")
    public FeatureCollection query() {
        Map<String, Long> tileToEntityCount = elasticQueries.aggregateEntities();

        return convertTileKeysToFeatureCollection(tileToEntityCount);
    }

    private static FeatureCollection convertTileKeysToFeatureCollection(Map<String, Long> tileToEntityCount) {
        List<Feature> features = new ArrayList<>();
        for (Map.Entry<String, Long> tileToCount : tileToEntityCount.entrySet()) {
            int[] codes = Arrays.stream(tileToCount.getKey().split("/")).mapToInt(Integer::parseInt).toArray();
            List<Position> coordinates =
                    BoundingBox.tile2boundingBox(codes[1], codes[2], codes[0]).convertToCoordinates();

            Feature feature = new Feature(new Polygon(Collections.singletonList(coordinates)));
            feature.setProperties(Collections.singletonMap("entities", tileToCount.getValue()));

            features.add(feature);
        }

        return new FeatureCollection(features);
    }
}

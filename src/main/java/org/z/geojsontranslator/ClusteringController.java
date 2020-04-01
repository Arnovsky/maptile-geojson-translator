package org.z.geojsontranslator;

import mil.nga.sf.geojson.FeatureCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ClusteringController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusteringController.class);

    private final AggregationQuery aggregationQuery;

    public ClusteringController(AggregationQuery aggregationQuery) {
        this.aggregationQuery = aggregationQuery;
    }

    @PostMapping(path = "/query", consumes = "application/json", produces = "application/json")
    public FeatureCollection query(@RequestBody Coordinates coordinates) {
        LOGGER.info("coordinates received {}", coordinates);
        return GeoJsonConverter.convertTilesToGeoJson(aggregationQuery.aggregateTiles(coordinates));
    }
}

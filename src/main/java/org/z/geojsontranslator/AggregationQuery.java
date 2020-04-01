package org.z.geojsontranslator;

import lombok.SneakyThrows;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.geo.GeoBoundingBox;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.geogrid.GeoGrid;
import org.elasticsearch.search.aggregations.bucket.geogrid.GeoGridAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AggregationQuery {
    private static final int QUERY_MAX_SIZE = 1500;

    @Value("${org.z.geojsontranslator.elastic.index}")
    private String index;

    @Value("${org.z.geojsontranslator.elastic.termsField}")
    private String termsField;

    @Value("${org.z.geojsontranslator.elastic.geoTileField}")
    private String geoTileField;

    private final RestHighLevelClient highLevelClient;

    public AggregationQuery(RestHighLevelClient highLevelClient) {
        this.highLevelClient = highLevelClient;
    }

    @SneakyThrows
    public Map<String, Map<String, Object>> aggregateTiles(Coordinates coordinates) {
        SearchRequest searchRequest = createSearchRequest(coordinates);

        SearchResponse response = highLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        return parseAggregation(response);
    }

    private static Map<String, Map<String, Object>> parseAggregation(SearchResponse response) {
        HashMap<String, Map<String, Object>> tileToProperties = new HashMap<>();

        GeoGrid grid = response.getAggregations().get("grid");
        for (GeoGrid.Bucket bucket : grid.getBuckets()) {
            // insert the sub-aggregation into the properties map.
            ParsedStringTerms parsedStringTerms = bucket.getAggregations().get("terms");
            Map<String, Object> properties = parsedStringTerms.getBuckets().stream()
                    .collect(Collectors.toMap(
                            MultiBucketsAggregation.Bucket::getKeyAsString,
                            MultiBucketsAggregation.Bucket::getDocCount
                    ));
            // insert the total entities that were queried in the tile.
            properties.put("Total Entities", bucket.getDocCount());
            tileToProperties.put(bucket.getKeyAsString(), properties);
        }
        return tileToProperties;
    }

    private SearchRequest createSearchRequest(Coordinates coordinates) {
        TermsAggregationBuilder terms = AggregationBuilders.terms("terms")
                .field(termsField)
                .size(QUERY_MAX_SIZE);

        GeoGridAggregationBuilder geoTile = AggregationBuilders.geotileGrid("grid")
                .field(geoTileField)
                .setGeoBoundingBox(new GeoBoundingBox(
                        new GeoPoint(coordinates.getTopLeft().getX(), coordinates.getTopLeft().getY()),
                        new GeoPoint(coordinates.getBottomRight().getX(), coordinates.getBottomRight().getY())
                ))
                .precision(coordinates.getZoomLevel())
                .subAggregation(terms)
                .size(QUERY_MAX_SIZE);

        return new SearchRequest()
                .indices(index)
                .source(SearchSourceBuilder.searchSource()
                        .query(QueryBuilders.matchAllQuery())
                        .aggregation(geoTile));
    }
}

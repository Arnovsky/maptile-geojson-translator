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
import org.elasticsearch.search.aggregations.bucket.geogrid.GeoGrid;
import org.elasticsearch.search.aggregations.bucket.geogrid.GeoGridAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * very original name
 */
@PropertySource("classpath:application.properties")
@Service
public class ElasticQueries {
    public static final int QUERY_MAX_SIZE = 1500;

    @Value("${spring.elasticsearch.rest.index}")
    private String index;

    private final RestHighLevelClient highLevelClient;

    public ElasticQueries(RestHighLevelClient highLevelClient) {
        this.highLevelClient = highLevelClient;
    }

    @SneakyThrows
    public Map<String, Map<String, Object>> aggregateEntities(Coordinates coordinates) {
        TermsAggregationBuilder terms = AggregationBuilders.terms("terms")
                .field("vessel_type.keyword")
                .size(QUERY_MAX_SIZE);

        GeoGridAggregationBuilder geoTile = AggregationBuilders.geotileGrid("grid")
                .field("coordinates")
                .setGeoBoundingBox(new GeoBoundingBox(
                        new GeoPoint(coordinates.getTopLeft().getX(), coordinates.getTopLeft().getY()),
                        new GeoPoint(coordinates.getBottomRight().getX(), coordinates.getBottomRight().getY())
                ))
                .precision(coordinates.getZoomLevel())
                .subAggregation(terms)
                .size(QUERY_MAX_SIZE);

        SearchRequest searchRequest = new SearchRequest()
                .indices(index)
                .source(SearchSourceBuilder.searchSource()
                        .query(QueryBuilders.matchAllQuery())
                        .aggregation(geoTile));

        SearchResponse response = highLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        GeoGrid grid = response.getAggregations().get("grid");


        HashMap<String, Map<String, Object>> tileToProperties = new HashMap<>();
        for (GeoGrid.Bucket bucket : grid.getBuckets()) {
            Map<String, Object> properties = new HashMap<>();
            properties.put("Total Entities", bucket.getDocCount());

            ParsedStringTerms parsedStringTerms = bucket.getAggregations().get("terms");
            for (Terms.Bucket termsBucket : parsedStringTerms.getBuckets()) {
                properties.put(termsBucket.getKeyAsString(), termsBucket.getDocCount());
            }

            tileToProperties.put(bucket.getKeyAsString(), properties);
        }
        return tileToProperties;
    }
}

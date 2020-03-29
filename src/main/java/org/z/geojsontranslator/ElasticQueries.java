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
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * very original name
 */
@Service
public class ElasticQueries {
    @Value("${spring.elasticsearch.rest.index}")
    private String index;

    private final RestHighLevelClient highLevelClient;

    public ElasticQueries(RestHighLevelClient highLevelClient) {
        this.highLevelClient = highLevelClient;
    }

    @SneakyThrows
    public Map<String, Long> aggregateEntities() {
        TermsAggregationBuilder terms = AggregationBuilders.terms("terms")
                .field("vessel_type.keyword")
                .size(1500);

        GeoGridAggregationBuilder geoTile = AggregationBuilders.geotileGrid("grid")
                .field("coordinates")
                .setGeoBoundingBox(new GeoBoundingBox(
                        new GeoPoint(50, -46),
                        new GeoPoint(-31, -164)
                ))
                .precision(4)
                .subAggregation(terms)
                .size(1500);

        SearchRequest searchRequest = new SearchRequest()
                .indices(index)
                .source(SearchSourceBuilder.searchSource()
                        .query(QueryBuilders.matchAllQuery())
                        .aggregation(geoTile));

        SearchResponse response = highLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        GeoGrid grid = response.getAggregations().get("grid");

        return grid.getBuckets().stream()
                .collect(Collectors.toMap(
                        MultiBucketsAggregation.Bucket::getKeyAsString, MultiBucketsAggregation.Bucket::getDocCount));
    }
}

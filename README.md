# CesiumJs server-side clustering via Elasticsearch

The goal of this project was to make a server-side clustering view in CesiumJs.
We use Elasticsearch for the backend (specifically `GeoTileGridAggregation`).

### Flow
When a user moves his map the webapp calculates the top-left and bottom-right coordinates of his screen,
which are then sent to the server that queries them in Elasticsearch and receives WMTS tiles as a response.

We take the tiles received from Elasticsearch and translates it to GeoJSON then we send it to the webapp which loads it as a `GeoJsonDataSource` in Cesium. 

### End Result
![](https://i.imgur.com/4bZHntV.png)

### Why not use [`GeoHash`](https://www.elastic.co/guide/en/elasticsearch/reference/current/search-aggregations-bucket-geohashgrid-aggregation.html) aggregation

#### Rationale
We used WMTS based map server and thought we might be able to add the clustering via a new layer in the map server, the idea fell through,
but we ended using the WMTS based [GeoTile](https://www.elastic.co/guide/en/elasticsearch/reference/current/search-aggregations-bucket-geotilegrid-aggregation.html) aggregation.

#### GeoHash
This demo could have been made using GeoHash, the only difference would have been the translation 'layer'.
Instead of translation from WMTS, we would need to translate a GeoHash.

Both methods achieve the same result, only via a different "translation" medium.

### Setup
- Load the `geojson.json` file into Elasticsearch (I did this via the Kibana UI).
- Change the Elasticsearch index in `application.properties` to your desired index.
- Run the application via `gradle run` - this will also build the webapp (make sure you have npm installed)
- Go to http:/localhost:8080/ (by defualt)

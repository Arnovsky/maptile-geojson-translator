package org.z.geojsontranslator;

import com.google.common.collect.Lists;
import mil.nga.sf.geojson.Position;

import java.util.List;

public class BoundingBox {

    private double north;
    private double south;
    private double east;
    private double west;

    public BoundingBox(double north, double south, double east, double west) {
        this.north = north;
        this.south = south;
        this.east = east;
        this.west = west;
    }

    public static BoundingBox tile2boundingBox(final int x, final int y, final int zoom) {
        return new BoundingBox(
                tile2lat(y, zoom),
                tile2lat(y + 1, zoom),
                tile2lon(x, zoom),
                tile2lon(x + 1, zoom)
        );
    }

    private static double tile2lon(int x, int z) {
        return x / Math.pow(2.0, z) * 360.0 - 180;
    }

    private static double tile2lat(int y, int z) {
        double n = Math.PI - (2.0 * Math.PI * y) / Math.pow(2.0, z);
        return Math.toDegrees(Math.atan(Math.sinh(n)));
    }

    public double getNorth() {
        return north;
    }

    public double getSouth() {
        return south;
    }

    public double getEast() {
        return east;
    }

    public double getWest() {
        return west;
    }


    public List<Position> convertToCoordinates() {
        return Lists.newArrayList(
                new Position(this.east, this.north),
                new Position(this.east, this.south),
                new Position(this.west, this.south),
                new Position(this.west, this.north)
        );
    }
}

package org.z.geojsontranslator;

import lombok.AllArgsConstructor;
import lombok.Data;
import mil.nga.sf.geojson.Position;

@Data
@AllArgsConstructor
public class Coordinates {
    private Position topLeft;
    private Position bottomRight;
    private int zoomLevel;
}

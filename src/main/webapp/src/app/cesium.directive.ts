import { Directive, ElementRef } from '@angular/core';
import { HttpClient } from "@angular/common/http";

interface Coordinates {
  topLeft: Array<number>;
  bottomRight: Array<number>;
  zoomLevel: number;
}

@Directive({
  selector: '[appCesium]'
})

export class CesiumDirective {
  private readonly viewer: any;

  constructor(private el: ElementRef, private http: HttpClient) {
    this.viewer = this.createCesiumViewer();
    this.viewer.camera.moveEnd.addEventListener(() => this.reloadGeoJsonOnMove());
  }

  private reloadGeoJsonOnMove(): void {
    this.http.post("/query", this.calculateCoordinates())
      .subscribe((res) => {
        if (res['features'] === undefined) {
          return;
        }

        this.viewer.dataSources.removeAll();
        this.viewer.entities.removeAll();
        Cesium.GeoJsonDataSource.load(res, {})
          .then((dataSource) => this.labelEntities(dataSource));
      })
  }

  private static normalize(values: any, target: number): any {
    const max = Math.max(...values);
    const min = Math.min(...values);
    // return (target - min) / (max - min);
    return Math.abs(1.0 - (target - min) / (max - min)) / 3.0;
  }

  private static calculateHeatmapColor(values: any, target: number) {
    return new Cesium.Color(1.0, CesiumDirective.normalize(values, target), CesiumDirective.normalize(values, target), 0.5);
  }

  private labelEntities(dataSource: any) {
    const entities = dataSource.entities.values;
    const values = entities.map(entity => entity.properties["Total Entities"].getValue());

    for (let i = 0; i < entities.length; i++) {
      let center = Cesium.BoundingSphere.fromPoints(entities[i].polygon.hierarchy.getValue().positions).center;
      Cesium.Ellipsoid.WGS84.scaleToGeodeticSurface(center, center);

      const totalEntities = entities[i].properties["Total Entities"].getValue();
      const color = CesiumDirective.calculateHeatmapColor(values, totalEntities);

      entities[i].polygon.material = new Cesium.ColorMaterialProperty(color);
      entities[i].polygon.outline = false;
      entities[i].merge({
        position: center,
        label: {
          text: totalEntities.toString(),
          font: '20px sans-serif',
          showBackground: true,
          horizontalOrigin: Cesium.HorizontalOrigin.BOTTOM,
        }
      });
    }
    this.viewer.dataSources.add(dataSource);
  }

  private calculateCoordinates(): Coordinates {
    const positionCartographic = this.viewer.camera.positionCartographic;
    const rect = this.getViewRectangle();
    const northWest = [Cesium.Math.toDegrees(rect.north), Cesium.Math.toDegrees(rect.west)]; //topLeft corner
    const southEast = [Cesium.Math.toDegrees(rect.south), Cesium.Math.toDegrees(rect.east)]; //bottomRight corner

    return {
      topLeft: northWest,
      bottomRight: southEast,
      zoomLevel: this.detectZoomLevel(positionCartographic.height) + 3
    };
  }

  private createCesiumViewer(): any {
    Cesium.Camera.DEFAULT_VIEW_FACTOR = 0;
    Cesium.Camera.DEFAULT_VIEW_RECTANGLE = Cesium.Rectangle.fromRadians(0.4, -0.5, 2.5, 0.8); // TODO: env variables?

    const viewer = new Cesium.Viewer(this.el.nativeElement, {
      geocoder: false,
      timeline: false,
      animation: false,
      sceneModePicker: false,
      selectionIndicator: false,
      baseLayerPicker: false,
      homeButton: false,
      navigationHelpButton: false,
      navigationInstructionsInitiallyVisible: false,
      sceneMode: Cesium.SceneMode.COLUMBUS_VIEW,
      creditContainer: undefined,
      creditViewport: undefined,
      mapMode2D: Cesium.MapMode2D.INFINITE_SCROLL
    });
    viewer.scene.screenSpaceCameraController.maximumZoomDistance = 20000000; // TODO: env variable?
    return viewer;
  }

  // taken from https://gist.github.com/ezze/d57e857a287677c9b43b5a6a43243b14
  private detectZoomLevel(distance: number): number | undefined {
    const scene = this.viewer.scene;
    const tileProvider = scene.globe._surface.tileProvider;
    const quadtree = tileProvider._quadtree;
    const drawingBufferHeight = this.viewer.canvas.height;
    const sseDenominator = this.viewer.camera.frustum.sseDenominator;

    for (let level = 0; level <= 19; level++) {
      const maxGeometricError = tileProvider.getLevelMaximumGeometricError(level);
      const error = (maxGeometricError * drawingBufferHeight) / (distance * sseDenominator);
      if (error < quadtree.maximumScreenSpaceError) {
        return level;
      }
    }
    return null;
  }

  private getViewRectangle(): any {
    const ellipsoid = this.viewer.scene.globe.ellipsoid;
    let rect = this.viewer.camera.computeViewRectangle(this.viewer.scene.globe.ellipsoid);

    if (rect == undefined) {
      const cl2 = new Cesium.Cartesian2(0, 0);
      let leftTop = this.viewer.scene.camera.pickEllipsoid(cl2, ellipsoid);

      const cr2 = new Cesium.Cartesian2(this.viewer.scene.canvas.width, this.viewer.scene.canvas.height);
      let rightDown = this.viewer.scene.camera.pickEllipsoid(cr2, ellipsoid);

      leftTop = ellipsoid.cartesianToCartographic(leftTop);
      rightDown = ellipsoid.cartesianToCartographic(rightDown);
      rect = new Cesium.Rectangle(leftTop.longitude, rightDown.latitude, rightDown.longitude, leftTop.latitude);
    }
    return rect;
  }
}

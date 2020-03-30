import { Directive, ElementRef, OnInit } from '@angular/core';
import { HttpClient } from "@angular/common/http";

@Directive({
  selector: '[appCesium]'
})
export class CesiumDirective implements OnInit {
  private readonly viewer: any;

  constructor(private el: ElementRef, private http: HttpClient) {
    this.viewer = new Cesium.Viewer(this.el.nativeElement, {
      geocoder: false,
      timeline: false,
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

    this.viewer.camera.moveEnd.addEventListener(() => {
      const carto = this.viewer.camera.positionCartographic;
      console.log(this.detectZoomLevel(carto.height));

      const rect = this.getViewRectangle();

      const northWest = [Cesium.Math.toDegrees(rect.north), Cesium.Math.toDegrees(rect.west)]; //topLeft corner
      const southEast = [Cesium.Math.toDegrees(rect.south), Cesium.Math.toDegrees(rect.east)]; //bottomRight corner

      console.log(northWest, southEast);
      http.post("http://localhost:8080/query", {
        topLeft: northWest,
        bottomRight: southEast,
        zoomLevel: this.detectZoomLevel(carto.height) + 3
      }).subscribe((res) => {
        this.viewer.dataSources.removeAll();
        if (!('features' in res)) {
          return;
        }

        Cesium.GeoJsonDataSource.load(res, {})
          .then((dataSource) => this.viewer.dataSources.add(dataSource));
      })
    });
  }

  ngOnInit() {

  }

  // taken from https://gist.github.com/ezze/d57e857a287677c9b43b5a6a43243b14
  private detectZoomLevel(distance: number) {
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

  private getViewRectangle() {
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

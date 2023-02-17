import L from 'leaflet';
import LeafletMap from '../leaflet-map';
import {DEFAULT_ZOOM_LEVEL, WidgetUnitedMapSettings} from '../map-models';
import {WidgetContext} from '@home/models/widget-component.models';

export class HEREMap extends LeafletMap {
    constructor(ctx: WidgetContext, $container, options: WidgetUnitedMapSettings) {
        super(ctx, $container, options);
        const map = L.map($container, {
          doubleClickZoom: !this.options.disableDoubleClickZooming,
          zoomControl: !this.options.disableZoomControl
        }).setView(options?.parsedDefaultCenterPosition, options?.defaultZoomLevel || DEFAULT_ZOOM_LEVEL);
        const tileLayer = (L.tileLayer as any).provider(options.mapProviderHere || 'HERE.normalDay', options.credentials);
        tileLayer.addTo(map);
        super.setMap(map);
    }
}

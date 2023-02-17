import LeafletMap from '@home/components/widget/lib/maps/leaflet-map';

export interface MapWidgetInterface {
    map?: LeafletMap;
    resize();
    update();
    destroy();
}

export interface MapWidgetStaticInterface {
    actionSources(): object;
}

import {OpenStreetMap} from './openstreet-map';
import {TencentMap} from './tencent-map';
import {GoogleMap} from './google-map';
import {HEREMap} from './here-map';
import {ImageMap} from './image-map';
import {Type} from '@angular/core';
import LeafletMap from '@home/components/widget/lib/maps/leaflet-map';

export const providerClass: { [key: string]: Type<LeafletMap> } = {
  'openstreet-map': OpenStreetMap,
  'tencent-map': TencentMap,
  'google-map': GoogleMap,
  here: HEREMap,
  'image-map': ImageMap
};

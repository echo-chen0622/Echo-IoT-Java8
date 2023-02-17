import {FormattedData} from '@shared/models/widget.models';

// redeclare module, maintains compatibility with @types/leaflet
declare module 'leaflet' {
  interface MarkerOptions {
    tbMarkerData?: FormattedData;
  }
}

import L from 'leaflet';
import {
  ShowTooltipAction, WidgetToolipSettings
} from './map-models';
import { Datasource } from '@app/shared/models/widget.models';

export function createTooltip(target: L.Layer,
                              settings: Partial<WidgetToolipSettings>,
                              datasource: Datasource,
                              autoClose = false,
                              showTooltipAction = ShowTooltipAction.click,
                              content?: string | HTMLElement
): L.Popup {
    const popup = L.popup();
    popup.setContent(content);
    target.bindPopup(popup, { autoClose, closeOnClick: false });
    if (showTooltipAction === ShowTooltipAction.hover) {
        target.off('click');
        target.on('mouseover', () => {
            target.openPopup();
        });
        target.on('mousemove', (e) => {
            // @ts-ignore
            popup.setLatLng(e.latlng);
        });
        target.on('mouseout', () => {
            target.closePopup();
        });
    }
    target.on('popupopen', () => {
      bindPopupActions(popup, settings, datasource);
      (target as any)._popup._closeButton.addEventListener('click', (event: Event) => {
        event.preventDefault();
      });
    });
    return popup;
}

export function bindPopupActions(popup: L.Popup, settings: Partial<WidgetToolipSettings>,
                                 datasource: Datasource) {
  const actions = popup.getElement().getElementsByClassName('tb-custom-action');
  Array.from(actions).forEach(
    (element: HTMLElement) => {
      const actionName = element.getAttribute('data-action-name');
      if (element && settings.tooltipAction[actionName]) {
        element.onclick = ($event) =>
        {
          settings.tooltipAction[actionName]($event, datasource);
          return false;
        };
      }
    });
}

export function isCutPolygon(data): boolean {
  if (data.length > 1 && Array.isArray(data[0]) && (Array.isArray(data[0][0]) || data[0][0] instanceof L.LatLng)) {
    return true;
  }
  return false;
}

export function isJSON(data: string): boolean {
  try {
    const parseData = JSON.parse(data);
    return !Array.isArray(parseData);
  } catch (e) {
    return false;
  }
}

import { Component, Inject, InjectionToken } from '@angular/core';
import { widgetType } from '@shared/models/widget.models';

export const DISPLAY_WIDGET_TYPES_PANEL_DATA = new InjectionToken<any>('DisplayWidgetTypesPanelData');

export interface WidgetTypes {
  type: widgetType;
  display: boolean;
}

export interface DisplayWidgetTypesPanelData {
  types: WidgetTypes[];
  typesUpdated: (columns: WidgetTypes[]) => void;
}

@Component({
  selector: 'tb-widget-types-panel',
  templateUrl: './widget-types-panel.component.html',
  styleUrls: ['./widget-types-panel.component.scss']
})
export class DisplayWidgetTypesPanelComponent {

  types: WidgetTypes[];

  constructor(@Inject(DISPLAY_WIDGET_TYPES_PANEL_DATA) public data: DisplayWidgetTypesPanelData) {
    this.types = this.data.types;
  }

  public update() {
    this.data.typesUpdated(this.types);
  }
}

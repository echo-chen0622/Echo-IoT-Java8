export interface ILayoutController {
  reload();
  resetHighlight();
  highlightWidget(widgetId: string, delay?: number);
  selectWidget(widgetId: string, delay?: number);
  pasteWidget($event: MouseEvent);
  pasteWidgetReference($event: MouseEvent);
}

export enum LayoutWidthType {
  PERCENTAGE = 'percentage',
  FIXED = 'fixed'
}

export enum LayoutPercentageSize {
  MIN = 10,
  MAX = 90
}

export enum LayoutFixedSize {
  MIN = 150,
  MAX = 4000
}

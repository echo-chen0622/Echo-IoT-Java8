import _ from 'lodash';
import tinycolor from 'tinycolor2';

import {
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  Input,
  OnDestroy,
  OnInit,
  SecurityContext,
  ViewChild
} from '@angular/core';
import {
  defaultTripAnimationSettings,
  MapProviders,
  WidgetUnitedTripAnimationSettings
} from '@home/components/widget/lib/maps/map-models';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { WidgetContext } from '@app/modules/home/models/widget-component.models';
import {
  findAngle,
  getRatio,
  interpolateOnLineSegment,
  parseWithTranslation
} from '@home/components/widget/lib/maps/common-maps-utils';
import { FormattedData, WidgetConfig } from '@shared/models/widget.models';
import moment from 'moment';
import {
  formattedDataArrayFromDatasourceData,
  formattedDataFormDatasourceData,
  isDefined,
  isUndefined,
  mergeFormattedData,
  parseFunction,
  safeExecute
} from '@core/utils';
import { ResizeObserver } from '@juggle/resize-observer';
import { MapWidgetInterface } from '@home/components/widget/lib/maps/map-widget.interface';

interface DataMap {
  [key: string]: FormattedData;
}

@Component({
  // tslint:disable-next-line:component-selector
  selector: 'trip-animation',
  templateUrl: './trip-animation.component.html',
  styleUrls: ['./trip-animation.component.scss']
})
export class TripAnimationComponent implements OnInit, AfterViewInit, OnDestroy {

  private mapResize$: ResizeObserver;

  constructor(private cd: ChangeDetectorRef, private sanitizer: DomSanitizer) { }

  @Input() ctx: WidgetContext;

  @ViewChild('map') mapContainer;

  mapWidget: MapWidgetInterface;
  historicalData: FormattedData[][];
  normalizationStep: number;
  interpolatedTimeData: {[time: number]: FormattedData}[] = [];
  formattedInterpolatedTimeData: FormattedData[][] = [];
  formattedCurrentPosition: FormattedData[] = [];
  formattedLatestData: FormattedData[] = [];
  widgetConfig: WidgetConfig;
  settings: WidgetUnitedTripAnimationSettings;
  mainTooltips = [];
  visibleTooltip = false;
  activeTrip: FormattedData;
  label: SafeHtml;
  minTime: number;
  maxTime: number;
  anchors: number[] = [];
  useAnchors: boolean;
  currentTime: number;

  ngOnInit(): void {
    this.widgetConfig = this.ctx.widgetConfig;
    this.settings = {
      buttonColor: tinycolor(this.widgetConfig.color).setAlpha(0.54).toRgbString(),
      ...defaultTripAnimationSettings,
      ...this.ctx.settings
    };
    this.useAnchors = this.settings.showPoints && this.settings.usePointAsAnchor;
    this.settings.parsedPointAsAnchorFunction = parseFunction(this.settings.pointAsAnchorFunction, ['data', 'dsData', 'dsIndex']);
    this.settings.parsedTooltipFunction = parseFunction(this.settings.tooltipFunction, ['data', 'dsData', 'dsIndex']);
    this.settings.parsedLabelFunction = parseFunction(this.settings.labelFunction, ['data', 'dsData', 'dsIndex']);
    this.settings.parsedColorPointFunction = parseFunction(this.settings.colorPointFunction, ['data', 'dsData', 'dsIndex']);
    this.normalizationStep = this.settings.normalizationStep;
    const subscription = this.ctx.defaultSubscription;
    subscription.callbacks.onDataUpdated = () => {
      this.historicalData = formattedDataArrayFromDatasourceData(this.ctx.data).map(
        item => this.clearIncorrectFirsLastDatapoint(item)).filter(arr => arr.length);
      this.interpolatedTimeData.length = 0;
      this.formattedInterpolatedTimeData.length = 0;
      const prevMinTime = this.minTime;
      const prevMaxTime = this.maxTime;
      this.calculateIntervals();
      const currentTime = this.calculateCurrentTime(prevMinTime, prevMaxTime);
      if (currentTime !== this.currentTime) {
        this.timeUpdated(currentTime);
      }
      this.mapWidget.map.map?.invalidateSize();
      this.mapWidget.map.setLoading(false);
      this.cd.detectChanges();
    };
    subscription.callbacks.onLatestDataUpdated = () => {
      this.formattedLatestData = formattedDataFormDatasourceData(this.ctx.latestData);
      this.updateCurrentData();
    };
  }

  ngAfterViewInit() {
    import('@home/components/widget/lib/maps/map-widget2').then(
      (mod) => {
        this.mapWidget = new mod.MapWidgetController(MapProviders.openstreet, false, this.ctx, this.mapContainer.nativeElement);
        this.mapResize$ = new ResizeObserver(() => {
          this.mapWidget.resize();
        });
        this.mapResize$.observe(this.mapContainer.nativeElement);
      }
    );
  }

  ngOnDestroy() {
    if (this.mapResize$) {
      this.mapResize$.disconnect();
    }
  }

  timeUpdated(time: number) {
    this.currentTime = time;
    // get point for each datasource associated with time
    this.formattedCurrentPosition = this.interpolatedTimeData
      .map(dataSource => dataSource[time]);
    for (let j = 0; j < this.interpolatedTimeData.length; j++) {
      if (isUndefined(this.formattedCurrentPosition[j])) {
        const timePoints = Object.keys(this.interpolatedTimeData[j]).map(item => parseInt(item, 10));
        for (let i = 1; i < timePoints.length; i++) {
          if (timePoints[i - 1] < time && timePoints[i] > time) {
            const beforePosition = this.interpolatedTimeData[j][timePoints[i - 1]];
            const afterPosition = this.interpolatedTimeData[j][timePoints[i]];
            const ratio = getRatio(timePoints[i - 1], timePoints[i], time);
            this.formattedCurrentPosition[j] = {
              ...beforePosition,
              time,
              ...interpolateOnLineSegment(beforePosition, afterPosition, this.settings.latKeyName, this.settings.lngKeyName, ratio)
            };
            break;
          }
        }
      }
    }
    for (let j = 0; j < this.interpolatedTimeData.length; j++) {
      if (isUndefined(this.formattedCurrentPosition[j])) {
        this.formattedCurrentPosition[j] = this.calculateLastPoints(this.interpolatedTimeData[j], time);
      }
    }
    this.updateCurrentData();
  }

  private updateCurrentData() {
    let currentPosition = this.formattedCurrentPosition;
    if (this.formattedLatestData.length) {
      currentPosition = mergeFormattedData(this.formattedCurrentPosition, this.formattedLatestData);
    }
    this.calcLabel(currentPosition);
    this.calcMainTooltip(currentPosition);
    if (this.mapWidget && this.mapWidget.map && this.mapWidget.map.map) {
      this.mapWidget.map.updateFromData(true, currentPosition, this.formattedInterpolatedTimeData, (trip) => {
        this.activeTrip = trip;
        this.timeUpdated(this.currentTime);
        this.cd.markForCheck();
      });
      if (this.settings.showPoints) {
        this.mapWidget.map.updatePoints(this.formattedInterpolatedTimeData, this.calcTooltip);
      }
    }
  }

  setActiveTrip() {
  }

  private calculateLastPoints(dataSource: DataMap, time: number): FormattedData {
    const timeArr = Object.keys(dataSource);
    let index = timeArr.findIndex((dtime) => {
      return Number(dtime) >= time;
    });

    if (index !== -1) {
      if (Number(timeArr[index]) !== time && index !== 0) {
        index--;
      }
    } else {
      index = timeArr.length - 1;
    }

    return dataSource[timeArr[index]];
  }

  calculateIntervals() {
    let minTime = Infinity;
    let maxTime = -Infinity;
    this.historicalData.forEach((dataSource) => {
      minTime = Math.min(dataSource[0].time, minTime);
      maxTime = Math.max(dataSource[dataSource.length - 1].time, maxTime);
    });
    this.minTime = minTime;
    this.maxTime = maxTime;
    this.historicalData.forEach((dataSource, index) => {
      this.interpolatedTimeData[index] = this.interpolateArray(dataSource);
    });
    this.formattedInterpolatedTimeData = this.interpolatedTimeData.map(ds => _.values(ds));
    if (!this.activeTrip) {
      this.activeTrip = this.interpolatedTimeData.map(dataSource => dataSource[this.minTime]).filter(ds => ds)[0];
    }
    if (this.useAnchors) {
      const anchorDate = Object.entries(_.union(this.interpolatedTimeData)[0]);
      this.anchors = anchorDate
        .filter((data: [string, FormattedData], tsIndex) => safeExecute(this.settings.parsedPointAsAnchorFunction, [data[1],
          this.formattedInterpolatedTimeData.map(ds => ds[tsIndex]), data[1].dsIndex]))
        .map(data => parseInt(data[0], 10));
    }
  }

  calcTooltip = (point: FormattedData, points: FormattedData[]): string => {
    const data = point ? point : this.activeTrip;
    const tooltipPattern: string = this.settings.useTooltipFunction ?
      safeExecute(this.settings.parsedTooltipFunction,
        [data, points, point.dsIndex]) : this.settings.tooltipPattern;
    return parseWithTranslation.parseTemplate(tooltipPattern, data, true);
  }

  private calcMainTooltip(points: FormattedData[]): void {
    const tooltips = [];
    for (const point of points) {
      tooltips.push(this.sanitizer.sanitize(SecurityContext.HTML, this.calcTooltip(point, points)));
    }
    this.mainTooltips = tooltips;
  }

  calcLabel(points: FormattedData[]) {
    if (this.activeTrip) {
      const data = points[this.activeTrip.dsIndex];
      const labelText: string = this.settings.useLabelFunction ?
        safeExecute(this.settings.parsedLabelFunction, [data, points, data.dsIndex]) : this.settings.label;
      this.label = this.sanitizer.bypassSecurityTrustHtml(parseWithTranslation.parseTemplate(labelText, data, true));
    }
  }

  private interpolateArray(originData: FormattedData[]): {[time: number]: FormattedData} {
    const result: {[time: number]: FormattedData} = {};
    const latKeyName = this.settings.latKeyName;
    const lngKeyName = this.settings.lngKeyName;
    for (const data of originData) {
      const currentTime = data.time;
      const normalizeTime = this.minTime + Math.ceil((currentTime - this.minTime) / this.normalizationStep) * this.normalizationStep;
      result[normalizeTime] = {
        ...data,
        minTime: this.minTime !== Infinity ? moment(this.minTime).format('YYYY-MM-DD HH:mm:ss') : '',
        maxTime: this.maxTime !== -Infinity ? moment(this.maxTime).format('YYYY-MM-DD HH:mm:ss') : '',
        rotationAngle: this.settings.rotationAngle
      };
    }
    const timeStamp = Object.keys(result);
    for (let i = 0; i < timeStamp.length - 1; i++) {
      if (isUndefined(result[timeStamp[i + 1]][latKeyName]) || isUndefined(result[timeStamp[i + 1]][lngKeyName])) {
        for (let j = i + 2; j < timeStamp.length - 1; j++) {
          if (isDefined(result[timeStamp[j]][latKeyName]) || isDefined(result[timeStamp[j]][lngKeyName])) {
            const ratio = getRatio(Number(timeStamp[i]), Number(timeStamp[j]), Number(timeStamp[i + 1]));
            result[timeStamp[i + 1]] = {
              ...interpolateOnLineSegment(result[timeStamp[i]], result[timeStamp[j]], latKeyName, lngKeyName, ratio),
              ...result[timeStamp[i + 1]],
            };
            break;
          }
        }
      }
      result[timeStamp[i]].rotationAngle += findAngle(result[timeStamp[i]], result[timeStamp[i + 1]], latKeyName, lngKeyName);
    }
    return result;
  }

  private calculateCurrentTime(minTime: number, maxTime: number): number {
    if (minTime !== this.minTime || maxTime !== this.maxTime) {
      if (this.minTime >= this.currentTime || isUndefined(this.currentTime)) {
        return this.minTime;
      } else if (this.maxTime <= this.currentTime) {
        return this.maxTime;
      } else {
        return this.minTime + Math.ceil((this.currentTime - this.minTime) / this.normalizationStep) * this.normalizationStep;
      }
    }
    return this.currentTime;
  }

  private clearIncorrectFirsLastDatapoint(dataSource: FormattedData[]): FormattedData[] {
    const firstHistoricalDataIndexCoordinate = dataSource.findIndex(this.findFirstHistoricalDataIndexCoordinate);
    if (firstHistoricalDataIndexCoordinate === -1) {
      return [];
    }
    let lastIndex = dataSource.length - 1;
    for (lastIndex; lastIndex > 0; lastIndex--) {
      if (this.findFirstHistoricalDataIndexCoordinate(dataSource[lastIndex])) {
        lastIndex++;
        break;
      }
    }
    if (firstHistoricalDataIndexCoordinate > 0 || lastIndex < dataSource.length) {
      return dataSource.slice(firstHistoricalDataIndexCoordinate, lastIndex);
    }
    return dataSource;
  }

  private findFirstHistoricalDataIndexCoordinate = (item: FormattedData): boolean => {
    return isDefined(item[this.settings.latKeyName]) && isDefined(item[this.settings.lngKeyName]);
  }
}

export let TbTripAnimationWidget = TripAnimationComponent;

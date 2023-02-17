import { Pipe, PipeTransform } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

@Pipe({
  name: 'milliSecondsToTimeString'
})
export class MillisecondsToTimeStringPipe implements PipeTransform {

  constructor(private translate: TranslateService) {
  }

  transform(millseconds: number, shortFormat = false): string {
    let seconds = Math.floor(millseconds / 1000);
    const days = Math.floor(seconds / 86400);
    let hours = Math.floor((seconds % 86400) / 3600);
    let minutes = Math.floor(((seconds % 86400) % 3600) / 60);
    seconds = seconds % 60;
    let timeString = '';
    if (shortFormat) {
      if (days > 0) {
        timeString += this.translate.instant('timewindow.short.days', {days});
      }
      if (hours > 0) {
        timeString += this.translate.instant('timewindow.short.hours', {hours});
      }
      if (minutes > 0) {
        timeString += this.translate.instant('timewindow.short.minutes', {minutes});
      }
      if (seconds > 0) {
        timeString += this.translate.instant('timewindow.short.seconds', {seconds});
      }
      if (!timeString.length) {
        timeString += this.translate.instant('timewindow.short.seconds', {seconds: 0});
      }
    } else {
      if (days > 0) {
        timeString += this.translate.instant('timewindow.days', {days});
      }
      if (hours > 0) {
        if (timeString.length === 0 && hours === 1) {
          hours = 0;
        }
        timeString += this.translate.instant('timewindow.hours', {hours});
      }
      if (minutes > 0) {
        if (timeString.length === 0 && minutes === 1) {
          minutes = 0;
        }
        timeString += this.translate.instant('timewindow.minutes', {minutes});
      }
      if (seconds > 0) {
        if (timeString.length === 0 && seconds === 1) {
          seconds = 0;
        }
        timeString += this.translate.instant('timewindow.seconds', {seconds});
      }
    }
    return timeString;
  }
}

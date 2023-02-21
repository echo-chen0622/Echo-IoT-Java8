import {ChangeDetectorRef, Component, forwardRef, Input, OnInit} from '@angular/core';
import {ControlValueAccessor, NG_VALUE_ACCESSOR} from '@angular/forms';
import {
  AlarmSchedule,
  AlarmScheduleType,
  dayOfWeekTranslations,
  getAlarmScheduleRangeText,
  utcTimestampToTimeOfDay
} from '@shared/models/device.models';
import {MatDialog} from '@angular/material/dialog';
import {
  AlarmScheduleDialogComponent,
  AlarmScheduleDialogData
} from '@home/components/profile/alarm/alarm-schedule-dialog.component';
import {deepClone, isDefinedAndNotNull} from '@core/utils';
import {TranslateService} from '@ngx-translate/core';

@Component({
  selector: 'tb-alarm-schedule-info',
  templateUrl: './alarm-schedule-info.component.html',
  styleUrls: ['./alarm-schedule-info.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => AlarmScheduleInfoComponent),
    multi: true
  }]
})
export class AlarmScheduleInfoComponent implements ControlValueAccessor, OnInit {

  @Input()
  disabled: boolean;

  private modelValue: AlarmSchedule;

  scheduleText = '';

  private propagateChange = (v: any) => { };

  constructor(private dialog: MatDialog,
              private translate: TranslateService,
              private cd: ChangeDetectorRef) {
  }

  ngOnInit(): void {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(value: AlarmSchedule): void {
    this.modelValue = value;
    this.updateScheduleText();
  }

  private updateScheduleText() {
    let schedule = this.modelValue;
    if (!isDefinedAndNotNull(schedule)) {
      schedule = {
        type: AlarmScheduleType.ANY_TIME
      };
    }
    this.scheduleText = '';
    switch (schedule.type) {
      case AlarmScheduleType.ANY_TIME:
        this.scheduleText = this.translate.instant('device-profile.schedule-any-time');
        break;
      case AlarmScheduleType.SPECIFIC_TIME:
        for (const day of schedule.daysOfWeek) {
          if (this.scheduleText.length) {
            this.scheduleText += ', ';
          }
          this.scheduleText += this.translate.instant(dayOfWeekTranslations[day - 1]);
        }
        this.scheduleText += ' <b>' + getAlarmScheduleRangeText(utcTimestampToTimeOfDay(schedule.startsOn),
          utcTimestampToTimeOfDay(schedule.endsOn)) + '</b>';
        break;
      case AlarmScheduleType.CUSTOM:
        for (const item of schedule.items) {
          if (item.enabled) {
            if (this.scheduleText.length) {
              this.scheduleText += ', ';
            }
            this.scheduleText += this.translate.instant(dayOfWeekTranslations[item.dayOfWeek  - 1]);
            this.scheduleText += ' <b>' + getAlarmScheduleRangeText(utcTimestampToTimeOfDay(item.startsOn),
              utcTimestampToTimeOfDay(item.endsOn)) + '</b>';
          }
        }
        break;
    }
  }

  public openScheduleDialog($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<AlarmScheduleDialogComponent, AlarmScheduleDialogData,
      AlarmSchedule>(AlarmScheduleDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        readonly: this.disabled,
        alarmSchedule: this.disabled ? this.modelValue : deepClone(this.modelValue)
      }
    }).afterClosed().subscribe((result) => {
      if (result) {
        this.modelValue = result;
        this.propagateChange(this.modelValue);
        this.updateScheduleText();
        this.cd.detectChanges();
      }
    });
  }

}

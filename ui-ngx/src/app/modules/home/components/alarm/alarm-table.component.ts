import {Component, Input, OnInit, ViewChild} from '@angular/core';
import {TranslateService} from '@ngx-translate/core';
import {DatePipe} from '@angular/common';
import {MatDialog} from '@angular/material/dialog';
import {EntityId} from '@shared/models/id/entity-id';
import {EntitiesTableComponent} from '@home/components/entity/entities-table.component';
import {DialogService} from '@core/services/dialog.service';
import {AlarmTableConfig} from './alarm-table-config';
import {AlarmSearchStatus} from '@shared/models/alarm.models';
import {AlarmService} from '@app/core/http/alarm.service';
import {Store} from '@ngrx/store';
import {AppState} from '@core/core.state';

@Component({
  selector: 'tb-alarm-table',
  templateUrl: './alarm-table.component.html',
  styleUrls: ['./alarm-table.component.scss']
})
export class AlarmTableComponent implements OnInit {

  activeValue = false;
  dirtyValue = false;
  entityIdValue: EntityId;

  @Input()
  set active(active: boolean) {
    if (this.activeValue !== active) {
      this.activeValue = active;
      if (this.activeValue && this.dirtyValue) {
        this.dirtyValue = false;
        this.entitiesTable.updateData();
      }
    }
  }

  @Input()
  set entityId(entityId: EntityId) {
    this.entityIdValue = entityId;
    if (this.alarmTableConfig && this.alarmTableConfig.entityId !== entityId) {
      this.alarmTableConfig.searchStatus = AlarmSearchStatus.ANY;
      this.alarmTableConfig.entityId = entityId;
      this.entitiesTable.resetSortAndFilter(this.activeValue);
      if (!this.activeValue) {
        this.dirtyValue = true;
      }
    }
  }

  @ViewChild(EntitiesTableComponent, {static: true}) entitiesTable: EntitiesTableComponent;

  alarmTableConfig: AlarmTableConfig;

  constructor(private alarmService: AlarmService,
              private dialogService: DialogService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private dialog: MatDialog,
              private store: Store<AppState>) {
  }

  ngOnInit() {
    this.dirtyValue = !this.activeValue;
    this.alarmTableConfig = new AlarmTableConfig(
      this.alarmService,
      this.dialogService,
      this.translate,
      this.datePipe,
      this.dialog,
      this.entityIdValue,
      AlarmSearchStatus.ANY,
      this.store
    );
  }

}

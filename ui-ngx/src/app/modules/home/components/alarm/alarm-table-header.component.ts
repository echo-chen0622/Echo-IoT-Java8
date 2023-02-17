import {Component} from '@angular/core';
import {Store} from '@ngrx/store';
import {AppState} from '@core/core.state';
import {EntityTableHeaderComponent} from '../../components/entity/entity-table-header.component';
import {AlarmInfo, AlarmSearchStatus, alarmSearchStatusTranslations} from '@shared/models/alarm.models';
import {AlarmTableConfig} from './alarm-table-config';

@Component({
  selector: 'tb-alarm-table-header',
  templateUrl: './alarm-table-header.component.html',
  styleUrls: ['./alarm-table-header.component.scss']
})
export class AlarmTableHeaderComponent extends EntityTableHeaderComponent<AlarmInfo> {

  alarmSearchStatusTranslationsMap = alarmSearchStatusTranslations;

  alarmSearchStatusTypes = Object.keys(AlarmSearchStatus);
  alarmSearchStatusEnum = AlarmSearchStatus;

  get alarmTableConfig(): AlarmTableConfig {
    return this.entitiesTableConfig as AlarmTableConfig;
  }

  constructor(protected store: Store<AppState>) {
    super(store);
  }

  searchStatusChanged(searchStatus: AlarmSearchStatus) {
    this.alarmTableConfig.searchStatus = searchStatus;
    this.alarmTableConfig.getTable().resetSortAndFilter(true, true);
  }
}

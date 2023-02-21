import {Component, Inject, InjectionToken} from '@angular/core';
import {FormBuilder, FormGroup} from '@angular/forms';
import {OverlayRef} from '@angular/cdk/overlay';
import {RpcStatus, rpcStatusTranslation} from '@shared/models/rpc.models';
import {TranslateService} from '@ngx-translate/core';

export const PERSISTENT_FILTER_PANEL_DATA = new InjectionToken<any>('AlarmFilterPanelData');

export interface PersistentFilterPanelData {
  rpcStatus: RpcStatus;
}

@Component({
  selector: 'tb-persistent-filter-panel',
  templateUrl: './persistent-filter-panel.component.html',
  styleUrls: ['./persistent-filter-panel.component.scss']
})
export class PersistentFilterPanelComponent {

  public persistentFilterFormGroup: FormGroup;
  public result: PersistentFilterPanelData;
  public rpcSearchStatusTranslationMap = rpcStatusTranslation;
  public rpcSearchPlaceholder: string;

  public persistentSearchStatuses = Object.keys(RpcStatus);

  constructor(@Inject(PERSISTENT_FILTER_PANEL_DATA)
              public data: PersistentFilterPanelData,
              public overlayRef: OverlayRef,
              private fb: FormBuilder,
              private translate: TranslateService) {
    this.persistentFilterFormGroup = this.fb.group(
      {
        rpcStatus: this.data.rpcStatus
      }
    );
    this.rpcSearchPlaceholder = this.translate.instant('widgets.persistent-table.any-status');
  }

  update() {
    this.result = {
      rpcStatus: this.persistentFilterFormGroup.get('rpcStatus').value
    };
    this.overlayRef.dispose();
  }

  cancel() {
    this.overlayRef.dispose();
  }
}

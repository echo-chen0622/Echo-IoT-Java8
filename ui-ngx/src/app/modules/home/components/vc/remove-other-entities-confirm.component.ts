import { Component, Input, OnInit } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

@Component({
  selector: 'tb-remove-other-entities-confirm',
  templateUrl: './remove-other-entities-confirm.component.html',
  styleUrls: []
})
export class RemoveOtherEntitiesConfirmComponent extends PageComponent implements OnInit {

  @Input()
  onClose: (result: boolean | null) => void;

  confirmFormGroup: FormGroup;

  removeOtherEntitiesConfirmText: SafeHtml;

  removeOtherEntitiesVerificationText = 'remove other entities';

  constructor(protected store: Store<AppState>,
              private translate: TranslateService,
              private sanitizer: DomSanitizer,
              private fb: FormBuilder) {
    super(store);
    this.removeOtherEntitiesConfirmText = this.sanitizer.bypassSecurityTrustHtml(this.translate.instant('version-control.remove-other-entities-confirm-text'));
  }

  ngOnInit(): void {
    this.confirmFormGroup = this.fb.group({
      verification: [null, []]
    });
  }

  cancel(): void {
    if (this.onClose) {
      this.onClose(null);
    }
  }

  confirm(): void {
    if (this.onClose) {
      this.onClose(true);
    }
  }
}

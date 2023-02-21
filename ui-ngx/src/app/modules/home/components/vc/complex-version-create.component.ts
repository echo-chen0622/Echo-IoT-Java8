import {ChangeDetectorRef, Component, Input, OnDestroy, OnInit} from '@angular/core';
import {PageComponent} from '@shared/components/page.component';
import {FormBuilder, FormGroup, Validators} from '@angular/forms';
import {
  ComplexVersionCreateRequest,
  createDefaultEntityTypesVersionCreate,
  SyncStrategy,
  syncStrategyHintMap,
  syncStrategyTranslationMap,
  VersionCreateRequestType,
  VersionCreationResult
} from '@shared/models/vc.models';
import {Store} from '@ngrx/store';
import {AppState} from '@core/core.state';
import {EntitiesVersionControlService} from '@core/http/entities-version-control.service';
import {TranslateService} from '@ngx-translate/core';
import {TbPopoverComponent} from '@shared/components/popover.component';
import {DomSanitizer, SafeHtml} from '@angular/platform-browser';
import {Observable, Subscription} from 'rxjs';
import {share} from 'rxjs/operators';
import {parseHttpErrorMessage} from '@core/utils';

@Component({
  selector: 'tb-complex-version-create',
  templateUrl: './complex-version-create.component.html',
  styleUrls: ['./version-control.scss']
})
export class ComplexVersionCreateComponent extends PageComponent implements OnInit, OnDestroy {

  @Input()
  branch: string;

  @Input()
  onClose: (result: VersionCreationResult | null, branch: string | null) => void;

  @Input()
  popoverComponent: TbPopoverComponent;

  createVersionFormGroup: FormGroup;

  syncStrategies = Object.values(SyncStrategy);

  syncStrategyTranslations = syncStrategyTranslationMap;

  syncStrategyHints = syncStrategyHintMap;

  resultMessage: SafeHtml;

  hasError = false;

  versionCreateResult: VersionCreationResult = null;

  versionCreateBranch: string = null;

  versionCreateResult$: Observable<VersionCreationResult>;

  private versionCreateResultSubscription: Subscription;

  constructor(protected store: Store<AppState>,
              private entitiesVersionControlService: EntitiesVersionControlService,
              private cd: ChangeDetectorRef,
              private sanitizer: DomSanitizer,
              private translate: TranslateService,
              private fb: FormBuilder) {
    super(store);
  }

  ngOnInit(): void {
    this.createVersionFormGroup = this.fb.group({
      branch: [this.branch, [Validators.required]],
      versionName: [null, [Validators.required, Validators.pattern(/(?:.|\s)*\S(&:.|\s)*/)]],
      syncStrategy: [SyncStrategy.MERGE, Validators.required],
      entityTypes: [createDefaultEntityTypesVersionCreate(), []],
    });
  }

  ngOnDestroy() {
    super.ngOnDestroy();
    if (this.versionCreateResultSubscription) {
      this.versionCreateResultSubscription.unsubscribe();
    }
  }

  cancel(): void {
    if (this.onClose) {
      this.onClose(this.versionCreateResult, this.versionCreateBranch);
    }
  }

  export(): void {
    const request: ComplexVersionCreateRequest = {
      branch: this.createVersionFormGroup.get('branch').value,
      versionName: this.createVersionFormGroup.get('versionName').value,
      syncStrategy: this.createVersionFormGroup.get('syncStrategy').value,
      entityTypes: this.createVersionFormGroup.get('entityTypes').value,
      type: VersionCreateRequestType.COMPLEX
    };

    this.versionCreateResult$ = this.entitiesVersionControlService.saveEntitiesVersion(request, {ignoreErrors: true}).pipe(
      share()
    );
    this.cd.detectChanges();
    if (this.popoverComponent) {
      this.popoverComponent.updatePosition();
    }

    this.versionCreateResultSubscription = this.versionCreateResult$.subscribe((result) => {
      let message: string;
      if (!result.error) {
        if (result.done && !result.added && !result.modified && !result.removed) {
          message = this.translate.instant('version-control.nothing-to-commit');
        } else {
          message = this.translate.instant('version-control.version-create-result',
            {added: result.added, modified: result.modified, removed: result.removed});
        }
      } else {
          message = result.error;
      }
      this.resultMessage = this.sanitizer.bypassSecurityTrustHtml(message);
      this.versionCreateResult = result;
      this.versionCreateBranch = request.branch;
      this.cd.detectChanges();
      if (this.popoverComponent) {
        this.popoverComponent.updatePosition();
      }
    },
    (error) => {
      this.hasError = true;
      this.resultMessage = this.sanitizer.bypassSecurityTrustHtml(parseHttpErrorMessage(error, this.translate).message);
      this.cd.detectChanges();
      if (this.popoverComponent) {
        this.popoverComponent.updatePosition();
      }
    });
  }
}

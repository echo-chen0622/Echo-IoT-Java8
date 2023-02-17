import {
  AfterViewInit,
  Component, ComponentFactoryResolver,
  ComponentRef,
  forwardRef,
  Input, OnChanges,
  OnDestroy,
  OnInit, SimpleChanges,
  ViewChild,
  ViewContainerRef
} from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import {
  IRuleNodeConfigurationComponent,
  RuleNodeConfiguration,
  RuleNodeDefinition
} from '@shared/models/rule-node.models';
import { Subscription } from 'rxjs';
import { RuleChainService } from '@core/http/rule-chain.service';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { TranslateService } from '@ngx-translate/core';
import { JsonObjectEditComponent } from '@shared/components/json-object-edit.component';
import { deepClone } from '@core/utils';
import { RuleChainType } from '@shared/models/rule-chain.models';
import { JsonFormComponent } from '@shared/components/json-form/json-form.component';
import { JsonFormComponentData } from '@shared/components/json-form/json-form-component.models';
import { IWidgetSettingsComponent, Widget, WidgetSettings } from '@shared/models/widget.models';
import { widgetSettingsComponentsMap } from '@home/components/widget/lib/settings/widget-settings.module';
import { Dashboard } from '@shared/models/dashboard.models';
import { WidgetService } from '@core/http/widget.service';
import { IAliasController } from '@core/api/widget-api.models';

@Component({
  selector: 'tb-widget-settings',
  templateUrl: './widget-settings.component.html',
  styleUrls: ['./widget-settings.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => WidgetSettingsComponent),
    multi: true
  }]
})
export class WidgetSettingsComponent implements ControlValueAccessor, OnInit, OnDestroy, AfterViewInit, OnChanges {

  @ViewChild('definedSettingsContent', {read: ViewContainerRef, static: true}) definedSettingsContainer: ViewContainerRef;

  @ViewChild('jsonFormComponent') jsonFormComponent: JsonFormComponent;

  @Input()
  disabled: boolean;

  @Input()
  aliasController: IAliasController;

  @Input()
  dashboard: Dashboard;

  @Input()
  widget: Widget;

  private settingsDirective: string;

  definedDirectiveError: string;

  widgetSettingsFormGroup: FormGroup;

  changeSubscription: Subscription;

  private definedSettingsComponentRef: ComponentRef<IWidgetSettingsComponent>;
  private definedSettingsComponent: IWidgetSettingsComponent;

  private widgetSettingsFormData: JsonFormComponentData;

  private propagateChange = (v: any) => { };

  constructor(private translate: TranslateService,
              private cfr: ComponentFactoryResolver,
              private widgetService: WidgetService,
              private fb: FormBuilder) {
    this.widgetSettingsFormGroup = this.fb.group({
      settings: [null, Validators.required]
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit(): void {
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (propName === 'widget') {
          if (this.definedSettingsComponent) {
            this.definedSettingsComponent.widget = this.widget;
          }
        }
        if (propName === 'dashboard') {
          if (this.definedSettingsComponent) {
            this.definedSettingsComponent.dashboard = this.dashboard;
          }
        }
        if (propName === 'aliasController') {
          if (this.definedSettingsComponent) {
            this.definedSettingsComponent.aliasController = this.aliasController;
          }
        }
      }
    }
  }

  ngOnDestroy(): void {
    if (this.definedSettingsComponentRef) {
      this.definedSettingsComponentRef.destroy();
    }
  }

  ngAfterViewInit(): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.widgetSettingsFormGroup.disable({emitEvent: false});
    } else {
      this.widgetSettingsFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: JsonFormComponentData): void {
    this.widgetSettingsFormData = value;
    if (this.changeSubscription) {
      this.changeSubscription.unsubscribe();
      this.changeSubscription = null;
    }
    if (this.settingsDirective !== this.widgetSettingsFormData.settingsDirective) {
      this.settingsDirective = this.widgetSettingsFormData.settingsDirective;
      this.validateDefinedDirective();
    }
    if (this.definedSettingsComponent) {
      this.definedSettingsComponent.settings = this.widgetSettingsFormData.model;
      this.changeSubscription = this.definedSettingsComponent.settingsChanged.subscribe((settings) => {
        this.updateModel(settings);
      });
    } else {
      this.widgetSettingsFormGroup.get('settings').patchValue(this.widgetSettingsFormData, {emitEvent: false});
      this.changeSubscription = this.widgetSettingsFormGroup.get('settings').valueChanges.subscribe(
        (widgetSettingsFormData: JsonFormComponentData) => {
          this.updateModel(widgetSettingsFormData.model);
        }
      );
    }
  }

  useDefinedDirective(): boolean {
    return this.settingsDirective &&
      this.settingsDirective.length && !this.definedDirectiveError;
  }

  useJsonForm(): boolean {
    return !this.settingsDirective || !this.settingsDirective.length;
  }

  private updateModel(settings: WidgetSettings) {
    this.widgetSettingsFormData.model = settings;
    if (this.definedSettingsComponent || this.widgetSettingsFormGroup.valid) {
      this.propagateChange(this.widgetSettingsFormData);
    } else {
      this.propagateChange(null);
    }
  }

  private validateDefinedDirective() {
    if (this.definedSettingsComponentRef) {
      this.definedSettingsComponentRef.destroy();
      this.definedSettingsComponentRef = null;
      this.definedSettingsComponent = null;
    }
    if (this.settingsDirective && this.settingsDirective.length) {
      const componentType = widgetSettingsComponentsMap[this.settingsDirective];
      if (!componentType) {
        this.definedDirectiveError = this.translate.instant('widget-config.settings-component-not-found',
          {selector: this.settingsDirective});
      } else {
        if (this.changeSubscription) {
          this.changeSubscription.unsubscribe();
          this.changeSubscription = null;
        }
        this.definedSettingsContainer.clear();
        const factory = this.cfr.resolveComponentFactory(componentType);
        this.definedSettingsComponentRef = this.definedSettingsContainer.createComponent(factory);
        this.definedSettingsComponent = this.definedSettingsComponentRef.instance;
        this.definedSettingsComponent.aliasController = this.aliasController;
        this.definedSettingsComponent.dashboard = this.dashboard;
        this.definedSettingsComponent.widget = this.widget;
        this.definedSettingsComponent.functionScopeVariables = this.widgetService.getWidgetScopeVariables();
        this.changeSubscription = this.definedSettingsComponent.settingsChanged.subscribe((settings) => {
          this.updateModel(settings);
        });
      }
    }
  }

  validate() {
    if (this.useDefinedDirective()) {
      this.definedSettingsComponent.validate();
    }
  }
}

import {Component} from '@angular/core';
import {WidgetSettings, WidgetSettingsComponent} from '@shared/models/widget.models';
import {FormBuilder, FormGroup} from '@angular/forms';
import {Store} from '@ngrx/store';
import {AppState} from '@core/core.state';

@Component({
  selector: 'tb-markdown-widget-settings',
  templateUrl: './markdown-widget-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class MarkdownWidgetSettingsComponent extends WidgetSettingsComponent {

  markdownWidgetSettingsForm: FormGroup;

  constructor(protected store: Store<AppState>,
              private fb: FormBuilder) {
    super(store);
  }

  protected settingsForm(): FormGroup {
    return this.markdownWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      useMarkdownTextFunction: false,
      markdownTextPattern: '# Markdown/HTML card \\n - **Current entity**: **${entityName}**. \\n - **Current value**: **${Random}**.',
      markdownTextFunction: 'return \'# Some title\\\\n - Entity name: \' + data[0][\'entityName\'];',
      markdownCss: ''
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.markdownWidgetSettingsForm = this.fb.group({
      useMarkdownTextFunction: [settings.useMarkdownTextFunction, []],
      markdownTextPattern: [settings.markdownTextPattern, []],
      markdownTextFunction: [settings.markdownTextFunction, []],
      markdownCss: [settings.markdownCss, []]
    });
  }

  protected validatorTriggers(): string[] {
    return ['useMarkdownTextFunction'];
  }

  protected updateValidators(emitEvent: boolean) {
    const useMarkdownTextFunction: boolean = this.markdownWidgetSettingsForm.get('useMarkdownTextFunction').value;
    if (useMarkdownTextFunction) {
      this.markdownWidgetSettingsForm.get('markdownTextPattern').disable();
      this.markdownWidgetSettingsForm.get('markdownTextFunction').enable();
    } else {
      this.markdownWidgetSettingsForm.get('markdownTextPattern').enable();
      this.markdownWidgetSettingsForm.get('markdownTextFunction').disable();
    }
    this.markdownWidgetSettingsForm.get('markdownTextPattern').updateValueAndValidity({emitEvent});
    this.markdownWidgetSettingsForm.get('markdownTextFunction').updateValueAndValidity({emitEvent});
  }

}

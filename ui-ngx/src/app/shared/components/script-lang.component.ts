import {Component, forwardRef, Input, OnInit, ViewEncapsulation} from '@angular/core';
import {ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR} from '@angular/forms';
import {PageComponent} from '@shared/components/page.component';
import {Store} from '@ngrx/store';
import {AppState} from '@core/core.state';
import {ScriptLanguage} from '@shared/models/rule-node.models';

@Component({
  selector: 'tb-script-lang',
  templateUrl: './script-lang.component.html',
  styleUrls: ['./script-lang.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => TbScriptLangComponent),
      multi: true
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class TbScriptLangComponent extends PageComponent implements ControlValueAccessor, OnInit {

  scriptLangFormGroup: FormGroup;

  scriptLanguage = ScriptLanguage;

  @Input()
  disabled: boolean;

  private propagateChange = null;

  constructor(protected store: Store<AppState>,
              private fb: FormBuilder) {
    super(store);
    this.scriptLangFormGroup = this.fb.group({
      scriptLang: [null]
    });
  }

  ngOnInit() {
    this.scriptLangFormGroup.get('scriptLang').valueChanges.subscribe(
      (scriptLang) => {
        this.updateView(scriptLang);
      }
    );
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.scriptLangFormGroup.disable({emitEvent: false});
    } else {
      this.scriptLangFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(scriptLang: ScriptLanguage): void {
    this.scriptLangFormGroup.get('scriptLang').patchValue(scriptLang, {emitEvent: false});
  }

  updateView(scriptLang: ScriptLanguage) {
    this.propagateChange(scriptLang);
  }
}

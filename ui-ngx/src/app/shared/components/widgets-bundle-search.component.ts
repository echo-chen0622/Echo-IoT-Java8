import { Component, ElementRef, forwardRef, Input, ViewChild, ViewEncapsulation } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

@Component({
  selector: 'tb-widgets-bundle-search',
  templateUrl: './widgets-bundle-search.component.html',
  styleUrls: ['./widgets-bundle-search.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => WidgetsBundleSearchComponent),
    multi: true
  }],
  encapsulation: ViewEncapsulation.None
})
export class WidgetsBundleSearchComponent implements ControlValueAccessor {

  searchText: string;
  focus = false;

  @Input() placeholder: string;

  @ViewChild('searchInput') searchInput: ElementRef<HTMLInputElement>;

  private propagateChange = (v: any) => { };

  constructor() {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  writeValue(value: string | null): void {
    this.searchText = value;
  }

  updateSearchText(): void {
    this.updateView();
  }

  private updateView() {
    this.propagateChange(this.searchText);
  }

  clear($event: Event): void {
    $event.preventDefault();
    $event.stopPropagation();
    this.searchText = '';
    this.updateView();
  }

  toggleFocus() {
    this.focus = !this.focus;
  }
}

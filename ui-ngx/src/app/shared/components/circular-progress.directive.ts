import {ComponentFactoryResolver, ComponentRef, Directive, ElementRef, Input, ViewContainerRef} from '@angular/core';
import {MatSpinner} from '@angular/material/progress-spinner';

@Directive({
  selector: '[tb-circular-progress]'
})
export class CircularProgressDirective {

  showProgressValue = false;

  children: JQuery<any>;

  cssWidth: any;

  @Input('tb-circular-progress')
  set showProgress(showProgress: boolean) {
    if (this.showProgressValue !== showProgress) {
      const element = this.elementRef.nativeElement;
      this.showProgressValue = showProgress;
      this.spinnerRef.instance._elementRef.nativeElement.style.display = showProgress ? 'block' : 'none';
      if (showProgress) {
        this.cssWidth = $(element).prop('style').width;
        if (!this.cssWidth) {
          $(element).css('width', '');
          const width = $(element).prop('offsetWidth');
          $(element).css('width', width + 'px');
        }
        this.children = $(element).children();
        $(element).empty();
        $(element).append($(this.spinnerRef.instance._elementRef.nativeElement));
      } else {
        $(element).empty();
        $(element).append(this.children);
        if (this.cssWidth) {
          $(element).css('width', this.cssWidth);
        } else {
          $(element).css('width', '');
        }
      }
    }
  }

  spinnerRef: ComponentRef<MatSpinner>;

  constructor(private elementRef: ElementRef,
              private componentFactoryResolver: ComponentFactoryResolver,
              private viewContainerRef: ViewContainerRef) {
    this.createCircularProgress();
  }

  createCircularProgress() {
    this.elementRef.nativeElement.style.position = 'relative';
    const factory = this.componentFactoryResolver.resolveComponentFactory(MatSpinner);
    this.spinnerRef = this.viewContainerRef.createComponent(factory, 0);
    this.spinnerRef.instance.mode = 'indeterminate';
    this.spinnerRef.instance.diameter = 20;
    const el = this.spinnerRef.instance._elementRef.nativeElement;
    el.style.margin = 'auto';
    el.style.position = 'absolute';
    el.style.left = '0';
    el.style.right = '0';
    el.style.top = '0';
    el.style.bottom = '0';
    el.style.display = 'none';
  }
}

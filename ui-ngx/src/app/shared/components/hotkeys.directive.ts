import {Directive, ElementRef, Input, OnDestroy, OnInit} from '@angular/core';
import {Hotkey} from 'angular2-hotkeys';
import * as Mousetrap from 'mousetrap';
import {MousetrapInstance} from 'mousetrap';
import {TbCheatSheetComponent} from '@shared/components/cheatsheet.component';

@Directive({
  selector : '[tb-hotkeys]'
})
export class TbHotkeysDirective implements OnInit, OnDestroy {
  @Input() hotkeys: Hotkey[] = [];
  @Input() cheatSheet: TbCheatSheetComponent;

  private mousetrap: MousetrapInstance;
  private hotkeysList: Hotkey[] = [];

  private preventIn = ['INPUT', 'SELECT', 'TEXTAREA'];

  constructor(private elementRef: ElementRef) {
    this.mousetrap = new Mousetrap(this.elementRef.nativeElement);
    (this.elementRef.nativeElement as HTMLElement).tabIndex = -1;
    (this.elementRef.nativeElement as HTMLElement).style.outline = '0';
  }

  ngOnInit() {
    for (const hotkey of this.hotkeys) {
      this.hotkeysList.push(hotkey);
      this.bindEvent(hotkey);
    }
    if (this.cheatSheet) {
      const hotkeyObj: Hotkey = new Hotkey(
        '?',
        (event: KeyboardEvent) => {
          this.cheatSheet.toggleCheatSheet();
          return false;
        },
        [],
        'Show / hide this help menu',
      );
      this.hotkeysList.unshift(hotkeyObj);
      this.bindEvent(hotkeyObj);
      this.cheatSheet.setHotKeys(this.hotkeysList);
    }
  }

  private bindEvent(hotkey: Hotkey): void {
    this.mousetrap.bind((hotkey as Hotkey).combo, (event: KeyboardEvent, combo: string) => {
      let shouldExecute = true;
      if (event) {
        const target: HTMLElement = (event.target || event.srcElement) as HTMLElement;
        const nodeName: string = target.nodeName.toUpperCase();
        if ((' ' + target.className + ' ').indexOf(' mousetrap ') > -1) {
          shouldExecute = true;
        } else if (this.preventIn.indexOf(nodeName) > -1 && (hotkey as Hotkey).
                   allowIn.map(allow => allow.toUpperCase()).indexOf(nodeName) === -1) {
          shouldExecute = false;
        }
      }

      if (shouldExecute) {
        return (hotkey as Hotkey).callback.apply(this, [event, combo]);
      }
    });
  }

  ngOnDestroy() {
    for (const hotkey of this.hotkeysList) {
      this.mousetrap.unbind(hotkey.combo);
    }
  }

}

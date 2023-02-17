import {Pipe, PipeTransform} from '@angular/core';

@Pipe({ name: 'highlight' })
export class HighlightPipe implements PipeTransform {
  transform(text: string, search): string {
    const pattern = search
      .replace(/[\-\[\]\/\{\}\(\)\*\+\?\.\\\^\$\|]/g, '\\$&');
    const regex = new RegExp('^' + pattern, 'i');

    return search ? text.replace(regex, match => `<b>${match}</b>`) : text;
  }
}

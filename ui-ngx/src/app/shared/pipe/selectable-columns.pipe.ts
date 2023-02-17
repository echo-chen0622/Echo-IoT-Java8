import { Pipe, PipeTransform } from '@angular/core';
import { DisplayColumn } from '@home/components/widget/lib/table-widget.models';

@Pipe({ name: 'selectableColumns' })
export class SelectableColumnsPipe implements PipeTransform {
  transform(allColumns: DisplayColumn[]): DisplayColumn[] {
    return allColumns.filter(column => column.selectable);
  }
}

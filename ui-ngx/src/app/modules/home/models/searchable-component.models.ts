export interface ISearchableComponent {
  onSearchTextUpdated(searchText: string);
}

export function instanceOfSearchableComponent(object: any): object is ISearchableComponent {
  return 'onSearchTextUpdated' in object;
}

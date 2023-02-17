interface JSTreeEventData {
  instance: JSTree;
}

interface JSTreeModelEventData extends JSTreeEventData {
  nodes: string[];
  parent: string;
}

interface JQuery {
  on(events: 'changed.jstree', handler: (e: Event, data: JSTreeEventData) => void): this;
  on(events: 'model.jstree', handler: (e: Event, data: JSTreeModelEventData) => void): this;
}

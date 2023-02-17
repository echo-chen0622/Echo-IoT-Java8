import {BreadCrumbLabelFunction} from '@shared/components/breadcrumb';
import {EntityDetailsPageComponent} from '@home/components/entity/entity-details-page.component';

export const entityDetailsPageBreadcrumbLabelFunction: BreadCrumbLabelFunction<EntityDetailsPageComponent>
  = ((route, translate, component) => {
  return component.entity?.name || component.headerSubtitle;
});

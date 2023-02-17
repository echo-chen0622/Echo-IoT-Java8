import { InjectionToken, Type } from '@angular/core';
import { ComponentType } from '@angular/cdk/portal';

export const SHARED_HOME_COMPONENTS_MODULE_TOKEN: InjectionToken<Type<any>> =
  new InjectionToken<Type<any>>('SHARED_HOME_COMPONENTS_MODULE_TOKEN');

export const HOME_COMPONENTS_MODULE_TOKEN: InjectionToken<Type<any>> =
  new InjectionToken<Type<any>>('HOME_COMPONENTS_MODULE_TOKEN');

export const COMPLEX_FILTER_PREDICATE_DIALOG_COMPONENT_TOKEN: InjectionToken<ComponentType<any>> =
  new InjectionToken<ComponentType<any>>('COMPLEX_FILTER_PREDICATE_DIALOG_COMPONENT_TOKEN');

export const DASHBOARD_PAGE_COMPONENT_TOKEN: InjectionToken<ComponentType<any>> =
  new InjectionToken<ComponentType<any>>('DASHBOARD_PAGE_COMPONENT_TOKEN');

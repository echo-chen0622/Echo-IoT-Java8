import { ComponentFactory, ComponentFactoryResolver, Injectable, Type } from '@angular/core';
import { deepClone } from '@core/utils';
import { IStateControllerComponent } from '@home/components/dashboard-page/states/state-controller.models';

export interface StateControllerData {
  factory: ComponentFactory<IStateControllerComponent>;
}

@Injectable()
export class StatesControllerService {

  statesControllers: {[stateControllerId: string]: StateControllerData} = {};

  statesControllerStates: {[stateControllerInstanceId: string]: any} = {};

  constructor(private componentFactoryResolver: ComponentFactoryResolver) {
  }

  public registerStatesController(stateControllerId: string, stateControllerComponent: Type<IStateControllerComponent>): void {
    const componentFactory = this.componentFactoryResolver.resolveComponentFactory(stateControllerComponent);
    this.statesControllers[stateControllerId] = {
      factory: componentFactory
    };
  }

  public getStateControllers(): {[stateControllerId: string]: StateControllerData} {
    return this.statesControllers;
  }

  public getStateController(stateControllerId: string): StateControllerData {
    return this.statesControllers[stateControllerId];
  }

  public preserveStateControllerState(stateControllerInstanceId: string, state: any) {
    this.statesControllerStates[stateControllerInstanceId] = deepClone(state);
  }

  public withdrawStateControllerState(stateControllerInstanceId: string): any {
    const state = this.statesControllerStates[stateControllerInstanceId];
    delete this.statesControllerStates[stateControllerInstanceId];
    return state;
  }

  public cleanupPreservedStates() {
    this.statesControllerStates = {};
  }
}

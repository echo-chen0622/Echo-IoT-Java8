import { Component } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityTabsComponent } from '../../components/entity/entity-tabs.component';
import { Customer } from '@shared/models/customer.model';

@Component({
  selector: 'tb-customer-tabs',
  templateUrl: './customer-tabs.component.html',
  styleUrls: []
})
export class CustomerTabsComponent extends EntityTabsComponent<Customer> {

  constructor(protected store: Store<AppState>) {
    super(store);
  }

  ngOnInit() {
    super.ngOnInit();
  }

}

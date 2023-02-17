import {BrowserModule} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {NgModule} from '@angular/core';

import {AppRoutingModule} from './app-routing.module';
import {CoreModule} from '@core/core.module';
import {LoginModule} from '@modules/login/login.module';
import {HomeModule} from '@home/home.module';

import {AppComponent} from './app.component';
import {DashboardRoutingModule} from '@modules/dashboard/dashboard-routing.module';
import {RouterModule, Routes} from '@angular/router';

const routes: Routes = [
  { path: '**',
    redirectTo: 'home'
  }
];

@NgModule({
  imports: [
    RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class PageNotFoundRoutingModule { }


@NgModule({
  declarations: [
    AppComponent
  ],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    AppRoutingModule,
    CoreModule,
    LoginModule,
    HomeModule,
    DashboardRoutingModule,
    PageNotFoundRoutingModule
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }

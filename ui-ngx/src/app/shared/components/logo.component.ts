import {Component} from '@angular/core';

@Component({
  selector: 'tb-logo',
  templateUrl: './logo.component.html',
  styleUrls: ['./logo.component.scss']
})
export class LogoComponent {

  logo = 'assets/logo_title_white.svg';

  gotoEchoiot(): void {
    window.open('https://echoiot.io', '_blank');
  }

}

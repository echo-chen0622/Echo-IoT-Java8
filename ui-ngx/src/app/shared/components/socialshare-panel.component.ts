import { Component, Input, OnInit } from '@angular/core';
import { isLocalUrl } from '@core/utils';

@Component({
  selector: 'tb-social-share-panel',
  templateUrl: './socialshare-panel.component.html',
  styleUrls: ['./socialshare-panel.component.scss']
})
export class SocialSharePanelComponent implements OnInit {

  @Input()
  shareTitle: string;

  @Input()
  shareText: string;

  @Input()
  shareLink: string;

  @Input()
  shareHashTags: string;

  constructor() {
  }

  ngOnInit(): void {
  }

  isShareLinkLocal(): boolean {
    if (this.shareLink && this.shareLink.length > 0) {
      return isLocalUrl(this.shareLink);
    } else {
      return true;
    }
  }

}

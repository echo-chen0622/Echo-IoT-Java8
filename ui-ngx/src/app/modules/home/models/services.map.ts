import { Type } from '@angular/core';
import { DeviceService } from '@core/http/device.service';
import { AssetService } from '@core/http/asset.service';
import { AttributeService } from '@core/http/attribute.service';
import { EntityRelationService } from '@core/http/entity-relation.service';
import { EntityService } from '@core/http/entity.service';
import { DialogService } from '@core/services/dialog.service';
import { CustomDialogService } from '@home/components/widget/dialog/custom-dialog.service';
import { DatePipe } from '@angular/common';
import { UtilsService } from '@core/services/utils.service';
import { TranslateService } from '@ngx-translate/core';
import { HttpClient } from '@angular/common/http';
import { EntityViewService } from '@core/http/entity-view.service';
import { CustomerService } from '@core/http/customer.service';
import { DashboardService } from '@core/http/dashboard.service';
import { UserService } from '@core/http/user.service';
import { AlarmService } from '@core/http/alarm.service';
import { Router } from '@angular/router';
import { BroadcastService } from '@core/services/broadcast.service';
import { ImportExportService } from '@home/components/import-export/import-export.service';
import { DeviceProfileService } from '@core/http/device-profile.service';
import { OtaPackageService } from '@core/http/ota-package.service';
import { AuthService } from '@core/auth/auth.service';
import { ResourceService } from '@core/http/resource.service';
import { TwoFactorAuthenticationService } from '@core/http/two-factor-authentication.service';
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';
import { MillisecondsToTimeStringPipe } from '@shared/pipe/milliseconds-to-time-string.pipe';

export const ServicesMap = new Map<string, Type<any>>(
  [
   ['broadcastService', BroadcastService],
   ['deviceService', DeviceService],
   ['alarmService', AlarmService],
   ['assetService', AssetService],
   ['entityViewService', EntityViewService],
   ['customerService', CustomerService],
   ['dashboardService', DashboardService],
   ['userService', UserService],
   ['attributeService', AttributeService],
   ['entityRelationService', EntityRelationService],
   ['entityService', EntityService],
   ['dialogs', DialogService],
   ['customDialog', CustomDialogService],
   ['date', DatePipe],
   ['milliSecondsToTimeString', MillisecondsToTimeStringPipe],
   ['utils', UtilsService],
   ['translate', TranslateService],
   ['http', HttpClient],
   ['router', Router],
   ['importExport', ImportExportService],
   ['deviceProfileService', DeviceProfileService],
   ['otaPackageService', OtaPackageService],
   ['authService', AuthService],
   ['resourceService', ResourceService],
   ['twoFactorAuthenticationService', TwoFactorAuthenticationService],
   ['telemetryWsService', TelemetryWebsocketService]
  ]
);

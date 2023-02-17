
export interface NotificationState {
  notification: NotificationMessage;
  hideNotification: HideNotification;
}

export declare type NotificationType = 'info' | 'warn' | 'success' | 'error';
export declare type NotificationHorizontalPosition = 'start' | 'center' | 'end' | 'left' | 'right';
export declare type NotificationVerticalPosition = 'top' | 'bottom';

export class NotificationMessage {
  message: string;
  type: NotificationType;
  target?: string;
  duration?: number;
  forceDismiss?: boolean;
  horizontalPosition?: NotificationHorizontalPosition;
  verticalPosition?: NotificationVerticalPosition;
  panelClass?: string | string[];
}

export class HideNotification {
  target?: string;
}

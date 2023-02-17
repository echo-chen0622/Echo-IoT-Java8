export class InterceptorConfig {
  constructor(public ignoreLoading: boolean = false,
              public ignoreErrors: boolean = false,
              public resendRequest: boolean = false) {}
}

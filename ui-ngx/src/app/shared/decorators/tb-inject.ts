import { Inject, Type } from '@angular/core';

export function TbInject<T>(token: any): (target: Type<T>, key: any, paramIndex: number) => void {
  return (target: Type<T>, key: any, paramIndex: number) => {
    Inject(token)(target, key, paramIndex);
  };
}

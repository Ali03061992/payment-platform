import { Pipe, PipeTransform } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

/** Stub de test : le pipe translate renvoie la clé telle quelle. */
@Pipe({ name: 'translate', standalone: false })
export class TranslateStubPipe implements PipeTransform {
  transform(value: string): string {
    return value;
  }
}

/** Stub de test : TranslateService.instant renvoie la clé telle quelle. */
export const TRANSLATE_SERVICE_STUB = {
  instant: (key: string) => key,
  get: (key: string) => key,
  use: () => {},
  setDefaultLang: () => {},
};

export function translateServiceProvider() {
  return { provide: TranslateService, useValue: TRANSLATE_SERVICE_STUB };
}

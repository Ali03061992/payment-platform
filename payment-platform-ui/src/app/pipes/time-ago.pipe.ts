import { Optional, Pipe, PipeTransform } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

export function timeAgo(dateStr: string): string {
  const now = Date.now();
  const then = new Date(dateStr).getTime();
  const diff = now - then;
  const mins = Math.floor(diff / 60000);
  if (mins < 1) return 'à l\'instant';
  if (mins < 60) return `il y a ${mins}min`;
  const hours = Math.floor(mins / 60);
  if (hours < 24) return `il y a ${hours}h`;
  const days = Math.floor(hours / 24);
  return `il y a ${days}j`;
}

@Pipe({
    name: 'timeAgo',
    standalone: false,
    pure: false
})
export class TimeAgoPipe implements PipeTransform {
  constructor(@Optional() private translate?: TranslateService) {}

  transform(value: string): string {
    if (this.translate) {
      const now = Date.now();
      const then = new Date(value).getTime();
      const mins = Math.floor((now - then) / 60000);
      if (mins < 1) return this.pick('LAYOUT.TIME_INSTANT', {}, 'à l\'instant');
      if (mins < 60) return this.pick('LAYOUT.TIME_MIN', { min: mins }, `il y a ${mins}min`);
      const hours = Math.floor(mins / 60);
      if (hours < 24) return this.pick('LAYOUT.TIME_HOURS', { h: hours }, `il y a ${hours}h`);
      const days = Math.floor(hours / 24);
      return this.pick('LAYOUT.TIME_DAYS', { d: days }, `il y a ${days}j`);
    }
    return timeAgo(value);
  }

  private pick(key: string, params: Record<string, number>, fallback: string): string {
    const v = this.translate!.instant(key, params);
    return v && v !== key ? v : fallback;
  }
}

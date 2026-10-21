import { Component, OnInit, OnDestroy, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { ChatbotService, ChatMessage } from './chatbot.service';

@Component({
    selector: 'app-chatbot',
    templateUrl: './chatbot.component.html',
    styleUrls: ['./chatbot.component.css'],
    standalone: false
})
export class ChatbotComponent implements OnInit, OnDestroy, AfterViewChecked {
  open = false;
  unread = 0;
  messages: ChatMessage[] = [];
  input = '';
  typing = false;

  @ViewChild('scrollBox') scrollBox!: ElementRef<HTMLDivElement>;
  private subs = new Subscription();
  private shouldScroll = false;

  constructor(private chatbot: ChatbotService, private router: Router) {}

  ngOnInit(): void {
    this.chatbot.start();
    this.subs.add(this.chatbot.messages$.subscribe((msgs) => {
      const prev = this.messages.length;
      this.messages = msgs;
      if (msgs.length > prev) {
        this.shouldScroll = true;
        const last = msgs[msgs.length - 1];
        if (last?.from === 'bot' && !this.open) this.unread++;
      }
    }));
  }

  ngOnDestroy(): void {
    this.subs.unsubscribe();
  }

  ngAfterViewChecked(): void {
    if (this.shouldScroll && this.scrollBox) {
      this.shouldScroll = false;
      try {
        this.scrollBox.nativeElement.scrollTop = this.scrollBox.nativeElement.scrollHeight;
      } catch {
        // ignore
      }
    }
  }

  toggle(): void {
    this.open = !this.open;
    if (this.open) this.unread = 0;
  }

  send(): void {
    const text = this.input.trim();
    if (!text) return;
    this.input = '';
    this.typing = true;
    // Petit délai "frappe" pour un rendu naturel (réponse synchrone du moteur local).
    setTimeout(() => {
      this.chatbot.send(text);
      this.typing = false;
    }, 350);
  }

  choose(option: string): void {
    this.typing = true;
    setTimeout(() => {
      this.chatbot.choose(option);
      this.typing = false;
    }, 350);
  }

  go(route: string): void {
    if (!route) return;
    this.open = false;
    this.router.navigate([route]);
  }

  trackByIndex(i: number): number {
    return i;
  }
}

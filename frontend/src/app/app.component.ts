import { Component } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <nav class="navbar">
      <div class="container nav-content">
        <a routerLink="/" class="logo">
          <span class="logo-icon">&#128270;</span>
          UDD Forensic
        </a>
        <div class="nav-links">
          <a routerLink="/search" routerLinkActive="active">Pretraga</a>
          <a routerLink="/upload" routerLinkActive="active">Novi Izveštaj</a>
          <a routerLink="/geo" routerLinkActive="active">Geo Pretraga</a>
          <a routerLink="/reports" routerLinkActive="active">Svi Izveštaji</a>
        </div>
      </div>
    </nav>
    <main class="container main-content">
      <router-outlet></router-outlet>
    </main>
  `,
  styles: [`
    .navbar {
      background: #1a1a2e;
      padding: 0 20px;
      position: sticky;
      top: 0;
      z-index: 1000;
      box-shadow: 0 2px 8px rgba(0,0,0,0.15);
    }
    .nav-content {
      display: flex;
      align-items: center;
      justify-content: space-between;
      height: 60px;
    }
    .logo {
      color: #ffffff;
      font-size: 20px;
      font-weight: 700;
      text-decoration: none;
      display: flex;
      align-items: center;
      gap: 8px;
    }
    .logo-icon { font-size: 24px; }
    .nav-links {
      display: flex;
      gap: 4px;
    }
    .nav-links a {
      color: #94a3b8;
      text-decoration: none;
      padding: 8px 16px;
      border-radius: 6px;
      font-size: 14px;
      font-weight: 500;
      transition: all 0.2s;
    }
    .nav-links a:hover {
      color: #ffffff;
      background: rgba(255,255,255,0.08);
    }
    .nav-links a.active {
      color: #ffffff;
      background: #4361ee;
    }
    .main-content {
      padding-top: 30px;
      padding-bottom: 40px;
    }
  `]
})
export class AppComponent {}

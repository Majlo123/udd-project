import { Component, AfterViewInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ForensicReportService } from '../../services/forensic-report.service';
import { ForensicReport } from '../../models/forensic-report.model';
import * as L from 'leaflet';

@Component({
  selector: 'app-map-search',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div style="margin-bottom: 24px;">
      <h1 style="font-size: 24px; font-weight: 700; color: #1a1a2e; margin-bottom: 6px;">
        Geolokacijska Pretraga
      </h1>
      <p style="color: #64748b; font-size: 14px;">
        Kliknite na mapu da postavite centar pretrage ili unesite koordinate ručno
      </p>
    </div>

    <div class="card">
      <div style="display: flex; gap: 16px; margin-bottom: 16px; flex-wrap: wrap; align-items: flex-end;">
        <div class="form-group" style="flex: 1; min-width: 140px; margin-bottom: 0;">
          <label>Latitude</label>
          <input type="number" step="0.0001" [(ngModel)]="latitude" placeholder="44.7866" />
        </div>
        <div class="form-group" style="flex: 1; min-width: 140px; margin-bottom: 0;">
          <label>Longitude</label>
          <input type="number" step="0.0001" [(ngModel)]="longitude" placeholder="20.4489" />
        </div>
        <div class="form-group" style="flex: 1; min-width: 120px; margin-bottom: 0;">
          <label>Radijus (km)</label>
          <input type="number" [(ngModel)]="radiusKm" placeholder="50" />
        </div>
        <button class="btn btn-primary" (click)="onSearch()" [disabled]="loading" style="height: 42px;">
          {{ loading ? 'Pretražujem...' : 'Pretraži u radijusu' }}
        </button>
      </div>

      <div id="map" style="height: 400px; border-radius: 8px; margin-bottom: 16px;"></div>

      <p style="font-size: 13px; color: #94a3b8;">
        Klikni na mapu za postavljanje centra pretrage. Rezultati su prikazani kao markeri.
      </p>
    </div>

    <!-- Results -->
    <div *ngIf="searched" style="margin-top: 20px;">
      <p style="font-size: 14px; color: #64748b; margin-bottom: 12px;">
        Pronađeno <strong>{{ results.length }}</strong> izveštaja u radijusu od {{ radiusKm }} km
      </p>

      <div *ngFor="let report of results" class="card" style="padding: 16px;">
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <div>
            <strong>{{ report.malwareName || 'N/A' }}</strong>
            <span style="margin-left: 8px; font-size: 12px; background: #f1f5f9; padding: 2px 8px; border-radius: 10px;">
              {{ report.classification }}
            </span>
          </div>
          <span style="font-size: 13px; color: #64748b;">
            &#128205; {{ report.city }}
          </span>
        </div>
        <div style="font-size: 13px; color: #64748b; margin-top: 6px;">
          {{ report.forensicInvestigator }} — {{ report.organization }}
        </div>
      </div>
    </div>
  `,
  styles: [`
    :host { display: block; }
  `]
})
export class MapSearchComponent implements AfterViewInit, OnDestroy {
  latitude = 44.7866;
  longitude = 20.4489;
  radiusKm = 50;
  results: ForensicReport[] = [];
  loading = false;
  searched = false;

  private map!: L.Map;
  private marker: L.Marker | null = null;
  private circle: L.Circle | null = null;
  private resultMarkers: L.Marker[] = [];

  constructor(private reportService: ForensicReportService) {}

  ngAfterViewInit(): void {
    this.initMap();
  }

  ngOnDestroy(): void {
    if (this.map) {
      this.map.remove();
    }
  }

  private initMap(): void {
    this.map = L.map('map').setView([this.latitude, this.longitude], 7);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; OpenStreetMap contributors'
    }).addTo(this.map);

    // Click handler to set search center
    this.map.on('click', (e: L.LeafletMouseEvent) => {
      this.latitude = parseFloat(e.latlng.lat.toFixed(4));
      this.longitude = parseFloat(e.latlng.lng.toFixed(4));
      this.updateMapMarker();
    });

    this.updateMapMarker();
  }

  private updateMapMarker(): void {
    if (this.marker) {
      this.map.removeLayer(this.marker);
    }
    if (this.circle) {
      this.map.removeLayer(this.circle);
    }

    this.marker = L.marker([this.latitude, this.longitude])
      .addTo(this.map)
      .bindPopup(`Centar pretrage: ${this.latitude}, ${this.longitude}`)
      .openPopup();

    this.circle = L.circle([this.latitude, this.longitude], {
      radius: this.radiusKm * 1000,
      color: '#4361ee',
      fillColor: '#4361ee',
      fillOpacity: 0.1
    }).addTo(this.map);
  }

  onSearch(): void {
    this.loading = true;
    this.searched = true;

    // Clear old result markers
    this.resultMarkers.forEach(m => this.map.removeLayer(m));
    this.resultMarkers = [];

    this.updateMapMarker();

    this.reportService.geoSearch({
      latitude: this.latitude,
      longitude: this.longitude,
      radiusKm: this.radiusKm
    }).subscribe({
      next: (data) => {
        this.results = data;
        this.loading = false;

        // Add markers for results
        data.forEach(report => {
          if (report.location) {
            const m = L.marker([report.location.lat, report.location.lon])
              .addTo(this.map)
              .bindPopup(`<strong>${report.malwareName || 'N/A'}</strong><br>${report.city}<br>${report.classification}`);
            this.resultMarkers.push(m);
          }
        });
      },
      error: (err) => {
        console.error('Geo search failed:', err);
        this.loading = false;
      }
    });
  }
}

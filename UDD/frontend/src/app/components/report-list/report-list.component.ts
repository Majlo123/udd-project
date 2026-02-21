import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ForensicReportService } from '../../services/forensic-report.service';
import { ForensicReport } from '../../models/forensic-report.model';

@Component({
  selector: 'app-report-list',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div style="margin-bottom: 24px;">
      <h1 style="font-size: 24px; font-weight: 700; color: #1a1a2e; margin-bottom: 6px;">
        Svi Forenzički Izveštaji
      </h1>
      <p style="color: #64748b; font-size: 14px;">
        Pregled svih indeksiranih izveštaja u Elasticsearch bazi
      </p>
    </div>

    <div *ngIf="loading" style="text-align: center; padding: 40px; color: #64748b;">
      Učitavanje izveštaja...
    </div>

    <div *ngIf="!loading && reports.length === 0" style="text-align: center; padding: 40px; color: #94a3b8;">
      Nema izveštaja u bazi. Kreirajte novi izveštaj putem "Novi Izveštaj" stranice.
    </div>

    <div class="card table-container" *ngIf="!loading && reports.length > 0">
      <table>
        <thead>
          <tr>
            <th>Malver</th>
            <th>Klasifikacija</th>
            <th>Istražitelj</th>
            <th>Organizacija</th>
            <th>Grad</th>
            <th>Akcije</th>
          </tr>
        </thead>
        <tbody>
          <tr *ngFor="let report of reports">
            <td><strong>{{ report.malwareName || 'N/A' }}</strong></td>
            <td>
              <span [class]="getClassBadge(report.classification)">
                {{ report.classification }}
              </span>
            </td>
            <td>{{ report.forensicInvestigator }}</td>
            <td>{{ report.organization }}</td>
            <td>{{ report.city }}</td>
            <td>
              <button class="btn btn-secondary" style="font-size: 12px; padding: 4px 10px; margin-right: 6px;"
                      (click)="downloadPdf(report.id)">
                &#x2B07; PDF
              </button>
              <button class="btn btn-danger" style="font-size: 12px; padding: 4px 10px;"
                      (click)="deleteReport(report.id)">
                &#x2716; Obriši
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  `,
  styles: [`
    :host { display: block; }
    .badge {
      display: inline-block;
      padding: 3px 8px;
      border-radius: 12px;
      font-size: 11px;
      font-weight: 600;
    }
    .badge-ransomware { background: #fee2e2; color: #991b1b; }
    .badge-phishing { background: #fef3c7; color: #92400e; }
    .badge-ddos { background: #dbeafe; color: #1e40af; }
    .badge-spyware { background: #ede9fe; color: #5b21b6; }
    .badge-default { background: #f1f5f9; color: #475569; }
  `]
})
export class ReportListComponent implements OnInit {
  reports: ForensicReport[] = [];
  loading = true;

  constructor(private reportService: ForensicReportService) {}

  ngOnInit(): void {
    this.loadReports();
  }

  loadReports(): void {
    this.loading = true;
    this.reportService.getAllReports().subscribe({
      next: (data) => {
        this.reports = data;
        this.loading = false;
      },
      error: (err) => {
        console.error('Failed to load reports:', err);
        this.loading = false;
      }
    });
  }

  getClassBadge(classification: string): string {
    switch (classification?.toLowerCase()) {
      case 'ransomware': return 'badge badge-ransomware';
      case 'phishing': return 'badge badge-phishing';
      case 'ddos': return 'badge badge-ddos';
      case 'spyware': return 'badge badge-spyware';
      default: return 'badge badge-default';
    }
  }

  downloadPdf(id: string): void {
    this.reportService.downloadPdf(id).subscribe(blob => {
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `report-${id}.pdf`;
      a.click();
      window.URL.revokeObjectURL(url);
    });
  }

  deleteReport(id: string): void {
    if (confirm('Da li ste sigurni da želite da obrišete ovaj izveštaj?')) {
      this.reportService.deleteReport(id).subscribe({
        next: () => this.loadReports(),
        error: (err) => console.error('Failed to delete:', err)
      });
    }
  }
}

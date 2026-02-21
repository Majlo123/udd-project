import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ForensicReportService } from '../../services/forensic-report.service';
import { ForensicReport, SearchRequest } from '../../models/forensic-report.model';

@Component({
  selector: 'app-search',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './search.component.html',
  styles: [`
    .search-header {
      text-align: center;
      margin-bottom: 30px;
    }
    .search-header h1 {
      font-size: 28px;
      font-weight: 700;
      color: #1a1a2e;
      margin-bottom: 8px;
    }
    .search-header p {
      color: #64748b;
      font-size: 15px;
    }
    .search-box {
      display: flex;
      gap: 12px;
      margin-bottom: 16px;
    }
    .search-input {
      flex: 1;
      padding: 12px 18px;
      border: 2px solid #e2e8f0;
      border-radius: 10px;
      font-size: 15px;
      font-family: inherit;
      transition: border-color 0.2s;
    }
    .search-input:focus {
      outline: none;
      border-color: #4361ee;
      box-shadow: 0 0 0 3px rgba(67, 97, 238, 0.1);
    }
    .toggle-row {
      display: flex;
      align-items: center;
      gap: 20px;
      margin-bottom: 20px;
      flex-wrap: wrap;
    }
    .toggle-item {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 14px;
      color: #475569;
    }
    .toggle-item input[type="checkbox"] {
      width: 18px;
      height: 18px;
      accent-color: #4361ee;
    }
    .geo-fields {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 12px;
      margin-bottom: 20px;
      padding: 16px;
      background: #f8fafc;
      border-radius: 8px;
    }
    .results-count {
      font-size: 14px;
      color: #64748b;
      margin-bottom: 16px;
    }
    .result-card {
      background: #fff;
      border: 1px solid #e2e8f0;
      border-radius: 10px;
      padding: 20px;
      margin-bottom: 14px;
      transition: box-shadow 0.2s;
    }
    .result-card:hover {
      box-shadow: 0 4px 12px rgba(0,0,0,0.08);
    }
    .result-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 10px;
    }
    .result-title {
      font-size: 16px;
      font-weight: 600;
      color: #1a1a2e;
    }
    .result-meta {
      display: flex;
      gap: 16px;
      flex-wrap: wrap;
      font-size: 13px;
      color: #64748b;
      margin-bottom: 8px;
    }
    .result-meta span {
      display: flex;
      align-items: center;
      gap: 4px;
    }
    .result-content {
      font-size: 14px;
      color: #475569;
      line-height: 1.5;
      max-height: 80px;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    .badge {
      display: inline-block;
      padding: 4px 10px;
      border-radius: 20px;
      font-size: 12px;
      font-weight: 600;
    }
    .badge-ransomware { background: #fee2e2; color: #991b1b; }
    .badge-phishing { background: #fef3c7; color: #92400e; }
    .badge-ddos { background: #dbeafe; color: #1e40af; }
    .badge-spyware { background: #ede9fe; color: #5b21b6; }
    .badge-default { background: #f1f5f9; color: #475569; }
    .help-box {
      background: #f0f4ff;
      border: 1px solid #c7d2fe;
      border-radius: 8px;
      padding: 16px;
      margin-bottom: 20px;
      font-size: 13px;
      color: #3730a3;
    }
    .help-box code {
      background: #e0e7ff;
      padding: 2px 6px;
      border-radius: 4px;
      font-size: 12px;
    }
    .loading {
      text-align: center;
      padding: 40px;
      color: #64748b;
    }
    .no-results {
      text-align: center;
      padding: 40px;
      color: #94a3b8;
      font-size: 15px;
    }
  `]
})
export class SearchComponent {
  query = '';
  advancedSearch = false;
  includeGeo = false;
  latitude: number | null = null;
  longitude: number | null = null;
  radiusKm: number | null = 50;
  results: ForensicReport[] = [];
  loading = false;
  searched = false;

  constructor(private reportService: ForensicReportService) {}

  onSearch(): void {
    if (!this.query.trim() && !this.includeGeo) return;

    this.loading = true;
    this.searched = true;

    const request: SearchRequest = {
      query: this.query,
      advancedSearch: this.advancedSearch,
      latitude: this.includeGeo && this.latitude ? this.latitude : undefined,
      longitude: this.includeGeo && this.longitude ? this.longitude : undefined,
      radiusKm: this.includeGeo && this.radiusKm ? this.radiusKm : undefined
    };

    this.reportService.search(request).subscribe({
      next: (data) => {
        this.results = data;
        this.loading = false;
      },
      error: (err) => {
        console.error('Search failed:', err);
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

  truncate(text: string, maxLen: number = 200): string {
    if (!text) return '';
    return text.length > maxLen ? text.substring(0, maxLen) + '...' : text;
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
}

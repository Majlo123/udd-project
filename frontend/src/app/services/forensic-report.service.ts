import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ForensicReport,
  ForensicReportDTO,
  SearchRequest,
  GeoSearchRequest
} from '../models/forensic-report.model';

@Injectable({
  providedIn: 'root'
})
export class ForensicReportService {

  private readonly apiUrl = '/api';

  constructor(private http: HttpClient) {}

  // ==================== Report CRUD ====================

  createReport(dto: ForensicReportDTO, file: File | null): Observable<ForensicReport> {
    const formData = new FormData();
    formData.append('metadata', new Blob([JSON.stringify(dto)], { type: 'application/json' }));
    if (file) {
      formData.append('file', file);
    }
    return this.http.post<ForensicReport>(`${this.apiUrl}/reports`, formData);
  }

  getAllReports(): Observable<ForensicReport[]> {
    return this.http.get<ForensicReport[]>(`${this.apiUrl}/reports`);
  }

  getReport(id: string): Observable<ForensicReport> {
    return this.http.get<ForensicReport>(`${this.apiUrl}/reports/${id}`);
  }

  deleteReport(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/reports/${id}`);
  }

  downloadPdf(id: string): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/reports/${id}/download`, {
      responseType: 'blob'
    });
  }

  // ==================== Search ====================

  search(request: SearchRequest): Observable<ForensicReport[]> {
    return this.http.post<ForensicReport[]>(`${this.apiUrl}/search`, request);
  }

  simpleSearch(query: string): Observable<ForensicReport[]> {
    const params = new HttpParams().set('q', query);
    return this.http.get<ForensicReport[]>(`${this.apiUrl}/search/simple`, { params });
  }

  geoSearch(request: GeoSearchRequest): Observable<ForensicReport[]> {
    return this.http.post<ForensicReport[]>(`${this.apiUrl}/search/geo`, request);
  }
}

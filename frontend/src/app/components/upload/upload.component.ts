import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ForensicReportService } from '../../services/forensic-report.service';
import { ForensicReportDTO } from '../../models/forensic-report.model';

@Component({
  selector: 'app-upload',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './upload.component.html',
  styles: [`
    .upload-header {
      margin-bottom: 24px;
    }
    .upload-header h1 {
      font-size: 24px;
      font-weight: 700;
      color: #1a1a2e;
      margin-bottom: 6px;
    }
    .upload-header p { color: #64748b; font-size: 14px; }
    .form-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 16px;
    }
    .form-full { grid-column: 1 / -1; }
    .file-drop {
      border: 2px dashed #cbd5e1;
      border-radius: 10px;
      padding: 40px;
      text-align: center;
      cursor: pointer;
      transition: all 0.2s;
      background: #f8fafc;
    }
    .file-drop:hover, .file-drop.active {
      border-color: #4361ee;
      background: #f0f4ff;
    }
    .file-drop p { color: #64748b; font-size: 14px; margin-top: 8px; }
    .file-name {
      font-weight: 600;
      color: #4361ee;
      margin-top: 8px;
    }
    .form-actions {
      display: flex;
      gap: 12px;
      margin-top: 20px;
    }
    .success-msg {
      background: #d1fae5;
      color: #065f46;
      padding: 12px 16px;
      border-radius: 8px;
      margin-bottom: 16px;
      font-size: 14px;
    }
    .error-msg {
      background: #fee2e2;
      color: #991b1b;
      padding: 12px 16px;
      border-radius: 8px;
      margin-bottom: 16px;
      font-size: 14px;
    }
    .pdf-toggle {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 14px 16px;
      background: #f0fdf4;
      border: 1px solid #bbf7d0;
      border-radius: 10px;
      margin-bottom: 8px;
    }
    .pdf-toggle.manual {
      background: #fefce8;
      border-color: #fef08a;
    }
    .pdf-toggle label {
      margin: 0;
      font-weight: 600;
      font-size: 14px;
      color: #1a1a2e;
    }
    .pdf-toggle small {
      color: #64748b;
      font-size: 12px;
    }
    .toggle-switch {
      position: relative;
      width: 44px;
      height: 24px;
      flex-shrink: 0;
    }
    .toggle-switch input { opacity: 0; width: 0; height: 0; }
    .toggle-slider {
      position: absolute;
      cursor: pointer;
      top: 0; left: 0; right: 0; bottom: 0;
      background-color: #cbd5e1;
      border-radius: 24px;
      transition: 0.3s;
    }
    .toggle-slider:before {
      position: absolute;
      content: "";
      height: 18px;
      width: 18px;
      left: 3px;
      bottom: 3px;
      background: white;
      border-radius: 50%;
      transition: 0.3s;
    }
    .toggle-switch input:checked + .toggle-slider {
      background-color: #22c55e;
    }
    .toggle-switch input:checked + .toggle-slider:before {
      transform: translateX(20px);
    }
  `]
})
export class UploadComponent {
  dto: ForensicReportDTO = {
    forensicInvestigator: '',
    organization: '',
    malwareName: '',
    description: '',
    classification: 'Ransomware',
    city: '',
    latitude: undefined,
    longitude: undefined
  };

  selectedFile: File | null = null;
  uploading = false;
  successMessage = '';
  errorMessage = '';
  autoGeneratePdf = true;  // podrazumevano: sistem sam generiše PDF

  classifications = ['Ransomware', 'Phishing', 'DDoS', 'Spyware', 'Trojan', 'Worm', 'Rootkit', 'Adware', 'Ostalo'];

  constructor(private reportService: ForensicReportService) {}

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.selectedFile = input.files[0];
    }
  }

  onSubmit(): void {
    if (!this.autoGeneratePdf && !this.selectedFile) {
      this.errorMessage = 'Morate izabrati PDF fajl ili uključiti automatsko generisanje.';
      return;
    }

    this.uploading = true;
    this.successMessage = '';
    this.errorMessage = '';

    const file = this.autoGeneratePdf ? null : this.selectedFile;

    this.reportService.createReport(this.dto, file).subscribe({
      next: (report) => {
        this.successMessage = `Izveštaj uspešno kreiran! ID: ${report.id}`;
        this.uploading = false;
        this.resetForm();
      },
      error: (err) => {
        this.errorMessage = 'Greška prilikom kreiranja izveštaja: ' + (err.error?.message || err.message);
        this.uploading = false;
      }
    });
  }

  resetForm(): void {
    this.dto = {
      forensicInvestigator: '',
      organization: '',
      malwareName: '',
      description: '',
      classification: 'Ransomware',
      city: '',
      latitude: undefined,
      longitude: undefined
    };
    this.selectedFile = null;
    this.autoGeneratePdf = true;
  }
}

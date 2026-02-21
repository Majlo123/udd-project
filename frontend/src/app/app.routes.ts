import { Routes } from '@angular/router';
import { SearchComponent } from './components/search/search.component';
import { UploadComponent } from './components/upload/upload.component';
import { MapSearchComponent } from './components/map-search/map-search.component';
import { ReportListComponent } from './components/report-list/report-list.component';

export const routes: Routes = [
  { path: '', redirectTo: '/search', pathMatch: 'full' },
  { path: 'search', component: SearchComponent },
  { path: 'upload', component: UploadComponent },
  { path: 'geo', component: MapSearchComponent },
  { path: 'reports', component: ReportListComponent },
];

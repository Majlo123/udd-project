export interface ForensicReport {
  id: string;
  forensicInvestigator: string;
  organization: string;
  malwareName: string;
  description: string;
  classification: string;
  fileHash: string;
  content: string;
  minioPath: string;
  city: string;
  location: GeoPoint | null;
}

export interface GeoPoint {
  lat: number;
  lon: number;
}

export interface ForensicReportDTO {
  forensicInvestigator: string;
  organization: string;
  malwareName: string;
  description: string;
  classification: string;
  fileHash?: string;
  city: string;
  latitude?: number;
  longitude?: number;
}

export interface SearchRequest {
  query: string;
  advancedSearch: boolean;
  latitude?: number;
  longitude?: number;
  radiusKm?: number;
}

export interface GeoSearchRequest {
  latitude: number;
  longitude: number;
  radiusKm: number;
}

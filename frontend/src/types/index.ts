export interface ClaimsRequest {
  encounterIds: string[];
}

export interface InsuranceRequestDTO {
  patientIds: string[];
}

export interface BenefitDetail {
  benefitType: string;
  coverageLevel: string;
  serviceType: string;
  inNetwork: boolean;
  amount: number | null;
  percent: number | null;
  timePeriod: string | null;
  message: string | null;
}

export interface EligibilityResponse {
  id: string;
  status: string;
  errorMessage: string | null;
  filePath: string | null;
  receivedAt: string;
  payerName: string;
  payerId: string;
  subscriberFirstName: string;
  subscriberLastName: string;
  memberId: string;
  groupNumber: string;
  eligibilityStatus: string;
  coverageStartDate: string;
  coverageEndDate: string;
  benefits: BenefitDetail[];
}

export interface SeedResult {
  practiceId: string;
  providerIds: string[];
  payerId: string;
  facilityIds: string[];
  patientIds: string[];
  insuranceIds: string[];
  encounterIds: string[];
}

export interface DiagnosisResponse {
  diagnosisCode: string;
  rank: number;
}

export interface ProcedureResponse {
  procedureCode: string;
  modifiers: string[];
  chargeAmount: number;
  units: number;
}

export interface EncounterResponse {
  id: string;
  patientId: string;
  patientName: string;
  providerId: string;
  providerName: string;
  facilityId: string;
  facilityName: string;
  dateOfService: string;
  authorizationNumber: string;
  diagnoses: DiagnosisResponse[];
  procedures: ProcedureResponse[];
}

export interface PatientResponse {
  id: string;
  firstName: string;
  lastName: string;
  dateOfBirth: string;
  gender: string;
  address: string;
  city: string;
  state: string;
  zipCode: string;
  phone: string;
}

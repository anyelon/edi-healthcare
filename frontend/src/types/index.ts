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
  needsAuth: boolean;
  clinicalReason: string | null;
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

export interface AuthorizationDecision {
  action: string;
  authorizationNumber: string | null;
  encounterId: string;
}

export interface PriorAuthResponse {
  payerName: string;
  payerId: string;
  subscriberFirstName: string;
  subscriberLastName: string;
  memberId: string;
  decisions: AuthorizationDecision[];
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

export interface SegmentError {
  segmentId: string;
  segmentPosition: number;
  segmentErrorCode: string;
  elementPosition: number;
  elementErrorCode: string | null;
  errorDescription: string;
}

export interface EDI999Acknowledgment {
  envelope: {
    senderIdQualifier: string;
    senderId: string;
    receiverIdQualifier: string;
    receiverId: string;
    date: string;
    time: string;
    controlNumber: string;
    ackRequested: string;
    usageIndicator: string;
  };
  functionalGroup: {
    senderId: string;
    receiverId: string;
    date: string;
    time: string;
    controlNumber: string;
  };
  acknowledgedGroupControlNumber: string;
  acknowledgedTransactionSetId: string;
  acknowledgedTransactionControlNumber: string;
  transactionStatus: "ACCEPTED" | "ACCEPTED_WITH_ERRORS" | "REJECTED";
  groupStatus: "ACCEPTED" | "ACCEPTED_WITH_ERRORS" | "REJECTED" | "PARTIALLY_ACCEPTED";
  transactionSetsIncluded: number;
  transactionSetsReceived: number;
  transactionSetsAccepted: number;
  errors: SegmentError[];
}

export enum PairingStatus {
  PENDING = 'PENDING',
  ACTIVE = 'ACTIVE',
  REVOKED = 'REVOKED',
}

export interface ParentChildDto {
  id: string;
  parentId: string;
  parentName: string;
  childId: string;
  childName: string;
  pairingStatus: PairingStatus;
  pairingDate: string;
  revokedAt: string | null;
}

export interface GeneratePairingCodeResponse {
  code: string;
  expiresIn: number;
  qrCodeDataUri: string;
}

export interface PairWithCodeRequest {
  code: string;
}

export interface ChildDashboardDto {
  childId: string;
  childName: string;
  averageScore: number;
  totalTests: number;
  recentAttempts: ChildAttemptDto[];
  subjectScores: SubjectScoreDto[];
}

export interface ChildAttemptDto {
  id: string;
  testTitle: string;
  score: number;
  maxScore: number;
  percentage: number;
  submittedAt: string;
}

export interface SubjectScoreDto {
  subjectId: string;
  subjectName: string;
  averageScore: number;
  totalAttempts: number;
}

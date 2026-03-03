export interface HomeworkSubmissionDto {
  id: string;
  homeworkId: string;
  studentId: string;
  studentName: string | null;
  textAnswer: string | null;
  fileUrl: string | null;
  fileName: string | null;
  submittedAt: string;
  grade: number | null;
  teacherComment: string | null;
  gradedAt: string | null;
  attemptNumber: number;
  status: 'SUBMITTED' | 'GRADED' | 'RETURNED';
}

export interface HomeworkDto {
  id: string;
  teacherId: string;
  teacherName: string | null;
  groupId: string | null;
  groupName: string | null;
  title: string;
  description: string | null;
  deadline: string | null;
  allowResubmission: boolean;
  fileRequired: boolean;
  maxFileSizeMb: number;
  status: 'ACTIVE' | 'CLOSED' | 'DRAFT';
  createdAt: string;
  totalSubmissions: number | null;
  gradedSubmissions: number | null;
  mySubmission: HomeworkSubmissionDto | null;
}

export interface CreateHomeworkRequest {
  title: string;
  description?: string;
  groupId?: string;
  deadline?: string;
  allowResubmission?: boolean;
  fileRequired?: boolean;
  maxFileSizeMb?: number;
}

export interface GradeSubmissionRequest {
  grade: number;
  teacherComment?: string;
}

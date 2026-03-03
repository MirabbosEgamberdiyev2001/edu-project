import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { homeworkApi } from '@/api/homeworkApi';
import type { CreateHomeworkRequest, GradeSubmissionRequest } from '@/types/homework';
import { useSnackbar } from 'notistack';

const KEYS = {
  teacherList: ['homeworks', 'teacher'] as const,
  studentList: (groupId?: string) => ['homeworks', 'student', groupId] as const,
  submissions: (id: string) => ['homeworks', id, 'submissions'] as const,
};

export function useTeacherHomeworks(page = 0, size = 20) {
  return useQuery({
    queryKey: [...KEYS.teacherList, page, size],
    queryFn: () => homeworkApi.getTeacherHomeworks(page, size).then((r) => r.data.data),
  });
}

export function useStudentHomeworks(groupId?: string, page = 0, size = 20) {
  return useQuery({
    queryKey: KEYS.studentList(groupId),
    queryFn: () => homeworkApi.getStudentHomeworks(groupId, page, size).then((r) => r.data.data),
  });
}

export function useHomeworkSubmissions(homeworkId: string, enabled = true) {
  return useQuery({
    queryKey: KEYS.submissions(homeworkId),
    queryFn: () => homeworkApi.getSubmissions(homeworkId).then((r) => r.data.data),
    enabled,
  });
}

export function useCreateHomework() {
  const qc = useQueryClient();
  const { enqueueSnackbar } = useSnackbar();
  return useMutation({
    mutationFn: (data: CreateHomeworkRequest) => homeworkApi.create(data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: KEYS.teacherList });
      enqueueSnackbar("Uy vazifasi yaratildi", { variant: 'success' });
    },
  });
}

export function useGradeSubmission(homeworkId: string) {
  const qc = useQueryClient();
  const { enqueueSnackbar } = useSnackbar();
  return useMutation({
    mutationFn: ({ submissionId, data }: { submissionId: string; data: GradeSubmissionRequest }) =>
      homeworkApi.gradeSubmission(submissionId, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: KEYS.submissions(homeworkId) });
      enqueueSnackbar("Baho saqlandi", { variant: 'success' });
    },
  });
}

export function useSubmitHomework() {
  const qc = useQueryClient();
  const { enqueueSnackbar } = useSnackbar();
  return useMutation({
    mutationFn: ({ homeworkId, textAnswer, file }: { homeworkId: string; textAnswer?: string; file?: File }) =>
      homeworkApi.submitHomework(homeworkId, textAnswer, file),
    onSuccess: (_data, vars) => {
      qc.invalidateQueries({ queryKey: KEYS.studentList() });
      enqueueSnackbar("Uy vazifasi topshirildi", { variant: 'success' });
    },
  });
}

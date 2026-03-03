import { useMutation, useQueryClient } from '@tanstack/react-query';
import { testTakingApi } from '@/api/testTakingApi';

export function useManualGrading(assignmentId: string) {
  const qc = useQueryClient();

  const mutation = useMutation({
    mutationFn: ({ answerId, score, feedback }: { answerId: string; score: number; feedback?: string }) =>
      testTakingApi.gradeAnswer({ answerId, score, feedback }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['assignments', assignmentId, 'results'] });
      qc.invalidateQueries({ queryKey: ['attempts'] });
    },
  });

  const gradeAnswer = (answerId: string, score: number, feedback?: string) =>
    mutation.mutateAsync({ answerId, score, feedback });

  return { gradeAnswer, isPending: mutation.isPending };
}

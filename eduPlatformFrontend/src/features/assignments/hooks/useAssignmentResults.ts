import { useQuery } from '@tanstack/react-query';
import { assignmentApi } from '@/api/assignmentApi';
import { useToast } from '@/hooks/useToast';

export function useAssignmentResults(assignmentId: string) {
  return useQuery({
    queryKey: ['assignments', assignmentId, 'results'],
    queryFn: async () => {
      const { data } = await assignmentApi.getResults(assignmentId);
      return data.data;
    },
    enabled: !!assignmentId,
  });
}

export function useExportResults() {
  const toast = useToast();

  const exportResults = async (assignmentId: string, format: 'CSV' | 'EXCEL') => {
    try {
      const response = await assignmentApi.exportResults(assignmentId, format);
      const blob = new Blob([response.data]);
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `results-${assignmentId}.${format === 'CSV' ? 'csv' : 'xlsx'}`;
      link.click();
      window.URL.revokeObjectURL(url);
    } catch {
      toast.error('Failed to export results');
    }
  };

  return { exportResults };
}

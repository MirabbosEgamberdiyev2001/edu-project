import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { analyticsApi } from '@/api/analyticsApi';
import { useToast } from '@/hooks/useToast';

export function useGroupStatistics(groupId: string) {
  return useQuery({
    queryKey: ['analytics', 'group', groupId],
    queryFn: async () => {
      const { data } = await analyticsApi.getGroupStatistics(groupId);
      return data.data;
    },
    enabled: !!groupId,
  });
}

export function useExportGroupStatistics() {
  const toast = useToast();
  const { t } = useTranslation('common');

  const exportGroupStats = async (groupId: string) => {
    try {
      const response = await analyticsApi.exportGroupStatistics(groupId);
      const blob = new Blob([response.data], { type: 'application/pdf' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `group-${groupId}-stats.pdf`;
      link.click();
      window.URL.revokeObjectURL(url);
    } catch {
      toast.error(t('error'));
    }
  };

  return { exportGroupStats };
}

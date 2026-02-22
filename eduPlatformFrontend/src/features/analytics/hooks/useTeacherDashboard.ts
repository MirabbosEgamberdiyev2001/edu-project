import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { analyticsApi } from '@/api/analyticsApi';
import { useToast } from '@/hooks/useToast';

export function useTeacherDashboard() {
  return useQuery({
    queryKey: ['analytics', 'teacher', 'dashboard'],
    queryFn: async () => {
      const { data } = await analyticsApi.getTeacherDashboard();
      return data.data;
    },
  });
}

export function useExportTeacherDashboard() {
  const toast = useToast();
  const { t } = useTranslation('common');

  const exportDashboard = async () => {
    try {
      const response = await analyticsApi.exportTeacherDashboard();
      const blob = new Blob([response.data], { type: 'application/pdf' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = 'teacher-dashboard.pdf';
      link.click();
      window.URL.revokeObjectURL(url);
    } catch {
      toast.error(t('error'));
    }
  };

  return { exportDashboard };
}

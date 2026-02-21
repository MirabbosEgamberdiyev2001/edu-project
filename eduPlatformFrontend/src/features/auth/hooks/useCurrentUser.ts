import { useQuery } from '@tanstack/react-query';
import { userApi } from '@/api/userApi';
import { useAuthStore } from '@/stores/authStore';

export function useCurrentUser() {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const setUser = useAuthStore((s) => s.setUser);

  return useQuery({
    queryKey: ['currentUser'],
    queryFn: async () => {
      const { data } = await userApi.getCurrentUser();
      setUser(data.data);
      return data.data;
    },
    enabled: isAuthenticated,
  });
}

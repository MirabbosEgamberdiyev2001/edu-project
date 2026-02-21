import { useAuthStore } from '@/stores/authStore';
import { storage } from '@/lib/storage';
import { useCallback } from 'react';

export function useAuth() {
  const { user, isAuthenticated, isHydrated, setAuth, logout: storeLogout } = useAuthStore();

  const logout = useCallback(() => {
    storeLogout();
    storage.clearAll();
  }, [storeLogout]);

  return { user, isAuthenticated, isHydrated, setAuth, logout };
}

import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { UserDto } from '@/types/user';

interface AuthState {
  user: UserDto | null;
  isAuthenticated: boolean;
  isHydrated: boolean;
  setAuth: (user: UserDto) => void;
  setUser: (user: UserDto) => void;
  logout: () => void;
  setHydrated: (hydrated: boolean) => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      isAuthenticated: false,
      isHydrated: false,

      setAuth: (user: UserDto) =>
        set({ user, isAuthenticated: true }),

      setUser: (user: UserDto) =>
        set({ user }),

      logout: () =>
        set({ user: null, isAuthenticated: false }),

      setHydrated: (hydrated: boolean) =>
        set({ isHydrated: hydrated }),
    }),
    {
      name: 'auth-storage',
      partialize: (state) => ({
        user: state.user,
        isAuthenticated: state.isAuthenticated,
      }),
      onRehydrateStorage: () => (state) => {
        state?.setHydrated(true);
      },
    },
  ),
);

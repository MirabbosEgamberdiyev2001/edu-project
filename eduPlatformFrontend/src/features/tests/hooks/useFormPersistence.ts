import { useState, useEffect } from 'react';

interface StoredData<T> {
  value: T;
  timestamp: number;
}

const STORAGE_EXPIRY_MS = 24 * 60 * 60 * 1000; // 24 hours

export function useFormPersistence<T>(key: string, initialValue: T) {
  const [value, setValue] = useState<T>(() => {
    try {
      const saved = localStorage.getItem(key);
      if (!saved) return initialValue;

      const parsed: StoredData<T> = JSON.parse(saved);
      const now = Date.now();

      // Check if data has expired (24 hours)
      if (now - parsed.timestamp > STORAGE_EXPIRY_MS) {
        localStorage.removeItem(key);
        return initialValue;
      }

      return parsed.value;
    } catch {
      return initialValue;
    }
  });

  useEffect(() => {
    try {
      const data: StoredData<T> = {
        value,
        timestamp: Date.now(),
      };
      localStorage.setItem(key, JSON.stringify(data));
    } catch (e) {
      // Handle quota exceeded or other localStorage errors
      console.warn(`Failed to persist form state to localStorage: ${e}`);
    }
  }, [key, value]);

  const clear = () => {
    localStorage.removeItem(key);
  };

  return [value, setValue, clear] as const;
}

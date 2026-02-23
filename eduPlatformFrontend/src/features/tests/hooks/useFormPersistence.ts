import { useState, useEffect } from 'react';

export function useFormPersistence<T>(key: string, initialValue: T) {
  const [value, setValue] = useState<T>(() => {
    try {
      const saved = sessionStorage.getItem(key);
      return saved ? JSON.parse(saved) : initialValue;
    } catch {
      return initialValue;
    }
  });

  useEffect(() => {
    sessionStorage.setItem(key, JSON.stringify(value));
  }, [key, value]);

  const clear = () => sessionStorage.removeItem(key);
  return [value, setValue, clear] as const;
}

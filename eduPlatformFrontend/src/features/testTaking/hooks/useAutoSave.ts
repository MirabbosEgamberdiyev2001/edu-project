import { useEffect, useRef, useCallback, useState } from 'react';
import { testTakingApi } from '@/api/testTakingApi';
import type { SubmitAnswerRequest } from '@/types/testTaking';

const AUTO_SAVE_INTERVAL = 30000; // 30 seconds — periodic backup
const DEBOUNCE_DELAY = 2000; // 2 seconds — flush shortly after last answer change

export type SaveStatus = 'idle' | 'pending' | 'saving' | 'saved' | 'error';

function getStorageKey(id: string) {
  return `exam_pending_${id}`;
}

export function useAutoSave(attemptId: string, enabled = true) {
  const pendingAnswers = useRef<Map<string, SubmitAnswerRequest>>(new Map());
  const isSaving = useRef(false);
  const debounceTimer = useRef<ReturnType<typeof setTimeout>>();
  const savedTimer = useRef<ReturnType<typeof setTimeout>>();

  const [saveStatus, setSaveStatus] = useState<SaveStatus>('idle');

  // Crash recovery: restore any in-flight answers from a previous session
  useEffect(() => {
    if (!attemptId) return;
    try {
      const raw = localStorage.getItem(getStorageKey(attemptId));
      if (raw) {
        const saved = JSON.parse(raw) as SubmitAnswerRequest[];
        saved.forEach((a) => pendingAnswers.current.set(a.questionId, a));
        if (saved.length > 0) setSaveStatus('pending');
      }
    } catch {
      // Corrupt entry — ignore
    }
  }, [attemptId]);

  const flush = useCallback(async (): Promise<boolean> => {
    if (isSaving.current || pendingAnswers.current.size === 0 || !attemptId) return true;

    isSaving.current = true;
    setSaveStatus('saving');
    const answers = Array.from(pendingAnswers.current.values());
    pendingAnswers.current.clear();

    try {
      await testTakingApi.batchSaveAnswers(attemptId, { answers });
      // Clear backup after confirmed server save
      try { localStorage.removeItem(getStorageKey(attemptId)); } catch { /* ignore */ }
      clearTimeout(savedTimer.current);
      setSaveStatus('saved');
      // Return to idle after 3 s so the indicator fades naturally
      savedTimer.current = setTimeout(() => setSaveStatus('idle'), 3000);
      return true;
    } catch {
      // Re-add failed answers so the next interval retries them
      answers.forEach((a) => pendingAnswers.current.set(a.questionId, a));
      setSaveStatus('error');
      return false;
    } finally {
      isSaving.current = false;
    }
  }, [attemptId]);

  const addAnswer = useCallback((answer: SubmitAnswerRequest) => {
    pendingAnswers.current.set(answer.questionId, answer);
    setSaveStatus('pending');

    // Persist to localStorage as crash / hard-refresh backup
    try {
      localStorage.setItem(
        getStorageKey(attemptId),
        JSON.stringify(Array.from(pendingAnswers.current.values())),
      );
    } catch {
      // Quota exceeded or private-mode — silently degrade
    }

    // Debounced flush: save 2 s after last answer change
    clearTimeout(debounceTimer.current);
    debounceTimer.current = setTimeout(flush, DEBOUNCE_DELAY);
  }, [flush, attemptId]);

  useEffect(() => {
    if (!enabled || !attemptId) return;

    const interval = setInterval(flush, AUTO_SAVE_INTERVAL);
    return () => {
      clearInterval(interval);
      clearTimeout(debounceTimer.current);
      clearTimeout(savedTimer.current);
      flush(); // Best-effort flush on unmount
    };
  }, [enabled, attemptId, flush]);

  return { addAnswer, flush, saveStatus };
}

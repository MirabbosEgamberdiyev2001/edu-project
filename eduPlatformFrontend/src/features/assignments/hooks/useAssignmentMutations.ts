import { useMutation, useQueryClient } from '@tanstack/react-query';
import { assignmentApi } from '@/api/assignmentApi';
import { useToast } from '@/hooks/useToast';
import type { CreateAssignmentRequest, UpdateAssignmentRequest } from '@/types/assignment';
import type { AxiosError } from 'axios';
import type { ApiError } from '@/types/api';

export function useAssignmentMutations() {
  const queryClient = useQueryClient();
  const toast = useToast();

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['assignments'] });

  const create = useMutation({
    mutationFn: (data: CreateAssignmentRequest) => assignmentApi.createAssignment(data),
    onSuccess: ({ data: resp }) => {
      toast.success(resp.message);
      invalidate();
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || 'Failed to create assignment');
    },
  });

  const update = useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateAssignmentRequest }) =>
      assignmentApi.updateAssignment(id, data),
    onSuccess: ({ data: resp }) => {
      toast.success(resp.message);
      invalidate();
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || 'Failed to update assignment');
    },
  });

  const activate = useMutation({
    mutationFn: (id: string) => assignmentApi.activateAssignment(id),
    onSuccess: ({ data: resp }) => {
      toast.success(resp.message);
      invalidate();
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || 'Failed to activate assignment');
    },
  });

  const cancel = useMutation({
    mutationFn: (id: string) => assignmentApi.cancelAssignment(id),
    onSuccess: ({ data: resp }) => {
      toast.success(resp.message);
      invalidate();
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || 'Failed to cancel assignment');
    },
  });

  const remove = useMutation({
    mutationFn: (id: string) => assignmentApi.deleteAssignment(id),
    onSuccess: ({ data: resp }) => {
      toast.success(resp.message);
      invalidate();
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || 'Failed to delete assignment');
    },
  });

  return { create, update, activate, cancel, remove };
}

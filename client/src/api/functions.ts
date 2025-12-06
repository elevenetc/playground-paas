import { apiClient } from './client';
import type { Function, CreateFunctionRequest, UpdateFunctionRequest } from '../types';

export const functionsApi = {
  getAll: async (): Promise<Function[]> => {
    const response = await apiClient.get<Function[]>('/api/functions');
    return response.data;
  },

  getByProjectId: async (projectId: string): Promise<Function[]> => {
    const response = await apiClient.get<Function[]>(`/api/projects/${projectId}/functions`);
    return response.data;
  },

  getById: async (projectId: string, functionId: string): Promise<Function> => {
    const response = await apiClient.get<Function>(
      `/api/projects/${projectId}/functions/${functionId}`
    );
    return response.data;
  },

  create: async (projectId: string, data: CreateFunctionRequest): Promise<Function> => {
    const response = await apiClient.post<Function>(
      `/api/projects/${projectId}/functions`,
      data
    );
    return response.data;
  },

  update: async (
    projectId: string,
    functionId: string,
    data: UpdateFunctionRequest
  ): Promise<Function> => {
    const response = await apiClient.put<Function>(
      `/api/projects/${projectId}/functions/${functionId}`,
      data
    );
    return response.data;
  },

  delete: async (projectId: string, functionId: string): Promise<void> => {
    await apiClient.delete(`/api/projects/${projectId}/functions/${functionId}`);
  },

  execute: async (projectId: string, functionId: string): Promise<void> => {
    await apiClient.post(`/api/projects/${projectId}/functions/${functionId}/execute`);
  },
};

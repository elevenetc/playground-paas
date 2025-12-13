// Types matching Kotlin DTOs from the backend

export interface Project {
  id: string;
  name: string;
  description: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateProjectRequest {
  name: string;
  description?: string | null;
}

export interface UpdateProjectRequest {
  name?: string | null;
  description?: string | null;
}

export interface FunctionParameter {
  name: string;
  type: string;
}

export enum FunctionStatus {
  PENDING = 'PENDING',
  COMPILING = 'COMPILING',
  BUILD_FAILED = 'BUILD_FAILED',
  DEPLOYMENT_FAILED = 'DEPLOYMENT_FAILED',
  SERVICE_FAILED = 'SERVICE_FAILED',
  READY = 'READY',
  RUN_FAILED = 'RUN_FAILED',
  STOPPED = 'STOPPED',
  STOPPING = 'STOPPING',
  DELETING = 'DELETING',
  DEPLOYMENT_DELETION_FAILED = 'DEPLOYMENT_DELETION_FAILED',
  SERVICE_DELETION_FAILED = 'SERVICE_DELETION_FAILED',
  DELETION_FAILED = 'DELETION_FAILED'
}

export interface Function {
  id: string;
  projectId: string;
  name: string;
  sourceCode: string;
  returnType: string;
  parameters: FunctionParameter[];
  status: FunctionStatus;
  containerName: string | null;
  containerId: string | null;
  port: number | null;
  imageTag: string | null;
  errorMessage: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateFunctionRequest {
  name?: string | null;
  sourceCode: string;
}

export interface UpdateFunctionRequest {
  name?: string | null;
  sourceCode?: string | null;
}

export interface ExecuteFunctionRequest {
  arguments: Record<string, string>;
}

export interface ExecuteFunctionResponse {
  result: string;
  executionTimeMs: number;
}

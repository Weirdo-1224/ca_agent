import { get, post } from './request';
import type {
  TaskCreateRequest,
  TaskDetailResponse,
  ReportResponse,
  Evidence,
  ReviewResult,
  AgentRunResponse,
  RepairDiffResponse,
} from '@/types';

export function createTask(req: TaskCreateRequest): Promise<TaskDetailResponse> {
  return post('/api/tasks', req);
}

export function getTaskDetail(taskId: string): Promise<TaskDetailResponse> {
  return get(`/api/tasks/${taskId}`);
}

export function getReport(taskId: string): Promise<ReportResponse> {
  return get(`/api/tasks/${taskId}/report`);
}

export function getEvidence(taskId: string): Promise<Evidence[]> {
  return get(`/api/tasks/${taskId}/evidence`);
}

export function getReview(taskId: string): Promise<ReviewResult> {
  return get(`/api/tasks/${taskId}/review`);
}

export function getAgentRuns(taskId: string): Promise<AgentRunResponse[]> {
  return get(`/api/tasks/${taskId}/agent-runs`);
}

export function getRepairDiffs(taskId: string): Promise<RepairDiffResponse> {
  return get(`/api/tasks/${taskId}/repair-diffs`);
}

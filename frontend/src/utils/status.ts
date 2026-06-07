import type { TaskStatus } from '@/types';

export const RUNNING_STATUSES: TaskStatus[] = [
  'CREATED',
  'PLANNING',
  'COLLECTING',
  'EXTRACTING',
  'ANALYZING',
  'WRITING',
  'REVIEWING',
  'REPAIRING',
];

export const TERMINAL_STATUSES: TaskStatus[] = [
  'COMPLETED',
  'COMPLETED_WITH_WARNINGS',
  'WAITING_HUMAN_REVIEW',
  'FAILED',
];

export function isRunningStatus(status?: string): boolean {
  return status ? (RUNNING_STATUSES as string[]).includes(status) : false;
}

export function isTerminalStatus(status?: string): boolean {
  return status ? (TERMINAL_STATUSES as string[]).includes(status) : false;
}

export function getStatusTagType(status?: string): 'success' | 'warning' | 'danger' | 'primary' | 'info' {
  switch (status) {
    case 'COMPLETED':
      return 'success';
    case 'COMPLETED_WITH_WARNINGS':
    case 'WAITING_HUMAN_REVIEW':
      return 'warning';
    case 'FAILED':
      return 'danger';
    case 'CREATED':
      return 'info';
    default:
      return isRunningStatus(status) ? 'primary' : 'info';
  }
}

export function getStatusText(status?: string): string {
  const map: Record<string, string> = {
    CREATED: '已创建',
    PLANNING: '规划中',
    COLLECTING: '采集中',
    EXTRACTING: '提取中',
    ANALYZING: '分析中',
    WRITING: '撰写中',
    REVIEWING: '质检中',
    REPAIRING: '修复中',
    WAITING_HUMAN_REVIEW: '等待人工审核',
    COMPLETED: '已完成',
    COMPLETED_WITH_WARNINGS: '已完成（有警告）',
    FAILED: '已失败',
  };
  return map[status || ''] || status || '未知';
}

export function getCurrentAgentByStatus(status?: string): string {
  const map: Record<string, string> = {
    CREATED: 'Task Created',
    PLANNING: 'PlannerAgent',
    COLLECTING: 'CollectorAgent',
    EXTRACTING: 'ExtractorAgent',
    ANALYZING: 'AnalyzerAgent',
    WRITING: 'WriterAgent',
    REVIEWING: 'ReviewerAgent',
    REPAIRING: 'RepairRouter',
    WAITING_HUMAN_REVIEW: 'Human Review',
    COMPLETED: 'Completed',
    COMPLETED_WITH_WARNINGS: 'Completed with Warnings',
    FAILED: 'Failed',
  };
  return map[status || ''] || '—';
}

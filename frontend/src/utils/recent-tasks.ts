const STORAGE_KEY = 'ca-agent.recent-task-ids';
const MAX_ITEMS = 10;

export interface RecentTask {
  taskId: string;
  taskName: string;
}

export function getRecentTasks(): RecentTask[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) return JSON.parse(raw) as RecentTask[];
  } catch { /* ignore */ }
  return [];
}

export function addRecentTask(taskId: string, taskName: string): RecentTask[] {
  const list = getRecentTasks().filter((t) => t.taskId !== taskId);
  list.unshift({ taskId, taskName });
  const trimmed = list.slice(0, MAX_ITEMS);
  localStorage.setItem(STORAGE_KEY, JSON.stringify(trimmed));
  return trimmed;
}

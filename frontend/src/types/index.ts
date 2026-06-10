export interface Result<T> {
  code: number;
  message: string | null;
  success: boolean;
  data: T;
}

export interface TaskCreateRequest {
  taskName: string;
  domain: string;
  targetProducts: string[];
  analysisGoal: string;
  outputFormat?: string;
  language?: string;
  maxIterations?: number;
}

export interface TaskDetailResponse {
  taskId: string;
  taskName: string;
  domain: string;
  targetProducts: string[];
  analysisGoal: string;
  status: TaskStatus;
  iterationCount: number;
  maxIterations: number;
  createdAt: string;
  updatedAt: string;
}

export type TaskStatus =
  | 'CREATED' | 'PLANNING' | 'COLLECTING' | 'EXTRACTING'
  | 'ANALYZING' | 'WRITING' | 'REVIEWING' | 'REPAIRING'
  | 'WAITING_HUMAN_REVIEW' | 'COMPLETED' | 'COMPLETED_WITH_WARNINGS' | 'FAILED';

export interface ReportResponse {
  taskId: string;
  reportTitle: string;
  reportFormat: string;
  sections: ReportSection[];
  sourceList: Evidence[];
  reviewResult: ReviewResult;
}

export interface ReportSection {
  sectionId: string;
  title: string;
  content: string;
  relatedClaimIds: string[];
  evidenceIds: string[];
}

export interface Evidence {
  evidenceId: string;
  productName: string;
  sourceType: string;
  sourceTitle: string;
  url: string;
  contentSnippet: string;
  collectedAt: string;
  reliability: string;
  usedFor: string[];
}

export interface ReviewResult {
  taskId: string;
  passed: boolean;
  score: number | null;
  summary: string | null;
  issues: ReviewIssue[];
  nextAction: NextAction | null;
}

export interface ReviewIssue {
  issueId: string;
  severity: string;
  type: string;
  description: string;
  targetAgent: string;
  targetProduct: string | null;
  targetDimension: string | null;
  repairInstruction: string | null;
}

export interface NextAction {
  action: string;
  targetAgent: string;
  reason: string;
}

export interface AgentRunResponse {
  runId: string;
  taskId: string;
  agentType: string;
  inputType: string;
  outputType: string;
  status: string;
  startTime: string;
  endTime: string;
  durationMs: number;
  errorMessage: string | null;
  promptTokens: number;
  completionTokens: number;
  totalTokens: number;
  llmCalls: LlmCallRecord[] | null;
}

export interface LlmCallRecord {
  systemPrompt: string;
  userPrompt: string;
  response: string;
  promptTokens: number;
  completionTokens: number;
  durationMs: number;
}

export interface RepairDiff {
  taskId: string;
  iteration: number;
  targetAgent: string;
  beforeScore: number | null;
  afterScore: number | null;
  beforeIssueCount: number | null;
  afterIssueCount: number | null;
  fixedIssueCount: number | null;
  addedEvidenceIds: string[];
  addedClaimIds: string[];
  changedSectionTitles: string[];
  changedProducts: string[];
  summary: string;
  createdAt: string;
}

export interface RepairDiffResponse {
  taskId: string;
  repairDiffs: RepairDiff[];
}

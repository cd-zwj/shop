import axios from 'axios';
import { getToken } from '../utils/token';
import { getCurrentAuthRole } from '../utils/authSession';
import { formatAuthToken } from './http';
import type { ApiResponse } from '../types/api';
import { ApiError } from '../types/api';

// RAG service proxy path — 单体后端 /api/rag/*
import { API_BASE_URL } from '../config/env';

const RAG_BASE_URL = `${API_BASE_URL}/rag`;

const ragHttp = axios.create({
  baseURL: RAG_BASE_URL,
  timeout: 60000,
});

// Request interceptor: inject auth token (same Sa-Token scheme as main backend)
ragHttp.interceptors.request.use((config) => {
  const role = getCurrentAuthRole();
  const token = role ? getToken(role) : null;
  if (token) {
    const authToken = formatAuthToken(token);
    config.headers.set('Authorization', authToken);
    config.headers.set('satoken', authToken);
  }
  return config;
});

// Response interceptor: unwrap standard ApiResponse envelope
ragHttp.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response) {
      const { status, data } = error.response;
      const msg = data?.message || `RAG service request failed (${status})`;
      return Promise.reject(new ApiError(msg, data?.code ?? status, status, data));
    }
    return Promise.reject(new ApiError('RAG service connection failed, check network'));
  },
);

async function unwrap<T>(p: Promise<{ data: ApiResponse<T> }>): Promise<T> {
  const { data } = await p;
  if (data.code !== 200) {
    throw new ApiError(data.message || 'RAG request failed', data.code, undefined, data);
  }
  return data.data;
}

// ============ Types ============


export type AiScenario =
  | 'GENERAL_RAG_QA'
  | 'USER_SHOPPING_ASSISTANT'
  | 'USER_WALLET_ADVISOR'
  | 'USER_ORDER_AFTERSALE'
  | 'MERCHANT_OPERATION_ASSISTANT'
  | 'MERCHANT_ORDER_ASSISTANT'
  | 'MERCHANT_MARKETING_ASSISTANT'
  | 'ADMIN_GOVERNANCE_ASSISTANT'
  | 'ADMIN_RISK_ASSISTANT';

export type AgentMode = 'AUTO' | 'REACT' | 'PLAN_EXECUTE';

export interface RagScenarioChatRequest {
  scenario: AiScenario;
  sessionId: string;
  message: string;
  turnCount?: number;
  modeHint?: AgentMode;
  approvedPlanId?: string;
}

export interface RagScenarioStreamRequest {
  url: string;
  method: 'POST';
  headers: Record<string, string>;
  body: string;
}
export interface RagSession {
  sessionId: string;
  userId: string;
  timestamp: number;
}

export interface SessionListResult {
  userId: string;
  sessions: string[];
  total: number;
  timestamp: number;
}

export interface MultiTurnResult {
  sessionId: string;
  turnCount: number;
  reply: string;
  hitKnowledge: boolean;
  referenceCount: number;
  timestamp: number;
}

export interface RagDocument {
  id: string;
  filename: string;
  fileHash: string;
  sourceType: string;
  fileSize: number;
  createdAt: string;
}

export interface DocumentPageResult {
  items: RagDocument[];
  total: number;
  page: number;
  pageSize: number;
  totalPages: number;
}

export interface UploadCheckResult {
  exists: boolean;
  sourceId?: string;
}

export interface ChunkCheckResult {
  uploadedChunks: number[];
  progress: number;
}

export interface UploadResult {
  filename: string;
  fileHash?: string;
  sourceId?: string;
  success: boolean;
  message: string;
}

export interface DeleteTaskResult {
  taskId: string;
  fileHash: string;
  message: string;
}

// ============ Chat APIs ============

/** Simple streaming chat */
export function ragSimpleChat(msg: string, userId: string): Promise<string> {
  return ragHttp.get('/ai/chatmemory/chat', {
    params: { msg, userId },
    responseType: 'text',
  }).then(r => r.data);
}

/** Get user session list */
export function ragSessionList(userId: string): Promise<SessionListResult> {
  return unwrap(ragHttp.post('/ai/session/list', { userId }));
}

/** Create new session */
export function ragSessionCreate(userId: string): Promise<RagSession> {
  return unwrap(ragHttp.post('/ai/session/create', { userId }));
}

/** Delete session */
export function ragSessionDelete(sessionId: string, userId: string): Promise<{ sessionId: string; message: string }> {
  return unwrap(ragHttp.post('/ai/session/delete', { sessionId, userId }));
}

/** Multi-turn chat */
export function ragMultiTurnChat(sessionId: string, message: string, turnCount = 0): Promise<MultiTurnResult> {
  return unwrap(ragHttp.post('/ai/multi-turn/chat', { sessionId, message, turnCount }));
}

/** Get session chat history */
export function ragSessionHistory(sessionId: string): Promise<Array<Record<string, unknown>>> {
  return unwrap(ragHttp.get('/ai/session/history', { params: { sessionId } }));
}

/** Submit AI message feedback (thumbs up/down) */
export function ragSubmitFeedback(params: {
  sessionId: string;
  messageIndex: number;
  feedbackType: 'UP' | 'DOWN' | 'REGENERATE';
}): Promise<string> {
  return unwrap(ragHttp.post('/ai/feedback', params));
}

/** Speech-to-text via ASR. Accepts audio Blob, returns transcribed text. */
export async function ragAsr(audioBlob: Blob): Promise<string> {
  const form = new FormData();
  form.append('file', audioBlob, 'recording.webm');
  const { data } = await ragHttp.post<{ code: number; message: string; data: string }>(
    '/ai/asr',
    form,
    { headers: { 'Content-Type': 'multipart/form-data' }, timeout: 30000 },
  );
  if (data.code !== 200) throw new ApiError(data.message || 'ASR request failed', data.code);
  return data.data;
}


/** Build unified scenario SSE request for fetch/EventSource-compatible callers. */
export function createRagScenarioStreamRequest(payload: RagScenarioChatRequest): RagScenarioStreamRequest {
  const role = getCurrentAuthRole();
  const token = role ? getToken(role) : null;
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    Accept: 'text/event-stream',
  };
  if (token) {
    const authToken = formatAuthToken(token);
    headers.Authorization = authToken;
    headers.satoken = authToken;
  }

  return {
    url: `${RAG_BASE_URL}/ai/scenario/chat`,
    method: 'POST',
    headers,
    body: JSON.stringify({
      scenario: payload.scenario,
      sessionId: payload.sessionId,
      message: payload.message,
      turnCount: payload.turnCount ?? 0,
      modeHint: payload.modeHint ?? 'AUTO',
      ...(payload.approvedPlanId ? { approvedPlanId: payload.approvedPlanId } : {}),
    }),
  };
}

/** Stream unified scenario AI chat. */
export async function ragScenarioChatStream(
  payload: RagScenarioChatRequest,
  onEvent: (event: { event: string; data: string }) => void,
): Promise<void> {
  const request = createRagScenarioStreamRequest(payload);
  const response = await fetch(request.url, request);
  if (!response.ok || !response.body) {
    throw new ApiError(`RAG scenario stream failed (${response.status})`, response.status, response.status);
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder('utf-8');
  let buffer = '';
  let completed = false;

  const emitEvent = (chunk: string) => {
    const event = parseSseChunk(chunk);
    if (!event) return;
    onEvent(event);
    if (event.event === 'done') {
      completed = true;
    }
  };

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    buffer += decoder.decode(value, { stream: true });
    const chunks = buffer.split('\n\n');
    buffer = chunks.pop() ?? '';
    for (const chunk of chunks) {
      emitEvent(chunk);
      if (completed) {
        await reader.cancel().catch(() => undefined);
        return;
      }
    }
  }

  emitEvent(buffer);
}

function parseSseChunk(chunk: string): { event: string; data: string } | null {
  if (!chunk.trim()) return null;
  let event = 'message';
  const data: string[] = [];
  for (const line of chunk.split('\n')) {
    if (line.startsWith('event:')) event = line.slice(6).trim();
    if (line.startsWith('data:')) data.push(line.slice(5).trimStart());
  }
  return { event, data: data.join('\n') };
}
// ============ Document APIs ============

/** Paginated document list */
export function ragDocumentList(params: {
  page?: number;
  pageSize?: number;
  sourceType?: string;
  keyword?: string;
  sortBy?: string;
  sortOrder?: string;
} = {}): Promise<DocumentPageResult> {
  return unwrap(ragHttp.get('/api/documents', { params }));
}

/** Delete document by hash */
export function ragDocumentDelete(fileHash: string): Promise<DeleteTaskResult> {
  return unwrap(ragHttp.delete(`/api/documents/${fileHash}`));
}

/** Check delete task status */
export function ragDocumentDeleteStatus(taskId: string): Promise<unknown> {
  return unwrap(ragHttp.get(`/api/documents/delete-status/${taskId}`));
}

// ============ Upload APIs ============

/** Check if file already exists */
export function ragUploadCheck(fileHash: string): Promise<UploadCheckResult> {
  return unwrap(ragHttp.get('/api/upload/check', { params: { fileHash } }));
}

/** Direct file upload */
export function ragUploadFile(file: File, fileHash: string): Promise<UploadResult> {
  const form = new FormData();
  form.append('file', file);
  form.append('fileHash', fileHash);
  return unwrap(ragHttp.post('/api/upload', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 300000,
  }));
}

/** Batch file upload (max 20 files) */
export function ragUploadBatch(files: File[], fileHashes: string[]): Promise<UploadResult[]> {
  const form = new FormData();
  files.forEach(f => form.append('files', f));
  fileHashes.forEach(h => form.append('fileHashes', h));
  return unwrap(ragHttp.post('/api/upload/batch', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 600000,
  }));
}

// ============ Chunk Upload APIs ============

/** Check chunk upload status */
export function ragChunkCheck(
  fileHash: string, filename: string, fileSize: number, totalChunks: number
): Promise<ChunkCheckResult> {
  return unwrap(ragHttp.get('/api/upload/chunk/check', {
    params: { fileHash, filename, fileSize, totalChunks },
  }));
}

/** Upload a single chunk */
export function ragChunkUpload(fileHash: string, chunkNumber: number, chunk: Blob): Promise<{ fileHash: string; chunkNumber: number; success: boolean }> {
  const form = new FormData();
  form.append('fileHash', fileHash);
  form.append('chunkNumber', String(chunkNumber));
  form.append('chunk', chunk);
  return unwrap(ragHttp.post('/api/upload/chunk', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  }));
}

/** Merge uploaded chunks */
export function ragChunkMerge(fileHash: string, filename: string): Promise<UploadResult> {
  const form = new FormData();
  form.append('fileHash', fileHash);
  form.append('filename', filename);
  return unwrap(ragHttp.post('/api/upload/chunk/merge', form));
}

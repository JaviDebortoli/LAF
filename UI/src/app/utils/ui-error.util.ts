export interface HttpLikeError {
  error?: unknown;
  message?: string;
}

const DEFAULT_BACKEND_ERROR_MESSAGE = 'Unexpected error while calling backend.';
const INVALID_BACKEND_PAYLOAD_MESSAGE =
  'Backend returned invalid JSON: expected graph, narrative, trace, and meta fields.';

const GRAPH_RENDER_ERROR_PREFIX = 'Failed to render graph';

export function mapInvalidBackendPayloadError(): string {
  return INVALID_BACKEND_PAYLOAD_MESSAGE;
}

export function mapBackendRequestError(error: HttpLikeError): string {
  if (typeof error.error === 'string') {
    return normalizeMessage(error.error, DEFAULT_BACKEND_ERROR_MESSAGE);
  }

  if (error.error && typeof error.error === 'object') {
    return JSON.stringify(error.error);
  }

  return normalizeMessage(error.message, DEFAULT_BACKEND_ERROR_MESSAGE);
}

export function mapGraphRenderError(error: unknown): string {
  const details = error instanceof Error ? error.message : String(error);
  return `${GRAPH_RENDER_ERROR_PREFIX}: ${details}`;
}

function normalizeMessage(value: string | undefined, fallback: string): string {
  const normalized = value?.trim();
  return normalized ? normalized : fallback;
}

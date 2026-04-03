import {
  mapBackendRequestError,
  mapGraphRenderError,
  mapInvalidBackendPayloadError,
} from './ui-error.util';

describe('ui-error.util', () => {
  it('should return backend string payload as-is (trimmed)', () => {
    expect(mapBackendRequestError({ error: '  Service unavailable  ' })).toBe('Service unavailable');
  });

  it('should serialize backend object payload', () => {
    expect(mapBackendRequestError({ error: { status: 500, detail: 'Oops' } })).toBe(
      '{"status":500,"detail":"Oops"}',
    );
  });

  it('should fallback to message or default for request errors', () => {
    expect(mapBackendRequestError({ message: '  Network error  ' })).toBe('Network error');
    expect(mapBackendRequestError({ message: '   ' })).toBe(
      'Unexpected error while calling backend.',
    );
  });

  it('should map invalid backend payload to a stable message', () => {
    expect(mapInvalidBackendPayloadError()).toBe(
      'Backend returned invalid JSON: expected graph, narrative, trace, and meta fields.',
    );
  });

  it('should map graph render errors consistently', () => {
    expect(mapGraphRenderError(new Error('Layout failed'))).toBe('Failed to render graph: Layout failed');
    expect(mapGraphRenderError('Unknown')).toBe('Failed to render graph: Unknown');
  });
});

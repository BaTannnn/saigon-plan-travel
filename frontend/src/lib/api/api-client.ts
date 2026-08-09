import type { ApiProblem } from "@/types/api";

export type RequestJsonOptions = {
  method?: string;
  body?: unknown;
  token?: string;
  cache?: RequestCache;
};

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly problem: ApiProblem | null,
  ) {
    super(problem?.detail ?? "Không thể kết nối đến API.");
    this.name = "ApiError";
  }
}

export async function requestJson<T>(
  url: string,
  options: RequestJsonOptions = {},
): Promise<T> {
  const hasBody = options.body !== undefined;
  const serializedBody = hasBody ? JSON.stringify(options.body) : undefined;
  const headers: Record<string, string> = {
    Accept: "application/json",
  };

  if (hasBody) {
    headers["Content-Type"] = "application/json";
  }

  if (options.token) {
    headers.Authorization = `Bearer ${options.token}`;
  }

  let response: Response;

  try {
    response = await fetch(url, {
      method: options.method,
      cache: options.cache,
      headers,
      body: serializedBody,
    });
  } catch {
    throw new ApiError(0, null);
  }

  if (!response.ok) {
    let problem: ApiProblem | null = null;

    try {
      problem = (await response.json()) as ApiProblem;
    } catch {
      // The status still provides a useful failure signal when the body is not JSON.
    }

    throw new ApiError(response.status, problem);
  }

  return (await response.json()) as T;
}

export const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "";
export const FRONTEND_SESSION_COOKIE = "ourmusic_frontend_session";

export function apiUrl(path: string) {
  const normalizedPath = path.startsWith("/") ? path : `/${path}`;
  return `${API_BASE_URL}${normalizedPath}`;
}

export function songArtworkUrl(songId: number) {
  return apiUrl(`/api/songs/${songId}/artwork`);
}

export function songStreamUrl(songId: number, playNonce: number) {
  return apiUrl(`/api/songs/${songId}/stream?v=${playNonce}`);
}

export type AuthSession = {
  id: number;
  username: string;
  isAdmin: boolean;
};

export type AuthResponse = {
  message?: string;
  id?: number;
  username?: string;
  isAdmin?: boolean;
};

function isAbortError(error: unknown) {
  return error instanceof DOMException && error.name === "AbortError";
}

async function readJson(response: Response) {
  try {
    return (await response.json()) as AuthResponse;
  } catch {
    return {};
  }
}

export async function fetchAuthSession(signal?: AbortSignal): Promise<AuthSession | null> {
  try {
    const response = await fetch(apiUrl("/api/auth/me"), {
      credentials: "include",
      signal,
    });

    if (!response.ok) {
      return null;
    }

    const data = await readJson(response);
    if (typeof data.id !== "number" || typeof data.username !== "string") {
      return null;
    }

    return {
      id: data.id,
      username: data.username,
      isAdmin: Boolean(data.isAdmin),
    };
  } catch (error) {
    if (isAbortError(error)) {
      return null;
    }

    throw error;
  }
}

export async function loginUser(username: string, password: string) {
  const response = await fetch(apiUrl("/api/auth/login"), {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    credentials: "include",
    body: JSON.stringify({ username, password }),
  });

  return { response, data: await readJson(response) };
}

export async function registerUser(username: string, password: string) {
  const response = await fetch(apiUrl("/api/auth/register"), {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    credentials: "include",
    body: JSON.stringify({ username, password }),
  });

  return { response, data: await readJson(response) };
}

export async function logoutUser() {
  const response = await fetch(apiUrl("/api/auth/logout"), {
    method: "POST",
    credentials: "include",
  });

  return { response, data: await readJson(response) };
}

export function setFrontendSessionCookie(username: string) {
  if (typeof document === "undefined") {
    return;
  }

  const cookieValue = encodeURIComponent(username);
  document.cookie = `${FRONTEND_SESSION_COOKIE}=${cookieValue}; Path=/; Max-Age=${60 * 60 * 24 * 30}; SameSite=Lax`;
}

export function clearFrontendSessionCookie() {
  if (typeof document === "undefined") {
    return;
  }

  document.cookie = `${FRONTEND_SESSION_COOKIE}=; Path=/; Max-Age=0; SameSite=Lax`;
}

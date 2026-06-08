import { NextRequest, NextResponse } from "next/server";

const AUTH_COOKIE = "ourmusic_frontend_session";

export function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;
  const sessionCookie = request.cookies.get(AUTH_COOKIE);
  const hasSession = Boolean(sessionCookie?.value);

  if (pathname === "/" || pathname.startsWith("/app")) {
    if (!hasSession) {
      const loginUrl = request.nextUrl.clone();
      loginUrl.pathname = "/login";
      loginUrl.search = "";
      return NextResponse.redirect(loginUrl);
    }
  }

  if (pathname === "/login" && hasSession) {
    const homeUrl = request.nextUrl.clone();
    homeUrl.pathname = "/";
    homeUrl.search = "";
    return NextResponse.redirect(homeUrl);
  }

  return NextResponse.next();
}

export const config = {
  matcher: ["/", "/login", "/app/:path*"],
};

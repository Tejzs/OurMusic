import type { NextConfig } from "next";

const publicApiBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL || "";
const rewriteApiBaseUrl = process.env.API_REWRITE_BASE_URL || publicApiBaseUrl || "http://localhost:8808";
const apiOrigin = new URL(rewriteApiBaseUrl);

const nextConfig: NextConfig = {
  allowedDevOrigins: [apiOrigin.hostname],
  async rewrites() {
    return [
      {
        source: "/api/:path*",
        destination: `${rewriteApiBaseUrl}/api/:path*`,
      },
    ];
  },
  images: {
    remotePatterns: [
      {
        protocol: apiOrigin.protocol.replace(":", "") as "http" | "https",
        hostname: apiOrigin.hostname,
        port: apiOrigin.port,
        pathname: "/api/**",
      },
    ],
  },
};

export default nextConfig;

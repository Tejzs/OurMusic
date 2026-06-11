import type { NextConfig } from "next";

const apiBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL || "http://192.168.1.76:8808";
const apiOrigin = new URL(apiBaseUrl);

const nextConfig: NextConfig = {
  allowedDevOrigins: [apiOrigin.hostname],
  async rewrites() {
    return [
      {
        source: "/api/:path*",
        destination: `${apiBaseUrl}/api/:path*`,
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

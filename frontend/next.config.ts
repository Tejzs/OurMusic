import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  allowedDevOrigins: ["192.168.1.76"],
  async rewrites() {
    return [
      {
        source: "/api/:path*",
        destination: "http://192.168.1.76:8808/api/:path*",
      },
    ];
  },
  images: {
    remotePatterns: [
      {
        protocol: "http",
        hostname: "192.168.1.76",
        port: "8808",
        pathname: "/api/**",
      },
    ],
  },
};

export default nextConfig;

import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig(({ command, mode }) => {
  const env = loadEnv(mode, process.cwd(), "");

  if (command === "serve" && !env.VITE_BACKEND_TARGET) {
    throw new Error("VITE_BACKEND_TARGET is required for local dev proxy.");
  }

  return {
    plugins: [react()],
    server: {
      port: 5173,
      proxy: {
        "/api": {
          target: env.VITE_BACKEND_TARGET,
          changeOrigin: true
        }
      }
    }
  };
});

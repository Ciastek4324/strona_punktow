import type { CapacitorConfig } from "@capacitor/cli";

const config: CapacitorConfig = {
  appId: "pl.punkty.app",
  appName: "Strona Punktow",
  webDir: "www",
  bundledWebRuntime: false,
  server: {
    url: "https://strona-punktow.onrender.com",
    cleartext: false
  },
  android: {
    allowMixedContent: false
  }
};

export default config;


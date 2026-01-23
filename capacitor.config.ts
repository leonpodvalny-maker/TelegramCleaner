import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.telegramcleaner.app',
  appName: 'Telegram Cleaner',
  webDir: 'www',
  server: {
    androidScheme: 'https'
  },
  android: {
    allowMixedContent: true,
    backgroundColor: '#343b54'
  },
  plugins: {
    TelegramCleanerPlugin: {
      enabled: true
    }
  }
};

export default config;

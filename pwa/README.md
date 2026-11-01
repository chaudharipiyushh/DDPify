# DPPify PWA (Progressive Web App) Setup Guide

This folder contains all the files needed to make your website (**https://progress-byte.vercel.app/**) a fully compliant, installable Progressive Web App (PWA) with offline caching and branded icons.

## Included Files:
1. `manifest.json` - PWA metadata (DPPify branding, Royal Blue `#0755D9`, Yellow `#FFC928`, portrait orientation, standalone display mode).
2. `service-worker.js` - Service worker providing network-first navigation with offline fallback, asset caching, and cache versioning.
3. `offline.html` - Branded offline fallback page featuring an interactive NCERT quick revision mini-quiz and auto-reconnect detector.
4. `icon-192.png`, `icon-192-maskable.png` - Standard and maskable 192x192 icons.
5. `icon-512.png`, `icon-512-maskable.png` - High-resolution 512x512 icons.

---

## How to Deploy to Vercel:

### Step 1: Copy Files to Your Web Project's `public/` Directory
Copy all the files from this `pwa/` folder into your website project's public folder (usually `public/` in Next.js, Vite, or static HTML):

```text
your-project/
├── public/
│   ├── manifest.json
│   ├── service-worker.js
│   ├── offline.html
│   ├── icon-192.png
│   ├── icon-192-maskable.png
│   ├── icon-512.png
│   └── icon-512-maskable.png
```

### Step 2: Reference the Manifest and Service Worker in Your HTML `<head>`
In your `index.html` (or `_document.tsx` / `layout.tsx`):

```html
<!-- PWA Manifest & Theme Color -->
<link rel="manifest" href="/manifest.json">
<meta name="theme-color" content="#0755D9">
<meta name="apple-mobile-web-app-capable" content="yes">
<meta name="apple-mobile-web-app-status-bar-style" content="default">
<meta name="apple-mobile-web-app-title" content="DPPify">
<link rel="apple-touch-icon" href="/icon-192.png">

<!-- Register Service Worker -->
<script>
  if ('serviceWorker' in navigator) {
    window.addEventListener('load', () => {
      navigator.serviceWorker.register('/service-worker.js')
        .then((reg) => console.log('DPPify SW registered:', reg.scope))
        .catch((err) => console.error('DPPify SW registration failed:', err));
    });
  }
</script>
```

### Step 3: Deploy to Vercel
Push your code to GitHub / Vercel:
```bash
git add .
git commit -m "feat: add DPPify PWA support with offline caching"
git push
```
Once deployed, visitors in mobile Chrome, Safari, or desktop browsers can install DPPify directly to their home screens!

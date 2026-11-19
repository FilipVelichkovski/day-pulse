# DayPulse

![AI-Generated](https://img.shields.io/badge/AI--Generated-LLM%20Code-blue)

> [!NOTE]
> **AI-Generated Code:** This repository and its codebase were generated with an LLM.

**DayPulse** is a WireGuard VPN-aware Android companion app and home screen widget that displays your daily desktop activity breakdown, live tracking status, top categories/apps, and recent sites/files.

It connects to the server provided by [FilipVelichkovski/activity-tracker](https://github.com/FilipVelichkovski/activity-tracker).

## Features

- **Today Dashboard**: Daily active time, live current app/category/title, category share breakdown, and top apps.
- **Activity Details**: Full app breakdown, top visited domains, and recent files.
- **Home Screen Widget (Glance)**: Compact widget showing today's total active time, live status, and top categories with manual/background refresh.
- **WireGuard VPN-Aware Sync**: Monitors active VPN tunnels (`VpnMonitor` + `VpnSyncService` + `WidgetSyncWorker`) and optionally restricts syncing to when a WireGuard/VPN tunnel is active (or allows direct LAN Wi-Fi).
- **Dynamic Server Configuration**: Configure the server Base URL (e.g., `http://10.0.0.2:7879`) and optional Bearer token directly in **Settings**.

## Server Compatibility

DayPulse expects a running [activity-tracker](https://github.com/FilipVelichkovski/activity-tracker) server exposing:

- `GET /api/v1/health` (unauthenticated)
- `GET /api/v1/status`
- `GET /api/v1/widget/today?top=<n>`
- `GET /api/v1/summary/today`

If your server is started with `--token`, configure the matching Bearer token in the app's **Settings** screen.

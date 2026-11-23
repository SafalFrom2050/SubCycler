# Feature Specification: AI-Powered In-App WebView Cancellation Co-Pilot 🤖🌐

> **Archived File**: `plans/features/postponed/AI_CANCELLATION_COPILOT_FEATURE.md`
> **Status**: Postponed
> **Target Android Versions**: Android 8.0 (API 26) to Android 15 (API 35)
> **Architecture**: Persistent In-App WebView + Cloud AI DOM/Vision Agent + Clean Architecture

> **Postponement note (2026-11-18):** Postponed indefinitely in favor of a strictly offline, zero-internet-permission architecture and the [Minimal Home UX & Assisted Subscribe/Unsubscribe Progress Notification](../MINIMAL_UX_AND_ACTION_NOTIFICATION_FEATURE.md) workflow. Instead of embedding WebViews or requiring network/cloud AI access, SubCycler posts a non-interruptive active notification with **Completed** / **Cancel** actions when the user clicks Subscribe or Unsubscribe, and persists the in-progress state locally so an easy recovery popup appears on the next app launch if the notification does not survive.

---

## 1. Executive Summary & Core Concept

Automating subscription cancellations on streaming platforms (Netflix, Disney+, Max, etc.) is notoriously difficult because of **CAPTCHAs, 2FA prompts, anti-bot heuristics, and retention dark patterns** (e.g. multi-step exit surveys, promotional discounts, and randomized CSS class names).

This feature solves this problem through a **Pre-Authenticated In-App WebView + Cloud AI Agent** architecture:
1. **Human-Handled Pre-Login**: The user signs into their streaming service once via SubCycler's embedded WebView. Android's native `CookieManager` persists the session locally for months.
2. **Zero-Touch Day 28 Navigation**: On cancellation day, SubCycler spins up a minimized/offscreen WebView with the existing authenticated session and navigates directly to the service's account/cancel portal.
3. **Semantic DOM Pruner**: Injected JavaScript extracts a 4KB clean semantic JSON tree of visible buttons and text (bypassing multi-megabyte HTML blobs).
4. **Cloud AI Reasoning**: A fast cloud reasoning model (e.g., Gemini 2.5 Flash) inspects the interactive tree, detects retention traps, and returns the next precise action (e.g., `click(element_id)`).
5. **100% On-Device Safety**: Credentials and passwords are **never stored or transmitted**. Only anonymized UI trees are sent for reasoning.

---

## 2. Technical Feasibility & Performance Footprint

| Dimension | Footprint / Specification | Impact |
| :--- | :--- | :--- |
| **RAM Consumption** | **60MB – 120MB (Transient)** | 🟢 Spikes only during the 15-second cancellation run; fully reclaimed upon `webView.destroy()`. |
| **CPU Usage** | **< 5%** | 🟢 Zero local ML matrix math; phone only renders HTML and evaluates lightweight JS. |
| **Battery Drain** | **< 0.05%** per cycle | 🟢 Negligible (equivalent to opening 1 webpage). |
| **Payload Size** | **~3KB – 8KB** per step | 🟢 Instant transfer over 4G/5G/Wi-Fi. |
| **End-to-End Latency** | **8 – 15 seconds** total | ⚡ 3–4 sequential clicks executed autonomously. |
| **Cloud API Cost** | **~$0.0005** per cancellation | 💰 Less than $0.03/year per active user. |

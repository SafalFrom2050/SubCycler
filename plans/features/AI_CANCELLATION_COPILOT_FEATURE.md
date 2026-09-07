# Feature Specification: AI-Powered In-App WebView Cancellation Co-Pilot 🤖🌐

> **Target File**: `plans/features/AI_CANCELLATION_COPILOT_FEATURE.md`
> **Status**: Ready for Implementation
> **Target Android Versions**: Android 8.0 (API 26) to Android 15 (API 35)
> **Architecture**: Persistent In-App WebView + Cloud AI DOM/Vision Agent + Clean Architecture

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

---

## 3. Architecture & Modular Breakdown

Following the project's Clean Architecture & SOLID standards:

```
SubCycler
 ├── :core:domain
 │    ├── model/AgentAction.kt                 (Click, SelectOption, InputText, VerifySuccess, RequestHumanHelp)
 │    ├── model/PrunedDomNode.kt               (Simplified interactive node data class)
 │    ├── model/CancellationSessionState.kt    (Active, Navigating, Analyzing, ActionExecuted, Succeeded, Failed)
 │    ├── repository/WebAutomationRepository.kt (Interface for WebView lifecycle & Cloud AI agent orchestration)
 │    └── usecase/ExecuteAiCancellationUseCase.kt (Drives the multi-step agent loop)
 │
 ├── :core:data
 │    ├── webview/AndroidWebViewManager.kt     (Manages persistent CookieManager, offscreen rendering, and JS injection)
 │    ├── webview/JsDomExtractor.kt            (Injected JavaScript to prune DOM to semantic JSON)
 │    ├── ai/CloudAiAgentClient.kt             (REST client communicating with Cloud AI API)
 │    ├── repository/WebAutomationRepositoryImpl.kt (Coordinates WebView, JS bridge, and AI client)
 │    └── di/AutomationModule.kt               (Hilt DI bindings)
 │
 ├── :core:ui
 │    ├── dialogs/ServiceLoginWebViewDialog.kt (User pre-login modal with cookie persistence)
 │    └── components/AiCoPilotProgressSheet.kt (OLED Obsidian progress modal with live step indicators)
 │
 └── :feature:dashboard / :feature:rotator
      ├── DashboardViewModel.kt                (Dispatches StartAiCancellation action)
      └── DashboardScreen.kt                   (Renders "Let AI Cancel" button and progress sheet)
```

---

## 4. Injected DOM Pruner Script (`JsDomExtractor.kt`)

Rather than sending 2.5MB of raw HTML to the AI, this script extracts only actionable interactive elements:

```javascript
(function() {
    let idCounter = 0;
    const elements = Array.from(document.querySelectorAll(
        'button, a, input, select, [role="button"], [role="radio"], [role="checkbox"], h1, h2, h3, p'
    )).filter(el => {
        const style = window.getComputedStyle(el);
        const rect = el.getBoundingClientRect();
        return style.display !== 'none' &&
               style.visibility !== 'hidden' &&
               style.opacity !== '0' &&
               rect.width > 0 && rect.height > 0;
    });

    const semanticTree = elements.map(el => {
        const currentId = idCounter++;
        el.setAttribute('data-subcycler-id', currentId);
        return {
            id: currentId,
            tag: el.tagName.toLowerCase(),
            role: el.getAttribute('role') || el.type || '',
            text: (el.innerText || el.value || el.getAttribute('aria-label') || '').trim().substring(0, 150)
        };
    }).filter(node => node.text.length > 0);

    return JSON.stringify({
        url: window.location.href,
        title: document.title,
        interactiveNodes: semanticTree
    });
})();
```

---

## 5. The Agent State Machine & Execution Loop

```mermaid
graph TD
    Start[User Taps 'Let AI Cancel' on Day 28] --> Init[Init Minimized WebView with Session Cookies]
    Init --> Nav[Navigate to Cancellation URL]
    Nav --> Extract[Inject JS DOM Pruner -> Extract 4KB JSON]
    Extract --> AI[Cloud AI Evaluates Page & Intent]

    AI -->|Action: Click Element ID| Exec[Execute JS Click on [data-subcycler-id]]
    Exec --> Wait[Wait for DOM Transition / Navigation]
    Wait --> Extract

    AI -->|Action: Success Banner Detected| Done[Mark Subscription as Cancelled in Room DB]
    Done --> UI_Success[Show Success Sheet: $15.49 Saved 🎉]

    AI -->|Action: 2FA / Re-Auth / CAPTCHA Detected| Fallback[Slide WebView Visible to User]
    Fallback --> Human[User Solves Prompt Manually -> AI Resumes]
```

### Agent Prompt Strategy:
```json
{
  "system_instruction": "You are SubCycler's Autonomous Cancellation Agent. Your goal is to safely cancel the recurring subscription or turn off auto-renew. You must bypass retention traps, skip optional surveys, and confirm the final cancellation. Return only structured JSON with the target element ID to click or mark success when cancellation confirmation is visible.",
  "expected_output_schema": {
    "thought": "The page is presenting an exit survey asking why I want to cancel. I will select 'Cost is too high' and click continue.",
    "action": "CLICK",
    "target_id": 14,
    "is_complete": false,
    "needs_human_intervention": false
  }
}
```

---

## 6. Safety, Security & Edge-Case Safeguards

1. **Zero Credential Exposure**:
   - The user enters their password directly into the streaming platform's official HTTPS form in the WebView. SubCycler never captures or reads input values from password fields (`input[type="password"]` is strictly excluded from DOM extraction).
2. **Strict 45-Second Watchdog Timer**:
   - If a page hangs, an internet drop occurs, or the site enters an unexpected loop, the agent aborts cleanly and notifies the user.
3. **Graceful Human-in-the-Loop Fallback**:
   - If the streaming service demands: *"Please re-enter your password to confirm"* or presents a Cloudflare puzzle, the AI signals `needs_human_intervention: true`. The bottom sheet smoothly expands the live WebView so the user can finish in 5 seconds.

---

## 7. UI / UX Design (OLED Obsidian Precision)

### Live Progress Bottom Sheet (`AiCoPilotProgressSheet.kt`)
- **Background**: `#0A0A0A` with subtle `1dp` outline `#1C1C1E`.
- **Live Progress Steps**:
  - `[✓] Connected to authenticated session`
  - `[✓] Navigated to Plan & Billing portal`
  - `[✓] Bypassed retention offer (Keep for $6.99)`
  - `[⚡] Confirming final cancellation...`
- **Result Hero**:
  - Sky Blue Pill (`#38BDF8`): `Netflix Paused • Save $15.49`
  - Countdown to Blackout End: `Next renewal scheduled for Oct 15th`

---

## 8. Verification & Testing Strategy

### Automated Unit Tests
- `CloudAiAgentClientTest`: Test JSON parsing of AI decision payloads with mocked API responses.
- `JsDomExtractorTest`: Verify that sensitive password inputs and hidden elements are filtered out.
- `ExecuteAiCancellationUseCaseTest`: Verify multi-step state machine transitions and database state updates using `StandardTestDispatcher`.

### Manual End-to-End Testing Matrix
| Test Scenario | Expected Outcome |
| :--- | :--- |
| **Valid Pre-Login Session** | Executes 3-step cancellation in <12s; marks Room DB as `isAutoRenewCancelled = true`. |
| **Expired Session Cookie** | Detects login form $\rightarrow$ prompts user with `ServiceLoginWebViewDialog` to sign in. |
| **Streaming Platform Retention Offer** | AI detects discount trap and chooses "Continue to Cancel" instead of "Accept Offer". |
| **Network Timeout / Broken Page** | Watchdog aborts at 45s; displays clear manual retry button with direct link. |

---

## 9. Step-by-Step Implementation Roadmap

- [ ] **Phase 1**: Implement `ServiceLoginWebViewDialog.kt` with persistent `CookieManager` in `:core:ui` & `:core:data`.
- [ ] **Phase 2**: Create `JsDomExtractor.kt` and inject semantic DOM extraction scripts.
- [ ] **Phase 3**: Build `CloudAiAgentClient.kt` with structured JSON tool-calling / reasoning prompts.
- [ ] **Phase 4**: Implement `ExecuteAiCancellationUseCase.kt` state machine and watchdog timers in `:core:domain`.
- [ ] **Phase 5**: Build `AiCoPilotProgressSheet.kt` and connect to Dashboard MVI actions.
- [ ] **Phase 6**: Comprehensive unit testing and simulated cancellation flow validation.

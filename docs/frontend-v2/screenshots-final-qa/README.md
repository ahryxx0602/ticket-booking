# Curated Final QA & Release Candidate Visual Evidence Manifest

This directory preserves the authoritative visual evidence verifying the 18 registered business views, data integrity guards, and visual compliance under the Claude+ Warm Minimalist Enterprise Mobility design system.

## Curated Screenshot Index

| Screenshot File | Target View / Capability | Verified Correctness Behavior |
|---|---|---|
| `trips-scheduling-view.png` | `OperatorTripsView.vue` | Authoritative trips table rendering only valid statuses (`SCHEDULED`, `DEPARTED`, `CANCELLED`). Zero mock/sample fallback. |
| `trip-schedule-modal.png` | `OperatorTripsView.vue` | Create partner trip modal with exact backend contract fields, station lookups, and vehicle selection. |
| `trip-buffer-violation.png` | `OperatorTripsView.vue` | 30-minute turnaround buffer validation blocking submission if new trip departs within 30 mins of vehicle arrival. |
| `fleet-vehicles-list.png` | `FleetAndDriversView.vue` | Vehicles list with authoritative statuses (`ACTIVE`, `MAINTENANCE`), seat calculations, and action guards. |
| `vehicle-modal-form.png` | `FleetAndDriversView.vue` | Vehicle create/edit modal with exact contract parameters; zero fabricated geometry fallback. |
| `vehicle-maintenance-dialog.png` | `FleetAndDriversView.vue` | Confirmation dialog for toggling maintenance/active status before dispatching mutation. |
| `fleet-drivers-list.png` | `FleetAndDriversView.vue` | Drivers list with verified legal and contact information. |
| `operator-profile-view.png` | `OperatorProfileView.vue` | Profile view displaying immutable legal fields as read-only and mutable contact fields with upload guard. |
| `operator-profile-mobile.png` | `OperatorProfileView.vue` | Responsive mobile layout compliance at 375px viewport. |
| `actual-withdrawal-confirmation-dialog.png` | `AdminWithdrawalsView.vue` | Payout confirmation dialog requiring explicit admin confirmation and mandatory rejection note. |
| `mutation-pending.png` | `AdminWithdrawalsView.vue` | Synchronous double-submission lock and pending spinner during inflight backend mutation. |
| `confirmed-authoritative-terminal-state.png` | `AdminWithdrawalsView.vue` | Authoritative `APPROVED` status update strictly confirmed by backend response. |
| `timeout-without-fabricated-terminal-status.png` | `AdminWithdrawalsView.vue` | Unknown outcome safety banner on network timeout; zero optimistic terminal mutation. |
| `reload-recovery.png` | `AdminWithdrawalsView.vue` | Persistent recovery from browser storage rehydrating unresolved operations on page reload. |
| `verification-failure.png` | `AdminWithdrawalsView.vue` | Read-only verification query maintaining safety lock when backend state remains ambiguous. |
| `correct-admin-trip-form.png` | `AdminSettingsView.vue` | Station and trip schedule management with strict input validation. |
| `station-mutation-rejection.png` | `AdminSettingsView.vue` | Handling backend business rejections with user-facing alerts and zero optimistic state change. |
| `runtime-settings-limitation.png` | `AdminSettingsView.vue` | Clear limitation disclosure for settings requiring backend reboot vs dynamic parameters. |
| `admin-operators-mobile.png` | `AdminOperatorsView.vue` | Responsive admin operator governance with 2-tier commission display. |

# OptiClean AI — Firestore Security Specification

## 1. Data Invariants
1. **Strict Tenant Isolation**: Every document in `/users/{userId}/room_tasks/{taskId}` and `/users/{userId}/inspections/{inspectionId}` belongs strictly to `userId`, and `request.auth.uid == userId` AND `data.userId == userId` must hold on all reads and writes.
2. **Path Hardening**: All `{userId}`, `{taskId}`, and `{inspectionId}` path IDs on single-document operations (`get`, `create`, `update`, `delete`) must match `^[a-zA-Z0-9_\-]+$` and be `<= 128` characters.
3. **Immutable Audit Identity & Timestamps**: `userId` and `createdAt` can never be mutated on `update`. All timestamps must be Firestore `timestamp` and `<= request.time`.
4. **Strict Attribute Bounds**:
   - `qualityScore` must be an `int` in `[0, 100]`.
   - `priority` must be in `['HIGH', 'MEDIUM', 'LOW']`.
   - `status` must be in `['PENDING', 'NEEDS_FIX', 'COMPLETED']`.
   - `iotSeverity` must be in `['CRITICAL', 'WARNING', 'STABLE']`.
   - Strings must enforce `.size()` upper bounds to block Denial-of-Wallet attacks.
5. **Shadow Field Rejection**: `keys().hasOnly(...)` on `create` and `diff().affectedKeys().hasOnly(...)` on `update` block any undeclared fields.

## 2. The "Dirty Dozen" Adversarial Payloads
1. **Unauthenticated List Scraping**: `unauthDb.collection("users/alice/room_tasks").get()` -> Rejected (`request.auth != null`).
2. **Cross-Tenant Read**: `bobDb.doc("users/alice/room_tasks/room_304").get()` -> Rejected (`request.auth.uid == userId`).
3. **Identity Spoofing on Create**: Alice writes to `users/alice/room_tasks/room_304` with `{"userId": "bob"}` -> Rejected (`incoming().userId == request.auth.uid`).
4. **Shadow/Ghost Field Injection**: Alice creates `room_tasks/room_304` with extra `"isAdmin": true` -> Rejected (`keys().hasOnly(...)`).
5. **Update Gap / Shadow Update**: Alice updates `room_tasks/room_304` adding `"hacked": "yes"` -> Rejected (`affectedKeys().hasOnly(...)`).
6. **Immutable `createdAt` Tampering**: Alice updates `room_tasks/room_304` with a modified `createdAt` -> Rejected (`incoming().createdAt == existing().createdAt`).
7. **Immutable `userId` Reassignment**: Alice updates `room_tasks/room_304` changing `userId` to `"bob"` -> Rejected (`incoming().userId == existing().userId`).
8. **Score Out-of-Bounds Poisoning**: Alice sets `qualityScore: 999` or `-10` -> Rejected (`qualityScore >= 0 && qualityScore <= 100`).
9. **Enum Poisoning**: Alice sets `status: "HACKED_STATE"` -> Rejected (`status in ['PENDING', 'NEEDS_FIX', 'COMPLETED']`).
10. **Oversized String DoW Attack**: Alice sends a 5,000-char `alertMessage` -> Rejected (`alertMessage.size() <= 500`).
11. **Epoch Long Timestamp Spoofing**: Alice sends `createdAt: 1700000000000` (int instead of server timestamp) -> Rejected (`createdAt is timestamp`).
12. **Future Timestamp Manipulation**: Alice sends `createdAt` 1 hour in the future -> Rejected (`createdAt <= request.time`).

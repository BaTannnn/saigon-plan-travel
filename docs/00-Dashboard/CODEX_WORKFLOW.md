# CODEX VIBE CODING WORKFLOW — SaigonPlanTravel

## 1. Mục tiêu

Dùng Codex để tăng tốc code nhưng không để agent tự mở rộng scope, sửa sai kiến trúc hoặc tạo code không kiểm soát.

Không dùng GitHub Spec Kit.

Workflow chính:

```text
Context
→ Grill With Docs
→ Feature Spec
→ File-by-file Plan
→ Small Implementation Steps
→ Test
→ Diff Review
→ Documentation Update
```

---

## 2. Khởi động Codex

Luôn mở từ root repository:

```bash
cd /home/batan/Project/saigon-plan-travel
codex
```

Kiểm tra skill:

```text
/skills
```

Skill chính:

```text
$grill-with-docs
```

---

## 3. Prompt kiểm tra context đầu phiên

```text
Read:
- @AGENTS.md
- @docs/00-Dashboard/PROJECT_CONTEXT.md
- the nearest relevant AGENTS.md

Summarize:
1. Current project state.
2. Non-negotiable constraints.
3. Current milestone.
4. Relevant files to inspect.

Do not modify files.
```

---

## 4. Bắt đầu feature lớn

```text
$grill-with-docs

Tôi muốn thực hiện [TÊN FEATURE].

Hãy đọc:
- @AGENTS.md
- @docs/00-Dashboard/PROJECT_CONTEXT.md
- feature spec liên quan nếu có
- source code liên quan

Hỏi tôi đúng một câu mỗi lần.
Không viết production code.
Kết thúc bằng locked requirements và chờ phê duyệt.
```

---

## 5. Tạo feature spec

Sau khi phê duyệt:

```text
Tạo hoặc cập nhật feature spec tại:
docs/01-Requirements/Features/[FILE].md

Dùng:
@docs/01-Requirements/Features/FEATURE_TEMPLATE.md

Không sửa production code.
```

---

## 6. Lập kế hoạch

```text
Đọc:
- @AGENTS.md
- @docs/00-Dashboard/PROJECT_CONTEXT.md
- @docs/01-Requirements/Features/[FEATURE].md
- source code liên quan

Hãy tạo kế hoạch file-by-file gồm:
1. File tạo/sửa.
2. Migration.
3. Entity/DTO.
4. Repository.
5. Service.
6. Controller/API.
7. Tests.
8. Lệnh verification.
9. Rủi ro.

Không viết code.
Không mở rộng scope.
```

---

## 7. Triển khai từng bước

Không giao toàn bộ feature trong một prompt nếu feature lớn.

Ví dụ:

```text
Goal:
Implement only the database migration for F01.

Context:
- @AGENTS.md
- @backend/AGENTS.md
- @docs/00-Dashboard/PROJECT_CONTEXT.md
- @docs/01-Requirements/Features/F01-Place-Catalog.md

Constraints:
- Do not create Java classes yet.
- Do not edit existing applied migrations.
- Use Flyway naming based on current repository history.
- Do not add Category or OpeningHour.

Done when:
- Migration is valid.
- Constraints match the approved spec.
- Diff contains only expected files.

Verify:
cd backend && ./mvnw test
```

Các bước tiếp theo:

```text
Migration
→ Entity
→ Repository
→ DTO/Mapper
→ Service
→ Controller
→ Tests
→ Documentation
```

---

## 8. Review sau mỗi bước

Yêu cầu Codex:

```text
Stop implementation.

Show:
1. Files changed.
2. Summary of behavior.
3. Assumptions.
4. Commands run and results.
5. Remaining tasks.
6. Any deviation from the approved spec.
```

Kiểm tra Git:

```bash
git status
git diff
```

---

## 9. Hoàn tất feature

```text
Review the completed feature against:
- @docs/01-Requirements/Features/[FEATURE].md
- @AGENTS.md
- @backend/AGENTS.md

Do not add new behavior.

Report:
1. Acceptance criteria status.
2. Tests run.
3. Known limitations.
4. Required documentation updates.
5. Thesis section mapping.
```

Sau đó cập nhật:

- Feature spec checkboxes.
- Development log.
- API/database note.
- `PROJECT_CONTEXT.md`.

---

## 10. Khi không cần grill

Có thể prompt trực tiếp nếu:

- sửa typo;
- lỗi compile rõ ràng;
- đổi tên biến;
- refactor nhỏ không đổi behavior;
- thêm validation đã có trong spec.

Vẫn phải cung cấp Goal, Context, Constraints, Done when và Verify.

---

## 11. Quy tắc an toàn

- Không giao lệnh “làm toàn bộ dự án”.
- Không chấp nhận thay đổi ngoài diff dự kiến.
- Không để agent tự quyết định thêm dependency lớn.
- Không cho phép sửa migration đã áp dụng.
- Không commit secret.
- Không đánh dấu hoàn thành khi chưa test.
- Không để agent tự bịa dữ liệu địa điểm.

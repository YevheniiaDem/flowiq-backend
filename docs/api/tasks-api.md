# Tasks API

**Controller:** `com.flowiq.tasks.controller.TaskController`  
**Base path:** `/api/tasks`  
**Auth:** JWT required

## Endpoints

| Method | Path | Body/Params | Response | Status |
|--------|------|-------------|----------|--------|
| GET | `/` | `page`, `size`, `search`, `type`, `priority`, `status`, `dueDateFrom`, `dueDateTo`, `sort` | `TaskPageResponse` | 200 |
| GET | `/today` | — | `List<TaskResponse>` | 200 |
| GET | `/upcoming` | — | `List<TaskResponse>` (30 days) | 200 |
| GET | `/grouped` | — | `TaskListResponse` | 200 |
| GET | `/suggestions` | — | `List<TaskSuggestionResponse>` | 200 |
| POST | `/` | `CreateTaskRequest` | `TaskResponse` | 201 |
| PUT | `/{id}` | `UpdateTaskRequest` | `TaskResponse` | 200 |
| PUT | `/{id}/complete` | — | `TaskResponse` | 200 |
| DELETE | `/{id}` | — | — | 204 |

## Enums

**TaskType:** `TAX`, `REPORTING`, `BUSINESS`, `CUSTOM`, `SYSTEM`  
**TaskPriority:** `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`  
**TaskStatus:** `TODO`, `IN_PROGRESS`, `COMPLETED`, `OVERDUE`

## Example: Create Task

```http
POST /api/tasks
Content-Type: application/json

{
  "title": "Submit Q2 unified tax declaration",
  "description": "Due within 40 days after quarter end",
  "type": "TAX",
  "priority": "HIGH",
  "dueDate": "2026-07-20"
}
```

## Example: Grouped Response

```json
{
  "today": [...],
  "upcoming": [...],
  "overdue": [...],
  "completed": [...]
}
```

## Auto-Generation

Not exposed via API — triggered by `DailyTaskScheduler` (07:30 daily) and import/report flows.

## Related

- [Tasks Center Module](../modules/tasks-center.md)
- [Dashboard tasks snapshot](dashboard-api.md)

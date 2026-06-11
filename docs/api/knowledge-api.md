# Knowledge / Business Guide API

**Controller:** `com.flowiq.knowledge.controller.BusinessGuideController`  
**Base path:** `/api/business-guide`  
**Auth:** JWT required

## Endpoints

| Method | Path | Params | Response |
|--------|------|--------|----------|
| GET | `/articles` | `page`, `size`, `category`, `tag` | `KnowledgeArticlePageDto` |
| GET | `/articles/{slug}` | `slug` | `KnowledgeArticleDetailDto` |
| GET | `/categories` | — | `List<KnowledgeCategoryDto>` |
| GET | `/search` | `q`, `page`, `size`, `category` | `KnowledgeSearchResponseDto` |
| GET | `/dashboard-snapshot` | — | `KnowledgeDashboardSnapshotDto` |

Also available via dashboard: `GET /api/dashboard/business-guide-snapshot`

## Categories (`KnowledgeCategory`)

`FOP_GROUPS`, `TAXES`, `ESV`, `MILITARY_TAX`, `DECLARATIONS`, `KVED_DIRECTORY`, `ACCOUNTING_BASICS`, `BUSINESS_FAQ`, `LEGAL_CHANGES`

## Example: List Articles

```http
GET /api/business-guide/articles?category=FOP_GROUPS&page=0&size=10
X-App-Language: uk
```

```json
{
  "content": [{
    "id": 1,
    "slug": "fop-group-2-overview",
    "title": "Група ФОП 2: IT-фрілансери та послуги",
    "category": "FOP_GROUPS",
    "summary": "...",
    "tags": ["ФОП 2 група", "IT"],
    "readingTimeMinutes": 2,
    "updatedAt": "2026-06-11T10:00:00"
  }],
  "page": 0,
  "size": 10,
  "totalElements": 4,
  "totalPages": 1
}
```

## Example: Smart Search

```http
GET /api/business-guide/search?q=Які податки платить ФОП 3 групи?
X-App-Language: uk
```

```json
{
  "query": "Які податки платить ФОП 3 групи?",
  "primaryArticle": { "slug": "fop-group-3-taxes-faq", "title": "..." },
  "results": [...],
  "relatedArticles": [...],
  "quickSummary": "ФОП 3 групи: єдиний податок 5%, ПДВ 20%, ЄСВ щомісяця...",
  "page": 0,
  "totalElements": 3
}
```

## Localization

`X-App-Language: uk` (default) or `en` selects `title_uk/en`, `content_uk/en`, `tags_uk/en`.

## View Count

`GET /articles/{slug}` increments `view_count` (used for popular articles).

## Related

- [Business Guide Module](../modules/business-guide.md)
- [Knowledge Search](../ai/knowledge-search.md)

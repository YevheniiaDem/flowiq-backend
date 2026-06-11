# Knowledge Search

## Smart Search

**Endpoint:** `GET /api/business-guide/search`  
**Service:** `KnowledgeService.search()`

### Repository Layer

JPQL queries match against:
- `title_uk`, `title_en`
- `content_uk`, `content_en`
- `tags_uk`, `tags_en`
- `summary_uk`, `summary_en`

### Scoring (`DatabaseKnowledgeProvider`)

| Signal | Weight |
|--------|--------|
| Title match | +12 |
| Tag match | +10 |
| Summary match | +8 |
| Content match | +3 |
| FOP group keyword (1/2/3) | +20 |
| Tax type (ЄСВ, військовий, єдиний) | +15–25 |
| KVED pattern `\d{2}\.\d{2}` | +30 |
| view_count | tie-breaker |

## AI-Assisted Summary

`KnowledgeProvider.assistSearch()` returns:
- `quickSummary` — from article summary or content excerpt
- `primaryArticle` — highest scored
- `relatedArticles` — up to 4 others

Example query: *«Які податки платить ФОП 3 групи?»* → `fop-group-3-taxes-faq`

## Highlighting

Frontend `highlight.utils.ts` wraps matches in `<mark>` for search dropdown.

## Related

- [Business Guide](../modules/business-guide.md)

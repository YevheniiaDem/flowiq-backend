# Test Scenarios

## AUTH

| ID | Scenario | Steps | Expected |
|----|----------|-------|----------|
| AUTH-01 | Valid login | POST login demo credentials | 200 + JWT |
| AUTH-02 | Invalid password | Wrong password | 401 |
| AUTH-03 | Protected without token | GET /transactions | 401 |
| AUTH-04 | Register new user | POST register unique email | 201 + JWT |

## Transactions

| ID | Scenario | Expected |
|----|----------|----------|
| TX-01 | Create revenue | 201, appears in list |
| TX-02 | Filter by date range | Only matching rows |
| TX-03 | Delete transaction | 204, removed from list |
| TX-04 | Import CSV | Job COMPLETED, rows in transactions |

## Forecasts

| ID | Scenario | Expected |
|----|----------|----------|
| FC-01 | Summary with seed data | projected months, insights array |
| FC-02 | FOP limit > 85% | Warning in summary |
| FC-03 | Empty transactions | Graceful zeros / empty projections |

## Tasks

| ID | Scenario | Expected |
|----|----------|----------|
| TK-01 | Create manual task | 201, appears in grouped |
| TK-02 | Complete task | status COMPLETED, completedAt set |
| TK-03 | Scheduler dedup | Same dedup key not duplicated |
| TK-04 | Overdue filter | Past due dates in overdue group |

## Knowledge

| ID | Scenario | Expected |
|----|----------|----------|
| KB-01 | Search FOP group 3 taxes | primaryArticle fop-group-3-taxes-faq |
| KB-02 | Article by slug | Full content UK with X-App-Language: uk |
| KB-03 | EN content | English title with X-App-Language: en |
| KB-04 | View count increment | Second GET increases count |

## Notifications

| ID | Scenario | Expected |
|----|----------|----------|
| NT-01 | Unread count | Matches unread items |
| NT-02 | Mark all read | updated count, unread 0 |
| NT-03 | action_url | Valid frontend path |

## Reports

| ID | Scenario | Expected |
|----|----------|----------|
| RP-01 | Generate PDF | Job COMPLETED, downloadable |
| RP-02 | Preview | Data without job creation |

# Regression Checklist

Run before each release.

## Authentication
- [ ] Login with demo account
- [ ] Register new user
- [ ] Logout clears session
- [ ] Invalid token rejected

## Core Financial
- [ ] Transaction CRUD
- [ ] Transaction filters and pagination
- [ ] CSV import (at least one bank format)
- [ ] Dashboard stats match transaction totals

## Forecasts & Analytics
- [ ] Forecast summary loads
- [ ] FOP limit forecast displays
- [ ] Analytics overview charts render
- [ ] Revenue trend matches dashboard

## Tasks & Notifications
- [ ] Task create/complete/delete
- [ ] Grouped tasks view
- [ ] Notification mark read / read all
- [ ] Notification bell count accurate

## Business Guide
- [ ] All 7 tabs render
- [ ] Search returns results + summary
- [ ] Article page loads by slug
- [ ] Legal updates section populated
- [ ] Dashboard knowledge widget links work

## Reports & AI
- [ ] PDF report generation
- [ ] AI Accountant recommendations load
- [ ] Chat sends message (if backend up)

## i18n & Preferences
- [ ] UK ↔ EN switch
- [ ] Currency formatting UAH/USD/EUR

## Cross-Browser
- [ ] Chrome
- [ ] Firefox
- [ ] Mobile viewport (sidebar)

## Performance
- [ ] Dashboard loads < 3s local
- [ ] No console errors on main routes

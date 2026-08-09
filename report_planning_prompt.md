# Insights & Reports Implementation Plan

**Goal:** Build a comprehensive Reports & Insights section in the app. The Insights screen will act as an "App Drawer" for various reports, allowing users to drill down into their data (Journal, Goals, Money, Screen Time). 

The centerpiece will be a "Strava-style" activity heatmap calendar showing daily consistency at a glance.

---

## 1. Strava-Style Consistency Heatmap
Before diving into individual charts, the main Insights dashboard should feature a **Consistency Calendar**.
- **Visuals:** A monthly or yearly grid (like GitHub contributions or Strava).
- **Icons:** Instead of plain dots, use small icons (e.g., a mini 📓 for journaling, 🎯 for goals, 💸 for tracking money) on days where activity was logged.
- **Interactivity:** Tapping a day brings up a quick summary card of that day's data.

## 2. The 12 Reports (App Drawer Style)
The Insights screen will feature a grid of icons (like an app drawer) navigating to detailed, filterable reports (Timeframe: 7 Days, 30 Days, Year, Custom Range).

### Journal & Mood Reports
1. **Mood Distribution & Triggers:** 
   - *Data:* `JournalEntry.moods`, `moodCauses`.
   - *Visual:* Pie chart of moods + list of top triggers causing positive/negative moods.
2. **The Ratings Trend (Line Chart):**
   - *Data:* `DailyRating` (overall, happiness, energy, focus, sleep).
   - *Visual:* Multi-line graph overlaying Energy vs. Sleep vs. Focus to find correlations.
3. **The Social Impact Report:**
   - *Data:* `JournalEntry.people` (`PersonRef`).
   - *Visual:* Bar chart of who you spend time with most and how they affect your mood (`feeling` field).
4. **Word Cloud / Gratitude Wall:**
   - *Data:* `JournalEntry.gratitude`, `bestPart`.
   - *Visual:* A visual word cloud highlighting frequently recurring words or tags in positive entries.

### Goal & Productivity Reports
5. **Goal Consistency & Effort:**
   - *Data:* `GoalUpdate` (`minutes`, `effort`, `focus`).
   - *Visual:* Heatmap or stacked bar chart showing total minutes invested in goals daily.
6. **Task Completion Rate:**
   - *Data:* `GoalTask.done` vs total tasks, `Milestone.done`.
   - *Visual:* Donut chart showing percentage of tasks completed per week.
7. **Screen Time vs. Productivity:**
   - *Data:* `ScreenTimeDay` (`totalMinutes`, `AppUsage.kind` - productive/distracting).
   - *Visual:* Stacked bar chart showing productive vs distracting app usage, overlayed with the `DailyRating.productivity` score.

### Financial Reports
8. **Expense Breakdown by Category:**
   - *Data:* `Expense.amount`, `Expense.category`.
   - *Visual:* Pie chart and sorted list of top spending categories.
9. **Impulse vs. Necessary Spending:**
   - *Data:* `Expense.necessity` (necessary vs unnecessary).
   - *Visual:* Bar chart comparing planned/necessary spending against impulsive spending over time.
10. **Cash Flow (Savings vs. Expenses):**
    - *Data:* `Expense.amount` vs `Saving.amount` & `Saving.kind` (earned/avoided).
    - *Visual:* Dual bar chart comparing money out vs. money in/saved.

### Holistic Insights
11. **Sleep & Stress Correlation:**
    - *Data:* `DailyRating.sleep` vs `DailyRating.stress`.
    - *Visual:* Scatter plot or dual-axis line chart to prove if poor sleep equals higher stress.
12. **The "Best Day" Formula:**
    - *Data:* Cross-referencing `DailyRating.overall == 9|10` with tags, causes, and habits.
    - *Visual:* Text-based insight cards summarizing exactly what conditions (who you were with, money spent, apps used) create your best days.

---

## Technical Instructions for the AI
When implementing this in the new chat, follow this workflow:
1. **Add Dependencies:** Ensure we have a Compose charting library (e.g., `Vico`, `YCharts`, or custom Canvas implementations for the Strava-style calendar).
2. **Build the Calendar:** Start by creating the `ConsistencyCalendar` composable using a lazy grid.
3. **Build the Drawer:** Create the `InsightsScreen` with a grid of report icons.
4. **Implement ViewModels:** Create an `InsightsViewModel` that queries the `Room`/`Firestore` database using date ranges (`start_date`, `end_date`) and aggregates the data.
5. **Build Detail Screens:** Implement the individual report screens using rich, glowing UI aesthetics consistent with the rest of the app.

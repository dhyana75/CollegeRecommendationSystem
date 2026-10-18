# Requirements Checklist

## Project
- Java Swing desktop application using JDBC and MySQL.
- Connect to the supplied `collegedb` schema.
- Keep the supplied database tables and names; do not replace them with the old `college_recommendation` schema.
- Use the 15 sample rows included in `sql/final_collegedb_setup.sql`; no CSV import is required for current testing.

## Student Features
- Search colleges by percentile, category code, city, and course/branch.
- View college details and available courses without student login.
- Bookmark colleges for the current application session.

## Admin Features
- Manage colleges and their cities.
- Manage courses and college-course associations.
- Manage cutoff records and their JEE/MHT-CET subtype records.
- No admin login is included.

## Database mapping
- `CITIES`, `COLLEGES`
- `COURSES`, `COLLEGE_COURSES`
- `CATEGORY`, `CUTOFFS`, `jee_cutoff`, `mht_cet_cutoff`
- `RECOMMENDATION_SESSIONS`, `RECOMMENDED_COLLEGES`
- `csv_cutoff_import` is part of the supplied SQL setup but CSV importing is not needed for current testing.

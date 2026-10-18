# File Structure

```text
CollegeRecommendationSystem/
|-- README.md
|-- compile.bat
|-- run.bat
|-- lib/mysql-connector-j-26.7.0.jar
|-- sql/final_collegedb_setup.sql
`-- src/com/college/
    |-- Main.java
    |-- dao/CollegeDAO.java
    |-- db/DBConfig.java, DBConnection.java, DBTest.java
    |-- model/College.java, Course.java, Cutoff.java
    |-- service/CollegeDataService.java, CourseDataService.java,
    |             CutoffDataService.java, RecommendationService.java,
    |             BookmarkService.java, RecommendationHistoryService.java
    `-- gui/HomeFrame.java, SearchFrame.java, CollegeDirectoryFrame.java,
                 CollegeDetailsFrame.java, AdminFrame.java,
                 CourseManagementFrame.java, CutoffManagementFrame.java,
                 MyRecommendationsFrame.java
```

The GUI is implemented in Swing. JDBC operations use `DBConnection` and the database name configured in `DBConfig.java`.

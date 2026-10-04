# Attendance Management System

A beginner-friendly desktop attendance application built with Java 25, JavaFX 25, Maven, and SQLite.

## Features

- Course/class sidebar
- Add, edit, and delete classes
- Add, edit, and delete students
- 8-week attendance grid with 4 periods per week
- Click attendance cells to cycle: blank -> Present -> Absent -> blank
- Mark all students present for a selected period
- Automatic attendance percentage
- SQLite database persistence
- Seeded sample classes and students on first run

## Requirements

- JDK 25
- Maven 3.10+ (or Maven 3.8+)
- VS Code with Extension Pack for Java

## Run in VS Code

Open the project folder, then open the terminal and run:

```text
mvn clean javafx:run
```

The application creates `attendance.db` in the project directory the first time it starts.

## Reset the sample database

Close the app and delete:

```text
attendance.db
```

Then run the project again. The sample classes and students will be recreated.

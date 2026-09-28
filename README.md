# RoomBook

RoomBook is a Java 17 / Spring Boot 3 REST API for meeting-room scheduling. It uses Spring MVC, Spring Data JPA, MySQL, Bean Validation, and Spring Security HTTP Basic authentication.

## Run locally

1. Start MySQL 8. The default connection is `jdbc:mysql://localhost:3306/roombook`; the database is created automatically if the MySQL user has permission.
2. Set a bootstrap administrator in PowerShell. The password must be at least 12 characters:

   ```powershell
   $env:BOOTSTRAP_ADMIN_EMAIL = "admin@example.com"
   $env:BOOTSTRAP_ADMIN_PASSWORD = "replace-with-a-long-local-password"
   ```

3. Start the app from the project directory:

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

   MySQL defaults are user `root` and an empty password. Override with `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`; override port 8080 with `SERVER_PORT`.

The bootstrap administrator is created only when both bootstrap variables are set and that email does not already exist. Employee passwords are stored with BCrypt. Never use the example password outside local development.

## Authorization

All API routes require HTTP Basic authentication. Admins can create, update, and delete rooms and manage employees. Employees can view rooms and manage their own bookings. Admins can view and manage all bookings. Create employee accounts with `POST /api/employees` while authenticated as an admin.

## Main API

| Method | Path | Access | Result |
| --- | --- | --- | --- |
| GET | `/api/rooms?page=0&size=20&sort=name,asc` | Authenticated | Paginated rooms |
| GET | `/api/rooms/{id}` | Authenticated | One room |
| POST / PUT / DELETE | `/api/rooms` or `/api/rooms/{id}` | Admin | Room CRUD; POST returns 201, DELETE 204 |
| GET / POST | `/api/employees` | Admin | List or create employees; POST returns 201 |
| GET / PUT / DELETE | `/api/employees/{id}` | Admin | Employee CRUD; DELETE returns 204 |
| GET | `/api/bookings` | Authenticated | Own bookings; admins see all |
| GET | `/api/bookings/{id}` | Owner or admin | One booking |
| GET | `/api/bookings/availability?roomId=1&startTime=2030-01-01T09:00:00&endTime=2030-01-01T10:00:00` | Authenticated | `true` or `false` |
| POST | `/api/bookings` | Authenticated | Create booking; returns 201 or 409 on overlap |
| POST | `/api/bookings/{id}/cancel` | Owner or admin | Cancel confirmed booking |
| POST | `/api/bookings/{id}/check-in` | Owner or admin | Check in to confirmed booking |

Errors use JSON with `timestamp`, `status`, `error`, `message`, `path`, and `validationErrors`. Typical statuses are 400 (validation/input), 401 (missing or invalid credentials), 403 (role/ownership), 404 (missing resource), and 409 (booking/data conflict).

## Postman scenarios

Import `postman/RoomBook.postman_collection.json` and configure `baseUrl`, `username`, `password`, and `roomId`. Use an admin account for employee/room setup; use any employee to run the booking collection.

Run **Create booking successfully** followed immediately by **Reject overlapping booking**. The first request creates a confirmed booking one hour in the future and stores its ID; the second reuses the same room and time and expects HTTP 409. Run the **Cancel booking and verify slot is available** folder to verify cancellation returns HTTP 200 and makes that interval available again.

The no-show scheduler checks every minute and releases confirmed bookings that remain unchecked-in at least ten minutes after their start. To verify this independently, insert a test booking whose start time is already 11 minutes past, then run the collection's **Verify no-show auto-release** request after the next scheduler pass. Set `noShowBookingId` to the inserted row's ID. For example, with existing room and employee IDs:

```sql
INSERT INTO bookings (room_id, organizer_id, start_time, end_time, status, checked_in, created_at)
VALUES (1, 1, NOW() - INTERVAL 11 MINUTE, NOW() + INTERVAL 49 MINUTE, 'CONFIRMED', FALSE, NOW());
SELECT LAST_INSERT_ID();
```

The no-show request expects the booking status to be `NO_SHOW_RELEASED`. Do not cancel this test booking before the scheduler processes it.

## Automated tests

Run the service unit tests with:

```powershell
.\mvnw.cmd test
```

The booking service tests cover successful creation, overlap rejection, cancellation freeing the slot, the no-show cutoff, and check-in state.

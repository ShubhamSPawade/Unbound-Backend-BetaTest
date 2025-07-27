# Unbound Platform Backend Documentation

---

## Table of Contents
1. [Project Overview](#project-overview)
2. [Architecture](#architecture)
3. [Technical Flow](#technical-flow)
4. [Layered Structure: File & Function Explanations](#layered-structure-file--function-explanations)
    - [Controllers](#controllers)
    - [Services](#services)
    - [Repositories](#repositories)
    - [Entities](#entities)
    - [DTOs](#dtos)
5. [API Endpoints & Flows](#api-endpoints--flows)
6. [Error Handling & Validation](#error-handling--validation)
7. [Advanced Features](#advanced-features)
8. [Setup, Testing, and Deployment](#setup-testing-and-deployment)

---

## Project Overview
Unbound is a full-stack college fest and event management platform. The backend is built with Spring Boot, following a layered architecture, and provides RESTful APIs for authentication, event management, payments, reviews, dashboards, notifications, certificates, media management, and more. It supports two roles: Student and College (no admin role).

---

## Architecture

- **Layered Structure:**
    - **Controller:** Handles HTTP requests, input validation, and response formatting.
    - **Service:** Contains business logic, orchestrates operations, and enforces rules.
    - **Repository:** Data access layer, interfaces with the database using JPA.
    - **Entity:** Maps to database tables, represents core data models.
    - **DTO (Data Transfer Object):** Used for request/response payloads, with validation annotations.
- **Security:** JWT-based authentication, role-based authorization.
- **Error Handling:** Centralized via `@ControllerAdvice`.
- **Media:** Static file serving, thumbnail generation.

---

## Technical Flow

### Example: Student Registers for an Event
```mermaid
graph TD;
  A[Student sends POST /api/student/events/register] --> B[StudentEventController.registerForEvent()];
  B --> C[EventRegistrationRequest DTO validation];
  C --> D[StudentDashboardService.registerForEvent()];
  D --> E[EventRepository, EventRegistrationRepository];
  E --> F[Database];
  D --> G[EmailService.sendRegistrationConfirmation()];
  D --> H[PaymentService.createOrder() if paid event];
  B --> I[Returns registration status, payment info];
```

### Example: College Uploads Event Poster
```mermaid
graph TD;
  A[College sends POST /api/college/events/{id}/poster] --> B[EventController.uploadPoster()];
  B --> C[File validation];
  C --> D[Thumbnail generation];
  D --> E[EventRepository.save()];
  B --> G[Returns poster URL, thumbnail URL];
```

---

## Layered Structure: File & Function Explanations

### Controllers
- **EventController.java**: Event CRUD, poster upload, poster moderation (by college), event details.
- **StudentEventController.java**: Student event exploration, registration, team management, my events.
- **CollegeDashboardController.java**: College dashboard stats, event/registration analytics, revenue.
- **PaymentController.java**: Payment order creation, payment verification, payment history.
- **FestController.java**: Fest CRUD for colleges.
- **EventReviewController.java**: Student reviews/ratings, college feedback viewing.
- **ExploreController.java**: Public event/fest exploration with filters/sorting.
- **CollegeController.java**: College profile management.
- **EventStatsController.java**: Event statistics (registrations, deadlines, etc.).
- **TeamController.java**: Team creation, join, leave, management for team events.
- **AuthController.java**: Register/login for students and colleges.
- **HealthController.java**: Health check endpoint.
- **TestController.java**: (For development/testing only).

#### Example Function Explanations (EventController):
- `createEvent(EventRequest)`: Creates a new event for a college.
- `updateEvent(Long, EventRequest)`: Updates event details.
- `uploadPoster(Long, MultipartFile)`: Handles poster upload, triggers thumbnail generation.
- `moderatePoster(Long, boolean)`: College approves/rejects poster (no admin).

### Services
- **PaymentService.java**: Handles Razorpay integration, order creation, payment verification, payment status tracking.
- **EmailService.java**: Sends registration, payment, and reminder emails.
- **CertificateService.java**: Generates and delivers PDF certificates (OpenPDF).
- **EventReminderService.java**: Schedules and sends event reminders.
- **StudentDashboardService.java**: Student dashboard logic (my events, payments, reviews).
- **CollegeDashboardService.java**: College dashboard logic (stats, analytics, revenue).
- **AuthService.java**: Authentication, registration, JWT issuance.
- **JwtService.java**: JWT creation and validation.
- **PasswordService.java**: Password hashing and verification.

#### Example Function Explanations (PaymentService):
- `createOrder(...)`: Creates a Razorpay order for event payment.
- `verifyPayment(...)`: Verifies payment signature and updates status.

### Repositories
- **EventRepository.java**: JPA repository for Event entity.
- **EventRegistrationRepository.java**: For event registrations.
- **EventReviewRepository.java**: For event reviews/ratings.
- **PaymentRepository.java**: For payment records.
- **TeamRepository.java**: For team management.
- **UserRepository.java**: For user authentication.
- **FestRepository.java**: For fest management.
- **StudentRepository.java**: For student data.
- **CollegeRepository.java**: For college data.
- **TeamMembersRepository.java**: For team membership.

### Entities
- **User.java**: Base user entity (student/college), authentication info.
- **Student.java**: Student profile, extends User.
- **College.java**: College profile, extends User.
- **Fest.java**: Fest details, linked to college.
- **Event.java**: Event details (category, mode, poster, approval, etc.).
- **EventRegistration.java**: Registration records (student/team, certificate status).
- **EventReview.java**: Ratings and reviews for events.
- **Payment.java**: Payment records (status, Razorpay info).
- **Team.java**: Team info for team events.
- **TeamMembers.java**: Team membership records.

### DTOs
- **RegisterRequest.java**: Registration payload, with validation.
- **LoginRequest.java**: Login payload.
- **ForgotPasswordRequest.java**: Password reset request payload (email).
- **ResetPasswordRequest.java**: Password reset payload (token, newPassword).
- **EventRequest.java**: Event creation/update payload.
- **EventRegistrationRequest.java**: Event registration payload.
- **FestRequest.java**: Fest creation/update payload.
- **AuthResponse.java**: Auth response with JWT.

---

## Controller Function Reference (Detailed)

### EventController

#### `GET /api/events` — listEvents
- **Access:** College only (JWT required)
- **Description:** List all events created by the authenticated college.
- **Parameters:**
  - Authentication: User (college)
- **Returns:**
  - 200: List of `Event` objects
  - 403: If not a college
  - 404: If college not found
- **Business Logic:**
  - Finds the college for the authenticated user
  - Returns all events owned by that college

#### `POST /api/events` — createEvent
- **Access:** College only (JWT required)
- **Description:** Create a new event for the authenticated college.
- **Parameters:**
  - Authentication: User (college)
  - Body: `EventRequest` (validated)
- **Returns:**
  - 200: Created `Event` object
  - 400: Duplicate event name, invalid fest, or date out of range
  - 403: If not a college
  - 404: If college not found
- **Business Logic:**
  - Checks for duplicate event name
  - Validates fest linkage and event date
  - Saves new event

#### `PUT /api/events/{eid}` — updateEvent
- **Access:** College only (JWT required)
- **Description:** Update an existing event for the authenticated college.
- **Parameters:**
  - Path: `eid` (event ID)
  - Authentication: User (college)
  - Body: `EventRequest` (validated)
- **Returns:**
  - 200: Updated `Event` object
  - 400: Duplicate event name, invalid fest, or date out of range
  - 403: If not a college
  - 404: If event not found or not owned by college
- **Business Logic:**
  - Checks ownership, duplicate name, fest linkage, and date
  - Updates event fields

#### `DELETE /api/events/{eid}` — deleteEvent
- **Access:** College only (JWT required)
- **Description:** Delete an event owned by the authenticated college.
- **Parameters:**
  - Path: `eid` (event ID)
  - Authentication: User (college)
- **Returns:**
  - 200: Success
  - 403: If not a college
  - 404: If event not found or not owned by college
- **Business Logic:**
  - Checks ownership, deletes event

#### `POST /api/events/{eid}/poster` — uploadEventPoster
- **Access:** College only (JWT required)
- **Description:** Upload a poster image for an event. Generates a thumbnail.
- **Parameters:**
  - Path: `eid` (event ID)
  - Authentication: User (college)
  - Form-data: `file` (image)
- **Returns:**
  - 200: `{ posterUrl, posterThumbnailUrl }`
  - 400: Invalid file type/size
  - 403: If not a college
  - 404: If event not found or not owned by college
- **Business Logic:**
  - Validates file type/size
  - Saves file and thumbnail

#### `POST /api/events/{eid}/poster/approve` — approveEventPoster
- **Access:** College only (JWT required)
- **Description:** Approve the uploaded poster for an event.
- **Parameters:**
  - Path: `eid` (event ID)
  - Authentication: User (college)
- **Returns:**
  - 200: Success message
  - 403: If not a college
  - 404: If event not found or not owned by college
- **Business Logic:**
  - Sets `posterApproved` to true

#### `POST /api/events/{eid}/poster/reject` — rejectEventPoster
- **Access:** College only (JWT required)
- **Description:** Reject the uploaded poster for an event, with a reason.
- **Parameters:**
  - Path: `eid` (event ID)
  - Authentication: User (college)
  - Form-data: `reason` (string)
- **Returns:**
  - 200: Success message
  - 403: If not a college
  - 404: If event not found or not owned by college
- **Business Logic:**
  - Sets `posterApproved` to false, logs action with reason

#### `DELETE /api/events/{eid}/poster` — deleteEventPoster
- **Access:** College only (JWT required)
- **Description:** Delete the poster and thumbnail for an event.
- **Parameters:**
  - Path: `eid` (event ID)
  - Authentication: User (college)
- **Returns:**
  - 200: Success message
  - 403: If not a college
  - 404: If event not found or not owned by college
- **Business Logic:**
  - Deletes files, updates event

#### `GET /api/events/{eid}/poster/audit-logs` — getEventPosterAuditLogs
- **Access:** College only (JWT required)
- **Description:** Get audit logs for poster moderation actions.
- **Parameters:**
  - Path: `eid` (event ID)
  - Authentication: User (college)
- **Returns:**
  - 200: List of audit log entries
  - 403: If not a college
  - 404: If event not found or not owned by college
- **Business Logic:**
  - Returns poster upload, approval, and rejection history

---

### StudentEventController

#### `POST /api/student/events/register` — registerForEvent
- **Access:** Student only (JWT required)
- **Description:** Register the authenticated student for an event (solo or team). Handles duplicate checks, capacity, and sends confirmation email.
- **Parameters:**
  - Authentication: User (student)
  - Body: `EventRegistrationRequest`
- **Returns:**
  - 200: Registration object and message
  - 400: Duplicate registration, event full, invalid registration type
  - 403: If not a student
  - 404: If student or event not found
- **Business Logic:**
  - Checks for duplicate registration, event capacity
  - Handles solo/team registration logic
  - Sends confirmation email

#### `GET /api/student/events/my` — myRegistrations
- **Access:** Student only (JWT required)
- **Description:** List all events the authenticated student is registered for.
- **Parameters:**
  - Authentication: User (student)
- **Returns:**
  - 200: List of registrations
  - 403: If not a student
  - 404: If student not found

#### `GET /api/student/events/dashboard/stats` — getStudentDashboardStats
- **Access:** Student only (JWT required)
- **Description:** Returns dashboard statistics for the authenticated student (e.g., events registered, payments, reviews).
- **Parameters:**
  - Authentication: User (student)
- **Returns:**
  - 200: Dashboard stats object
  - 403: If not a student
  - 404: If student not found

#### `GET /api/student/events/{eventId}/certificate` — downloadCertificate
- **Access:** Student only (JWT required)
- **Description:** Download PDF certificate for an event if eligible (registered, paid, event completed, certificate approved).
- **Parameters:**
  - Path: `eventId` (event ID)
  - Authentication: User (student)
- **Returns:**
  - 200: PDF file as attachment
  - 403: Not eligible (not registered, not paid, event not completed, certificate not approved)
  - 404: If student or event not found
- **Business Logic:**
  - Checks registration, payment, event completion, certificate approval
  - Generates and returns PDF certificate

---

### CollegeDashboardController

#### `GET /api/college/dashboard/earnings` — getTotalEarnings
- **Access:** College only (JWT required)
- **Description:** Get total earnings and per-event breakdown for the authenticated college.
- **Parameters:**
  - Authentication: User (college)
- **Returns:**
  - 200: `{ totalEarnings, breakdown }`
  - 403: If not a college
  - 404: If college not found
- **Business Logic:**
  - Aggregates paid payments for all events owned by the college

#### `GET /api/college/dashboard/registrations` — getRegistrationStats
- **Access:** College only (JWT required)
- **Description:** Get registration stats (total, paid, unpaid, event-wise) for the college.
- **Parameters:**
  - Authentication: User (college)
- **Returns:**
  - 200: Registration stats object
  - 403: If not a college
  - 404: If college not found
- **Business Logic:**
  - Aggregates registrations and payment status for all events

#### `GET /api/college/dashboard/analytics/by-fest` — getStatsByFest
- **Access:** College only (JWT required)
- **Description:** Get registration and earnings stats grouped by fest.
- **Parameters:**
  - Authentication: User (college)
- **Returns:**
  - 200: Fest stats object
  - 403: If not a college
  - 404: If college not found
- **Business Logic:**
  - Aggregates stats for each fest and its events

#### `GET /api/college/dashboard/analytics/by-date` — getStatsByDate
- **Access:** College only (JWT required)
- **Description:** Get registration and earnings stats grouped by event date.
- **Parameters:**
  - Authentication: User (college)
- **Returns:**
  - 200: Date stats object
  - 403: If not a college
  - 404: If college not found
- **Business Logic:**
  - Aggregates stats for each event date

#### `GET /api/college/dashboard/analytics/top-events` — getTopEvents
- **Access:** College only (JWT required)
- **Description:** Get top 5 events by registrations and earnings.
- **Parameters:**
  - Authentication: User (college)
- **Returns:**
  - 200: `{ topByRegistrations, topByEarnings }`
  - 403: If not a college
  - 404: If college not found
- **Business Logic:**
  - Sorts events by registrations and earnings

#### `GET /api/college/dashboard/events/{eventId}/registrations` — getEventRegistrations
- **Access:** College only (JWT required)
- **Description:** List all registrations for a specific event owned by the college.
- **Parameters:**
  - Path: `eventId` (event ID)
  - Authentication: User (college)
- **Returns:**
  - 200: List of registration info objects
  - 403: If not a college
  - 404: If event not found or not owned by college

#### `POST /api/college/dashboard/events/{eventId}/registrations/{registrationId}/approve-certificate` — approveCertificate
- **Access:** College only (JWT required)
- **Description:** Approve a certificate for a specific registration.
- **Parameters:**
  - Path: `eventId`, `registrationId`
  - Authentication: User (college)
- **Returns:**
  - 200: Success message
  - 403: If not a college
  - 404: If event or registration not found or not owned by college
- **Business Logic:**
  - Sets `certificateApproved` to true for the registration

#### `POST /api/college/dashboard/events/{eventId}/registrations/approve-all-certificates` — approveAllCertificates
- **Access:** College only (JWT required)
- **Description:** Approve certificates for all registrations in an event.
- **Parameters:**
  - Path: `eventId`
  - Authentication: User (college)
- **Returns:**
  - 200: Success message
  - 403: If not a college
  - 404: If event not found or not owned by college
- **Business Logic:**
  - Sets `certificateApproved` to true for all registrations in the event

#### `POST /api/college/dashboard/events/{eventId}/registrations/approve-certificates` — approveCertificatesForList
- **Access:** College only (JWT required)
- **Description:** Approve certificates for a list of registration IDs in an event.
- **Parameters:**
  - Path: `eventId`
  - Body: `{ registrationIds: [int, ...] }`
  - Authentication: User (college)
- **Returns:**
  - 200: Success message with count
  - 403: If not a college
  - 404: If event not found or not owned by college
- **Business Logic:**
  - Sets `certificateApproved` to true for each listed registration

#### `GET /api/college/dashboard/events` — getAllCollegeEvents
- **Access:** College only (JWT required)
- **Description:** List all events for the college, with registration and review stats.
- **Parameters:**
  - Authentication: User (college)
- **Returns:**
  - 200: List of event info objects
  - 403: If not a college
  - 404: If college not found
- **Business Logic:**
  - Uses `CollegeDashboardService.getAllCollegeEvents`

#### `GET /api/college/dashboard/stats` — getCollegeDashboardStats
- **Access:** College only (JWT required)
- **Description:** Get overall dashboard stats for the college (events, registrations, reviews, revenue).
- **Parameters:**
  - Authentication: User (college)
- **Returns:**
  - 200: Stats object
  - 403: If not a college
  - 404: If college not found
- **Business Logic:**
  - Uses `CollegeDashboardService.getCollegeDashboardStats`

#### `GET /api/college/dashboard/earnings` — getTotalEarnings
- **Access:** College only (JWT required)
- **Description:** Get total earnings and per-event breakdown for the authenticated college.
- **Parameters:**
  - Authentication: User (college)
- **Returns:**
  - 200: `{ totalEarnings, breakdown }`
  - 403: If not a college
  - 404: If college not found
- **Business Logic:**
  - Aggregates paid payments for all events owned by the college

#### `GET /api/college/dashboard/registrations` — getRegistrationStats
- **Access:** College only (JWT required)
- **Description:** Get registration stats (total, paid, unpaid, event-wise) for the college.
- **Parameters:**
  - Authentication: User (college)
- **Returns:**
  - 200: Registration stats object
  - 403: If not a college
  - 404: If college not found
- **Business Logic:**
  - Aggregates registrations and payment status for all events

#### `GET /api/college/dashboard/analytics/by-fest` — getStatsByFest
- **Access:** College only (JWT required)
- **Description:** Get registration and earnings stats grouped by fest.
- **Parameters:**
  - Authentication: User (college)
- **Returns:**
  - 200: Fest stats object
  - 403: If not a college
  - 404: If college not found
- **Business Logic:**
  - Aggregates stats for each fest and its events

#### `GET /api/college/dashboard/analytics/by-date` — getStatsByDate
- **Access:** College only (JWT required)
- **Description:** Get registration and earnings stats grouped by event date.
- **Parameters:**
  - Authentication: User (college)
- **Returns:**
  - 200: Date stats object
  - 403: If not a college
  - 404: If college not found
- **Business Logic:**
  - Aggregates stats for each event date

#### `GET /api/college/dashboard/analytics/top-events` — getTopEvents
- **Access:** College only (JWT required)
- **Description:** Get top 5 events by registrations and earnings.
- **Parameters:**
  - Authentication: User (college)
- **Returns:**
  - 200: `{ topByRegistrations, topByEarnings }`
  - 403: If not a college
  - 404: If college not found
- **Business Logic:**
  - Sorts events by registrations and earnings

#### `POST /api/college/dashboard/events/{eventId}/registrations/{registrationId}/approve-certificate` — approveCertificate
- **Access:** College only (JWT required)
- **Description:** Approve a certificate for a specific registration.
- **Parameters:**
  - Path: `eventId`, `registrationId`
  - Authentication: User (college)
- **Returns:**
  - 200: Success message
  - 403: If not a college
  - 404: If event or registration not found or not owned by college
- **Business Logic:**
  - Sets `certificateApproved` to true for the registration

#### `POST /api/college/dashboard/events/{eventId}/registrations/approve-all-certificates` — approveAllCertificates
- **Access:** College only (JWT required)
- **Description:** Approve certificates for all registrations in an event.
- **Parameters:**
  - Path: `eventId`
  - Authentication: User (college)
- **Returns:**
  - 200: Success message
  - 403: If not a college
  - 404: If event not found or not owned by college
- **Business Logic:**
  - Sets `certificateApproved` to true for all registrations in the event

#### `POST /api/college/dashboard/events/{eventId}/registrations/approve-certificates` — approveCertificatesForList
- **Access:** College only (JWT required)
- **Description:** Approve certificates for a list of registration IDs in an event.
- **Parameters:**
  - Path: `eventId`
  - Body: `{ registrationIds: [int, ...] }`
  - Authentication: User (college)
- **Returns:**
  - 200: Success message with count
  - 403: If not a college
  - 404: If event not found or not owned by college
- **Business Logic:**
  - Sets `certificateApproved` to true for each listed registration

---

### PaymentController

#### `POST /api/payments/create-order` — createOrder
- **Access:** Authenticated (student or college)
- **Description:** Create a Razorpay order for an event registration.
- **Parameters:**
  - Body: `{ registrationId: int, amount: int, currency?: string, receiptEmail: string }`
  - Authentication: User
- **Returns:**
  - 200: `{ order: RazorpayOrderJson }`
  - 400: Invalid registration ID
  - 500: Payment gateway error
- **Business Logic:**
  - Validates registration
  - Calls `PaymentService.createOrder`
  - Saves payment record

#### `POST /api/payments/verify` — verifyPayment
- **Access:** Authenticated (student or college)
- **Description:** Update payment status after Razorpay payment.
- **Parameters:**
  - Body: `{ razorpayOrderId: string, status: string, paymentId: string }`
- **Returns:**
  - 200: Success message
- **Business Logic:**
  - Updates payment and registration status
  - Sends email receipt if successful

---

### EventReviewController

#### `POST /api/events/{eventId}/review` — submitReview
- **Access:** Student only (JWT required)
- **Description:** Submit a review for an event after completion.
- **Parameters:**
  - Path: `eventId` (event ID)
  - Body: `{ rating: int (1-5), reviewText?: string }`
  - Authentication: User (student)
- **Returns:**
  - 200: Success message
  - 400: Not eligible, already reviewed, invalid rating
  - 403: If not a student
  - 404: If student or event not found
- **Business Logic:**
  - Checks event completion, registration, payment, and duplicate review
  - Saves review

#### `GET /api/events/{eventId}/review` — getMyReview
- **Access:** Student only (JWT required)
- **Description:** Get the authenticated student's review for an event.
- **Parameters:**
  - Path: `eventId` (event ID)
  - Authentication: User (student)
- **Returns:**
  - 200: Review object
  - 403: If not a student
  - 404: If student, event, or review not found

#### `GET /api/events/{eventId}/reviews` — getEventReviews
- **Access:** College only (JWT required)
- **Description:** Get all reviews for an event owned by the college.
- **Parameters:**
  - Path: `eventId` (event ID)
  - Authentication: User (college)
- **Returns:**
  - 200: List of reviews
  - 403: If not a college
  - 404: If college, event not found, or not owned by college

#### `GET /api/events/{eventId}/rating` — getEventRating
- **Access:** Public
- **Description:** Get average rating and review count for an event.
- **Parameters:**
  - Path: `eventId` (event ID)
- **Returns:**
  - 200: `{ eventId, averageRating, reviewCount }`
  - 404: If event not found

---

### ExploreController

#### `GET /api/explore/fests` — exploreFests
- **Access:** Public
- **Description:** List and filter fests by name, college, start/end date.
- **Parameters:**
  - Query: `name`, `college`, `startDate`, `endDate` (all optional)
- **Returns:**
  - 200: List of fests

#### `GET /api/explore/events` — exploreEvents
- **Access:** Public
- **Description:** List and filter events by category, date, fee, team, fest, college, location, mode, and sort order.
- **Parameters:**
  - Query: `category`, `date`, `entryFee`, `team`, `festName`, `college`, `location`, `mode`, `sort` (all optional)
- **Returns:**
  - 200: List of events
- **Business Logic:**
  - Filters events by provided criteria
  - For popularity sorting, uses EventRegistrationRepository to count registrations for each event

---

### FestController

#### `GET /api/fests` — listFests
- **Access:** College only (JWT required)
- **Description:** List all fests created by the authenticated college.
- **Parameters:**
  - Authentication: User (college)
- **Returns:**
  - 200: List of fests
  - 403: If not a college
  - 404: If college not found

#### `POST /api/fests` — createFest
- **Access:** College only (JWT required)
- **Description:** Create a new fest for the authenticated college.
- **Parameters:**
  - Authentication: User (college)
  - Body: `FestRequest` (validated)
- **Returns:**
  - 200: Created fest object
  - 400: Duplicate name or invalid date
  - 403: If not a college
  - 404: If college not found

#### `PUT /api/fests/{fid}` — updateFest
- **Access:** College only (JWT required)
- **Description:** Update a fest owned by the authenticated college.
- **Parameters:**
  - Path: `fid` (fest ID)
  - Authentication: User (college)
  - Body: `FestRequest` (validated)
- **Returns:**
  - 200: Updated fest object
  - 400: Duplicate name or invalid date
  - 403: If not a college
  - 404: If fest not found or not owned by college

#### `DELETE /api/fests/{fid}` — deleteFest
- **Access:** College only (JWT required)
- **Description:** Delete a fest owned by the authenticated college.
- **Parameters:**
  - Path: `fid` (fest ID)
  - Authentication: User (college)
- **Returns:**
  - 200: Success
  - 403: If not a college
  - 404: If fest not found or not owned by college

---

### AuthController

#### `POST /api/auth/register` — register
- **Access:** Public
- **Description:** Register a new student or college account.
- **Parameters:**
  - Body: `RegisterRequest`
- **Returns:**
  - 200: `AuthResponse` (JWT, role, email, name)
  - 400: Email already registered, missing/invalid fields
- **Business Logic:**
  - Validates uniqueness, hashes password, creates user and profile, issues JWT

#### `POST /api/auth/login` — login
- **Access:** Public
- **Description:** Login as student or college.
- **Parameters:**
  - Body: `LoginRequest`
- **Returns:**
  - 200: `AuthResponse` (JWT, role, email, name)
  - 400: Invalid credentials
- **Business Logic:**
  - Validates credentials, issues JWT

#### `POST /api/auth/forgot-password` — forgotPassword
- **Access:** Public
- **Description:** Send password reset link to email address.
- **Parameters:**
  - Body: `ForgotPasswordRequest` (email)
- **Returns:**
  - 200: Success message
  - 400: Email not found
- **Business Logic:**
  - Validates email exists, generates reset token, sends email

#### `POST /api/auth/reset-password` — resetPassword
- **Access:** Public
- **Description:** Reset password using token from email.
- **Parameters:**
  - Body: `ResetPasswordRequest` (token, newPassword)
- **Returns:**
  - 200: Success message
  - 400: Invalid token or expired token
- **Business Logic:**
  - Validates token, updates password, deletes token

---

### CollegeController

#### `GET /api/college/profile` — getProfile
- **Access:** College only (JWT required)
- **Description:** Get the profile of the authenticated college.
- **Parameters:**
  - Authentication: User (college)
- **Returns:**
  - 200: College object
  - 403: If not a college
  - 404: If college not found

#### `PUT /api/college/profile` — updateProfile
- **Access:** College only (JWT required)
- **Description:** Update the profile of the authenticated college.
- **Parameters:**
  - Authentication: User (college)
  - Body: College object (fields to update)
- **Returns:**
  - 200: Updated college object
  - 403: If not a college
  - 404: If college not found

---

### TeamController

#### `GET /api/student/teams/event/{eventId}` — viewTeamsForEvent
- **Access:** Public
- **Description:** List all teams for a given event.
- **Parameters:**
  - Path: `eventId` (event ID)
- **Returns:**
  - 200: List of teams
  - 404: If event not found

#### `GET /api/student/teams/my` — myTeams
- **Access:** Student only (JWT required)
- **Description:** List all teams the authenticated student is a member of.
- **Parameters:**
  - Authentication: User (student)
- **Returns:**
  - 200: List of teams
  - 403: If not a student
  - 404: If student not found

#### `GET /api/student/teams/{teamId}/members` — viewTeamMembers
- **Access:** Public
- **Description:** List all members of a team.
- **Parameters:**
  - Path: `teamId` (team ID)
- **Returns:**
  - 200: List of students
  - 404: If team not found

#### `DELETE /api/student/teams/{teamId}/leave` — leaveTeam
- **Access:** Student only (JWT required)
- **Description:** Leave a team the student is a member of.
- **Parameters:**
  - Path: `teamId` (team ID)
  - Authentication: User (student)
- **Returns:**
  - 200: Success message
  - 400: Not a member
  - 403: If not a student
  - 404: If student or team not found

---

### EventStatsController

#### `GET /api/events/{eventId}/stats` — getEventStats
- **Access:** Public
- **Description:** Get statistics for an event (registrations, days left, deadline, date).
- **Parameters:**
  - Path: `eventId` (event ID)
- **Returns:**
  - 200: Stats object
  - 400: Invalid event date
  - 404: If event not found

---

### HealthController

#### `GET /api/health` — healthCheck
- **Access:** Public
- **Description:** Health check endpoint for backend status.
- **Returns:**
  - 200: Status string

---

### TestController

#### `GET /api/protected` — protectedEndpoint
- **Access:** Authenticated (any user)
- **Description:** Test endpoint to verify authentication and role.
- **Returns:**
  - 200: Authenticated user info
  - 401: If not authenticated

---

## API Endpoints & Flows

- **Authentication:**
    - `POST /api/auth/register` (student/college)
    - `POST /api/auth/login`
    - `POST /api/auth/forgot-password` (send reset link)
    - `POST /api/auth/reset-password` (reset password)
- **Event Management:**
    - `GET /api/events` (list college events)
    - `POST /api/events` (create event)
    - `PUT /api/events/{id}` (update event)
    - `DELETE /api/events/{id}` (delete event)
    - `POST /api/events/{id}/poster` (upload poster)
    - `POST /api/events/{id}/poster/approve` (approve poster)
    - `POST /api/events/{id}/poster/reject` (reject poster)
    - `DELETE /api/events/{id}/poster` (delete poster)
    - `GET /api/events/{id}/poster/audit-logs` (audit logs)
- **Fest Management:**
    - `GET /api/fests` (list college fests)
    - `POST /api/fests` (create fest)
    - `PUT /api/fests/{id}` (update fest)
    - `DELETE /api/fests/{id}` (delete fest)
- **Student Event Registration:**
    - `POST /api/student/events/register` (register for event)
    - `GET /api/student/events/my` (my registrations)
    - `GET /api/student/events/dashboard/stats` (dashboard stats)
    - `GET /api/student/events/{eventId}/certificate` (download certificate)
- **Payments:**
    - `POST /api/payments/create-order` (create order)
    - `POST /api/payments/verify` (verify payment)
- **Reviews & Feedback:**
    - `POST /api/events/{eventId}/review` (submit review)
    - `GET /api/events/{eventId}/review` (get my review)
    - `GET /api/events/{eventId}/reviews` (get all reviews)
    - `GET /api/events/{eventId}/rating` (get event rating)
- **College Dashboard:**
    - `GET /api/college/dashboard/stats` (overall stats)
    - `GET /api/college/dashboard/earnings` (earnings breakdown)
    - `GET /api/college/dashboard/registrations` (registration stats)
    - `GET /api/college/dashboard/analytics/by-fest` (stats by fest)
    - `GET /api/college/dashboard/analytics/by-date` (stats by date)
    - `GET /api/college/dashboard/analytics/top-events` (top events)
    - `GET /api/college/dashboard/events` (events with stats)
    - `GET /api/college/dashboard/events/{eventId}/registrations` (event registrations)
    - `POST /api/college/dashboard/events/{eventId}/registrations/{registrationId}/approve-certificate` (approve certificate)
    - `POST /api/college/dashboard/events/{eventId}/registrations/approve-all-certificates` (approve all certificates)
    - `POST /api/college/dashboard/events/{eventId}/registrations/approve-certificates` (approve certificates for list)
- **College Profile:**
    - `GET /api/college/profile` (get profile)
    - `PUT /api/college/profile` (update profile)
- **Team Management:**
    - `GET /api/student/teams/event/{eventId}` (teams for event)
    - `GET /api/student/teams/my` (my teams)
    - `GET /api/student/teams/{teamId}/members` (team members)
    - `DELETE /api/student/teams/{teamId}/leave` (leave team)
- **Explore:**
    - `GET /api/explore/fests` (explore fests)
    - `GET /api/explore/events` (explore events)
- **Event Statistics:**
    - `GET /api/events/{eventId}/stats` (event stats)
- **Other:**
    - `GET /api/health` (health check)
    - `GET /api/protected` (test authentication)

---

## Error Handling & Validation
- **Global Exception Handling:**
    - All exceptions are handled by `GlobalExceptionHandler` (`@ControllerAdvice`).
    - Consistent error response format: `{ "timestamp": ..., "status": ..., "error": ..., "message": ..., "path": ... }`
    - Validation errors return detailed messages (e.g., which field failed and why).
- **Validation:**
    - DTOs use validation annotations (e.g., `@NotNull`, `@Email`, `@Size`).
    - Controllers validate input before passing to services.
- **Error Message Improvements:**
    - All thrown exceptions include context (IDs, operation details).
    - No silent exception handling; all errors are surfaced and logged.

---

## Advanced Features
- **Authentication & Security:**
    - JWT-based authentication with role-based access control.
    - Password reset functionality with email-based token verification.
    - Secure password hashing and validation.
- **Media Management:**
    - Poster/banner upload with file validation (type, size).
    - Thumbnail generation for images.
    - Static file serving via `/api/media/{filename}`.
    - Poster audit logging for moderation tracking.
- **Payments:**
    - Razorpay integration for order creation and payment verification.
    - Payment status tracking and history.
    - Email receipts for successful payments.
- **Notifications:**
    - Email notifications for registration, payment, reminders.
    - Password reset emails with secure tokens.
    - Scheduled event reminders via `EventReminderService`.
- **Certificates:**
    - PDF certificate generation (OpenPDF) for event participants.
    - Certificate approval workflow for colleges.
    - Downloadable via `/api/student/events/{eventId}/certificate`.
- **Dashboards:**
    - Student and college dashboards with comprehensive stats and analytics.
    - Revenue tracking and earnings breakdown.
    - Registration analytics by fest, date, and top events.
- **Team Management:**
    - Team creation and management for team events.
    - Team member management with join/leave functionality.

---

## Setup, Testing, and Deployment

### Prerequisites
- Java 17+
- Maven
- MySQL

### Setup
1. Clone the repository.
2. Configure `src/main/resources/application.properties` with your:
   - MySQL database credentials
   - Email configuration (SMTP settings for password reset emails)
   - Razorpay API keys for payments
   - Frontend URL for password reset links
3. Build the project:
   ```sh
   mvn clean install
   ```
4. Run the backend:
   ```sh
   mvn spring-boot:run
   ```
5. Access API at `http://localhost:8081/api/`
6. Access Swagger UI at `http://localhost:8081/swagger-ui/index.html`

### Testing
- Use the provided Postman collection (`Unbound.postman_collection.json`) for API testing.
- Integration and unit tests can be run with:
   ```sh
   mvn test
   ```

### Deployment
- Ensure `application.properties` is production-ready (DB, mail, static file paths, etc.).
- Use a process manager (e.g., systemd, pm2) or deploy to a cloud provider (AWS, GCP, Azure, etc.).
- Serve static files from a persistent directory.
- Secure environment variables and secrets.

---

## Contact & Contribution
- For issues, open a GitHub issue or contact the maintainer.
- Contributions are welcome! Please follow the code style and submit pull requests for review.

---

This documentation is auto-generated and should be kept up to date with code changes. For detailed API usage, see the Postman collection and in-code JavaDocs.

# Unbound Platform: Database Schema

---

## Entity Relationship Overview

- User (Student or College)
- Student (linked to User, belongs to College)
- College (linked to User)
- Fest (belongs to College)
- Event (belongs to College, optionally to Fest)
- EventRegistration (links Student, Event, optionally Team)
- Team (belongs to Event, has creator Student)
- TeamMembers (links Team and Student)
- Payment (for EventRegistration)
- EventReview (links Student and Event)

---

## Entities

### User
| Field      | Type      | Description                |
|------------|-----------|----------------------------|
| uid        | Integer   | Primary key                |
| email      | String    | Unique, login email        |
| password   | String    | Hashed password            |
| role       | Enum      | Student or College         |
| createdAt  | Timestamp | Account creation time      |

### Student
| Field   | Type    | Description                |
|---------|---------|----------------------------|
| sid     | Integer | Primary key                |
| user    | User    | One-to-one, FK to User     |
| college | College | Many-to-one, FK to College |
| sname   | String  | Student name               |

### College
| Field        | Type    | Description                |
|--------------|---------|----------------------------|
| cid          | Integer | Primary key                |
| user         | User    | One-to-one, FK to User     |
| cname        | String  | College name               |
| cdescription | String  | Description                |
| address      | String  | Address                    |
| contactEmail | String  | Contact email              |

### Fest
| Field        | Type    | Description                |
|--------------|---------|----------------------------|
| fid          | Integer | Primary key                |
| college      | College | Many-to-one, FK to College |
| fname        | String  | Fest name                  |
| fdescription | String  | Description                |
| startDate    | String  | Start date (YYYY-MM-DD)    |
| endDate      | String  | End date (YYYY-MM-DD)      |

### Event
| Field              | Type    | Description                        |
|--------------------|---------|------------------------------------|
| eid                | Integer | Primary key                        |
| college            | College | Many-to-one, FK to College         |
| fest               | Fest    | Many-to-one, FK to Fest (nullable) |
| ename              | String  | Event name                         |
| edescription       | String  | Description                        |
| eventDate          | String  | Date (YYYY-MM-DD)                  |
| fees               | Integer | Entry fee (0 = free)               |
| location           | String  | Location                           |
| capacity           | Integer | Max registrations                  |
| teamIsAllowed      | Boolean | Team registration allowed          |
| category           | String  | Category (e.g., Technical)         |
| mode               | String  | Online/Offline                     |
| posterUrl          | String  | Poster image URL                   |
| posterThumbnailUrl | String  | Poster thumbnail URL               |
| posterApproved     | Boolean | Poster approved by college         |
| (registrations)   | -       | Registration count fetched via EventRegistrationRepository |

### EventRegistration
| Field              | Type    | Description                        |
|--------------------|---------|------------------------------------|
| rid                | Integer | Primary key                        |
| event              | Event   | Many-to-one, FK to Event           |
| student            | Student | Many-to-one, FK to Student         |
| team               | Team    | Many-to-one, FK to Team (nullable) |
| erdateTime         | String  | Registration datetime              |
| status             | String  | Registration status                |
| paymentStatus      | String  | Payment status                     |
| certificateApproved| Boolean | Certificate approved               |

### Team
| Field   | Type    | Description                |
|---------|---------|----------------------------|
| tid     | Integer | Primary key                |
| event   | Event   | Many-to-one, FK to Event   |
| tname   | String  | Team name                  |
| creator | Student | Many-to-one, FK to Student |

### TeamMembers
| Field   | Type    | Description                |
|---------|---------|----------------------------|
| team    | Team    | Many-to-one, FK to Team    |
| student | Student | Many-to-one, FK to Student |

### Payment
| Field           | Type              | Description                        |
|-----------------|-------------------|------------------------------------|
| pid             | Integer           | Primary key                        |
| eventRegistration| EventRegistration | Many-to-one, FK to EventRegistration|
| razorpayOrderId | String            | Razorpay order ID                  |
| status          | String            | Payment status                     |
| amount          | Integer           | Amount paid                        |
| currency        | String            | Currency (e.g., INR)               |
| createdAt       | String            | Payment creation datetime          |
| paymentId       | String            | Razorpay payment ID                |
| receiptEmail    | String            | Email for receipt                  |

### EventReview
| Field      | Type    | Description                |
|------------|---------|----------------------------|
| id         | Integer | Primary key                |
| event      | Event   | Many-to-one, FK to Event   |
| student    | Student | Many-to-one, FK to Student |
| rating     | Integer | 1-5 rating                 |
| reviewText | String  | Review text                |
| createdAt  | String  | Review creation datetime   |

---

## Relationships Diagram (Textual)

- User 1---1 Student
- User 1---1 College
- College 1---* Fest
- College 1---* Event
- Fest 1---* Event
- Event 1---* EventRegistration
- Student 1---* EventRegistration
- Event 1---* Team
- Team 1---* TeamMembers
- Student 1---* TeamMembers
- EventRegistration 1---1 Payment
- Event 1---* EventReview
- Student 1---* EventReview

---

For a visual ERD, use the above relationships in a diagramming tool. 
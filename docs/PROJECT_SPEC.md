# English Learning Platform

**Document:** Project Specification
**Version:** 0.1
**Status:** Draft
**Project Type:** Graduation Project

---

# 1. Project Overview

English Learning Platform is a responsive web-based platform designed to help users learn and practice English vocabulary through structured courses and interactive exercises.

The platform focuses primarily on vocabulary learning, pronunciation, listening, vocabulary comprehension, practice, assessment, review, and learning-progress tracking.

The system supports three primary roles:

- Student
- Teacher
- Admin

All roles access the system through a web browser.

Students receive a mobile-first learning experience, while Teacher and Admin interfaces are optimized primarily for desktop management while remaining responsive and usable on mobile devices.

---

# 2. Project Objectives

The platform aims to:

1. Provide structured English vocabulary learning based on CEFR levels.
2. Organize vocabulary into Courses and topic-based Lessons.
3. Provide vocabulary pronunciation, definitions, meanings, examples, and audio.
4. Allow Students to practice vocabulary through multiple exercise types.
5. Track Student learning results and vocabulary mastery.
6. Allow Students to search and save vocabulary independently from Courses.
7. Support vocabulary review based on learning activity.
8. Allow multiple Teachers to create and manage Courses and Lessons.
9. Allow Teachers to use vocabulary data from approved dictionary sources.
10. Provide Standard and Premium learning experiences.
11. Support subscription-based Premium access.
12. Provide Admin management and system/revenue statistics.

---

# 3. User Roles

## 3.1 Student

Students are the primary learners of the platform.

Students can:

- create an account
- log in and log out
- manage their profile
- browse CEFR levels
- browse Courses
- view Teacher information
- enroll in Courses
- access My Courses
- study Lessons
- learn vocabulary
- listen to vocabulary pronunciation
- complete Fill Word exercises
- complete Listening exercises
- complete Quizzes
- view scores
- view learning progress
- view learning history
- search vocabulary
- save vocabulary
- manage My Vocabulary
- review vocabulary
- identify weak vocabulary
- use Standard access
- upgrade to Premium
- view subscription information

---

# 4. Student Account Types

Student accounts support two access levels:

## 4.1 Standard

Standard is the free learning tier.

Standard must provide meaningful learning functionality.

Standard Students should be able to access core learning features such as:

- vocabulary learning
- pronunciation
- basic definitions
- Vietnamese meanings
- examples
- audio
- basic exercises
- vocabulary search
- saved vocabulary
- basic learning progress

The final content and feature limitations will be defined in Business Rules.

---

## 4.2 Premium

Premium provides deeper learning functionality and additional content.

Potential Premium benefits include:

- Premium Courses
- specialized vocabulary Courses
- advanced vocabulary information
- multiple vocabulary senses
- collocations
- synonyms
- antonyms
- word families
- advanced exercises
- advanced listening practice
- weak-vocabulary practice
- personalized review
- detailed learning analytics

Premium access belongs to the platform rather than to an individual Teacher.

---

# 5. Teacher

Teachers are responsible for creating and managing educational content.

The system supports multiple Teachers.

A Teacher can:

- manage Teacher profile
- create Courses
- update Courses
- manage owned Courses
- create Lessons
- update Lessons
- manage vocabulary used in Lessons
- search vocabulary
- use existing platform vocabulary
- retrieve vocabulary information from approved dictionary sources when necessary
- select appropriate vocabulary senses
- configure exercises
- publish learning content
- view analytics for owned Courses

A Teacher may own multiple Courses.

A Course has one primary Teacher.

Teachers do not receive direct Student payments in the current project scope.

---

# 6. Admin

Admins manage the platform and business operations.

The Admin Overview Dashboard provides operational platform statistics.
This scope excludes expense management, salaries, bookkeeping and profit/loss;
it does not expand approved account or Course management permissions.
Teachers are users filtered by TEACHER role; Admins may inspect permitted
Teacher information and their Courses without a separate Teacher identity.

Admin functionality includes:

- manage Students
- manage Teachers
- manage user accounts
- oversee Courses
- oversee learning content
- manage subscription plans
- monitor subscriptions
- monitor transactions
- view system statistics
- view Student statistics
- view Teacher statistics
- view Course statistics
- view Premium statistics
- view revenue statistics over time

Teacher payout and revenue sharing are outside the current project scope.

---

# 7. Learning Content Structure

The primary learning hierarchy is:

CEFR Level
→ Course
→ Lesson
→ Vocabulary

---

# 8. CEFR Levels

The platform organizes English learning according to CEFR levels:

- A1 — Beginner
- A2 — Elementary
- B1 — Intermediate
- B2 — Upper Intermediate
- C1 — Advanced
- C2 — Proficiency

CEFR Levels belong to the platform.

Teachers do not create custom CEFR levels.

Courses are associated with an appropriate CEFR level.

---

# 9. Course

A Course represents a structured collection of Lessons.

Each Course:

- belongs to one CEFR Level
- has one primary Teacher
- contains multiple Lessons
- may have Standard or Premium access
- may contain enrollment and progress information

Example:

A1
→ Everyday English A1
→ Teacher A

Another Teacher may create another Course at the same CEFR level.

Example:

A1
→ Basic Vocabulary A1
→ Teacher B

---

# 10. Course Enrollment

Students enroll in Courses before using them as part of their learning program.

Enrollment allows the system to track:

- My Courses
- Course progress
- Lesson progress
- Student enrollment counts
- Course learning activity
- Course completion

Free Courses may use a free enrollment action.

Premium Courses require appropriate Premium access.

---

# 11. Lesson

Courses are divided into Lessons.

Lessons should primarily be organized by topic.

Examples include:

- Greetings
- Family
- Food & Drinks
- Daily Activities
- School
- Home
- Clothes
- Weather
- Transportation
- Shopping

Lessons should not primarily be divided only by grammatical part of speech.

Part of speech is vocabulary metadata.

---

# 12. Lesson Learning Flow

The primary Lesson learning experience is:

Learn Vocabulary
→ Fill Word
→ Listening
→ Quiz
→ Result
→ Progress
→ Review

The system may allow Students to repeat learning activities.

The exact requirements for mandatory ordering and Lesson completion will be defined later in Business Rules.

---

# 13. Vocabulary

Vocabulary is a core shared resource of the platform.

Vocabulary should be reusable between different Lessons and Teachers when appropriate.

A vocabulary entry may contain:

- word
- IPA / pronunciation
- CEFR level
- pronunciation audio
- one or more vocabulary senses

---

# 14. Vocabulary Sense

A word may contain multiple senses.

Each sense may contain:

- part of speech
- English definition
- Vietnamese meaning
- example sentence

Example:

`book`

Sense 1:

- Part of speech: noun
- Meaning: a written or printed work

Sense 2:

- Part of speech: verb
- Meaning: to reserve something

When adding vocabulary to a Lesson, a Teacher may select the appropriate sense for the Lesson context.

---

# 15. Vocabulary Data Sources

Teachers may search for vocabulary.

The system should first determine whether appropriate vocabulary already exists in the platform vocabulary database.

Conceptual flow:

Teacher searches vocabulary
→ Search Platform Vocabulary
→ If found: reuse vocabulary
→ If not found: request data from approved Dictionary Provider
→ Teacher reviews/selects vocabulary data
→ Vocabulary becomes available for Lesson use

Potential dictionary data includes:

- word
- IPA
- part of speech
- English definition
- example
- pronunciation audio

The final Dictionary Provider has not yet been selected.

Licensing, caching, storage, and redistribution requirements must be reviewed before selecting a provider.

---

# 16. Vocabulary Audio

Vocabulary pronunciation may be obtained from an approved Dictionary Provider.

Where supported, pronunciation may include:

- UK pronunciation
- US pronunciation

Example sentence audio may later use a Text-to-Speech provider.

The exact audio architecture and provider remain undecided.

---

# 17. Exercise Types

The initial system contains three primary exercise groups.

## 17.1 Fill Word

Fill Word evaluates vocabulary recall and spelling.

A Fill Word question may show:

- IPA
- English definition
- partially hidden vocabulary

Example:

IPA:

`/ˈbjuː.tɪ.fəl/`

Definition:

`pleasing the senses or mind`

Question:

`b _ _ _ _ _ _ l`

Student enters:

`beautiful`

---

## 17.2 Listening

Listening evaluates recognition of spoken vocabulary.

Possible formats include:

### Listen and Choose

Student listens to pronunciation and selects the correct vocabulary from multiple options.

### Listen and Type

Student listens to pronunciation and types the vocabulary word.

---

## 17.3 Quiz

Quiz evaluates vocabulary understanding.

Quiz questions may include:

- word → definition
- definition → word
- IPA → word
- sentence/context → word

Quiz design may evolve while remaining within vocabulary-learning objectives.

---

# 18. Score and Attempts

The system records Student exercise activity.

Potential tracked information includes:

- attempt
- correct answers
- incorrect answers
- score
- accuracy
- completion status

Exact score calculations will be defined in Business Rules.

---

# 19. Vocabulary Mastery

The platform should track learning performance at vocabulary level where possible.

Example:

beautiful — 95%
environment — 82%
education — 74%
development — 42%

Vocabulary-level performance can be used to identify weak vocabulary.

The exact mastery calculation has not yet been finalized.

---

# 20. Weak Vocabulary

The system should identify vocabulary that a Student frequently answers incorrectly or has low mastery for.

Weak vocabulary can be used for targeted review.

Advanced personalized weak-vocabulary training may be a Premium feature.

---

# 21. Vocabulary Search

Vocabulary Search is a core Student feature.

Students can search vocabulary outside the Course learning flow.

Search results may provide:

- word
- IPA
- pronunciation audio
- part of speech
- English definition
- Vietnamese meaning
- example
- CEFR level

Standard Students should receive useful basic dictionary information.

Premium may provide deeper vocabulary information.

---

# 22. Saved Vocabulary

Students can save vocabulary.

Saved vocabulary is stored in:

`My Vocabulary`

My Vocabulary allows Students to maintain a personal vocabulary collection.

Saved vocabulary may be used as a source for future review and exercises.

---

# 23. Vocabulary Review

Review is part of the learning system.

Review vocabulary may come from:

- weak vocabulary
- saved vocabulary
- incorrectly answered vocabulary
- recently learned vocabulary

The first project version does not require a complex spaced-repetition algorithm.

Spaced repetition may be considered as a future improvement.

---

# 24. Learning Progress

Students should be able to track their learning progress.

Potential progress information includes:

- Course progress
- Lesson progress
- exercise scores
- accuracy
- vocabulary mastery
- weak vocabulary
- completed Lessons
- learning history

Premium may provide more detailed analytics.

---

# 25. Learning History

The platform should retain relevant Student learning activity.

Examples:

- completed exercise
- completed Quiz
- Lesson activity
- score
- date/time
- attempt

Learning history must not be deleted simply because Premium access expires.

---

# 26. Subscription

Premium uses a subscription model.

Initial subscription periods:

- Monthly
- Yearly

Conceptual flow:

Standard Student
→ Upgrade Premium
→ Choose Plan
→ Payment
→ Successful Transaction
→ Activate Subscription
→ Premium Access

---

# 27. Subscription Expiration

When Premium expires:

- Student account remains active
- Student returns to Standard access
- learning history remains
- Course history remains
- saved vocabulary remains
- learning progress remains
- previous scores remain

Premium-only functionality becomes unavailable until Premium is renewed.

---

# 28. Payment

The current business model uses Student Premium subscriptions as the platform revenue source.

The project does not currently include:

- Teacher commission
- Teacher payout
- Teacher withdrawal
- Course-by-course Teacher revenue sharing
- marketplace financial settlement

The exact payment provider has not yet been selected.

---

# 29. Admin Revenue Analytics

Admin should be able to view total verified successful-payment revenue and
revenue over a selected time range, separately by currency. Revenue grouping
uses stored plan identity or purchased Monthly/Yearly evidence, not current prices.

Potential metrics include:

- revenue today
- monthly revenue
- yearly revenue
- custom date-range revenue
- number of successful transactions
- number of active Premium subscriptions

---

# 30. Teacher Analytics

Teachers should be able to view analytics related to their own Courses.

Potential information includes:

- number of Courses
- number of enrollments
- active Students
- Course completion
- average score
- difficult vocabulary

Teachers should not have access to platform-wide revenue information.

---

# 31. Admin System Analytics

The operational Overview Dashboard shall include total Students, Teachers and
Courses, distinct currently entitled Premium Students, total and selected-range
verified revenue, new subscriptions, renewals, successful and confirmed-failed
payment counts, and revenue by plan where historical data supports it.

Other Admin analytics may include:

## Users

- total users
- new users
- active users
- Standard Students
- Premium Students

## Teachers

- total Teachers
- active Teachers

## Learning Content

- Courses
- Lessons
- vocabulary
- enrollments

## Business

- active subscriptions
- transactions
- revenue

---

# 32. Web Platform Requirements

The system is a web platform.

No native mobile application is required.

## Student UI

- mobile-first
- responsive
- touch-friendly
- optimized for vocabulary learning on smartphones
- usable on desktop

## Teacher UI

- responsive
- optimized for desktop content management
- functional on mobile

## Admin UI

- responsive
- optimized for desktop dashboards
- functional on mobile

---

# 33. Intended Technology Direction

The intended technology direction is currently:

## Frontend

- Next.js
- TypeScript
- responsive web design

## Backend

- Java
- Spring Boot

## Database

- PostgreSQL

Additional technology choices will be made during architecture design.

---

# 34. Current Core Scope

The current core project includes:

- authentication
- Student / Teacher / Admin roles
- Standard / Premium Student access
- CEFR levels
- multiple Teachers
- Teacher-owned Courses
- Course enrollment
- topic-based Lessons
- shared Vocabulary
- vocabulary senses
- IPA
- pronunciation audio
- dictionary integration
- Learn Vocabulary
- Fill Word
- Listening
- Quiz
- score
- attempts
- progress
- vocabulary mastery
- weak vocabulary
- vocabulary search
- saved vocabulary
- My Vocabulary
- review
- learning history
- subscription
- payment
- transactions
- Teacher analytics
- Admin analytics
- revenue analytics

---

# 35. Out of Scope for Initial Version

The following are currently outside the initial project scope:

- native Android application
- native iOS application
- live video classes
- video calls
- Teacher/Student chat
- social network
- forum
- Teacher marketplace
- Teacher commission
- Teacher payout
- certificates
- leaderboards
- complex gamification
- AI chatbot
- AI speaking evaluation
- placement test
- complex spaced repetition

These features may be considered future improvements.

---

# 36. Open Decisions

The following decisions remain intentionally unresolved:

1. Dictionary API/provider
2. Audio storage strategy
3. Text-to-Speech provider
4. Payment provider
5. Premium pricing
6. Exact Standard/Premium feature matrix
7. Score calculation
8. Vocabulary mastery calculation
9. Course completion rules
10. Lesson completion rules
11. Content approval workflow
12. Detailed Teacher permissions
13. Detailed Admin permissions
14. Database schema
15. API architecture
16. UI design system
17. Deployment architecture

These decisions should be addressed in later requirements, business-rule, architecture, and implementation stages.

---

# 37. Project Principle

The project should prioritize the complete core learning workflow:

Discover
→ Enroll
→ Learn
→ Practice
→ Assess
→ Track
→ Review

Teacher functionality exists to create and manage high-quality learning content.

Admin functionality exists to manage the platform and its business operations.

New features should not be added unless they clearly support these goals or are explicitly approved as part of the project scope.

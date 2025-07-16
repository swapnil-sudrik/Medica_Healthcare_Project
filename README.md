<h1 align="center">Medica HealthCare</h1>

<p align="center">
  <em>Transforming Healthcare Through Innovation and Care Excellence</em>
</p>

<p align="center">
  <img src="https://img.shields.io/github/last-commit/swapnil-sudrik/Medica_Healthcare_Project?style=flat-square" alt="Last Commit">
  <img src="https://img.shields.io/badge/java-100%25-blue?style=flat-square" alt="Language">
  <img src="https://img.shields.io/github/languages/count/swapnil-sudrik/Medica_Healthcare_Project?style=flat-square" alt="Languages Count">
</p>

---

<p align="center">
  <em>Built with the tools and technologies:</em>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Markup-Markdown-black?logo=Markdown&style=flat-square" alt="Markdown">
  <img src="https://img.shields.io/badge/Config-XML-blue?logo=XML&style=flat-square" alt="XML">
</p>



**Medica HealthCare** is a cloud-based healthcare practice management system designed to streamline hospital and clinic operations. It provides modules for managing patients, appointments, prescriptions, billing, and secure user roles — ensuring better patient care, efficient workflows, and HIPAA-compliant data security.

## 📋 Table of Contents

* [About](#about)
* [Features](#features)
* [Tech Stack](#tech-stack)
* [Modules](#modules)
* [Installation](#installation)
* [Environment Variables](#environment-variables)
* [Usage](#usage)
* [API Documentation](#api-documentation)
* [Contributing](#contributing)
* [License](#license)

## About

Medica HealthCare digitizes everyday hospital workflows. From appointment scheduling to billing and notifications, everything is handled securely using JWT-based authentication and role-based access control.

## Features

* Super Admin can register hospitals and manage onboarding.
* Admin can manage hospital users (Doctors, Receptionists).
* Doctors & Receptionists can manage patients, appointments, and prescriptions.
* E-prescription and electronic medical records.
* Flexible billing system (consultation and hospitalization).
* Notification system with login/logout and event-based updates.
* Session & JWT-based authentication.
* Production-grade logging using Log4j2.
* Role-based API access.
* HIPAA-compliant security design.

## 🛠Tech Stack

* **Backend:** Java 17, Spring Boot, Spring Security, JWT, Hibernate
* **Database:** MySQL (production) / H2 (local testing)
* **Frontend:** HTML, CSS, JavaScript, jQuery *(Optional React/Angular for future enhancement)*
* **DevOps:** AWS (EC2, S3), CI/CD pipelines
* **Logging:** Log4j2
* **Notifications:** WebSocket + Redis (for real-time persistent notifications)
* **APIs:** Paid APIs for WhatsApp & SMS integration

## Modules

| Module           | Description                                                                |
| ---------------- | -------------------------------------------------------------------------- |
| **User**         | Manages system users with roles: SUPER\_ADMIN, ADMIN, DOCTOR, RECEPTIONIST |
| **Hospital**     | Manages hospital registration, letterhead generation, and configurations   |
| **Patient**      | Stores patient records, history, and details                               |
| **Appointment**  | Schedules appointments between patients and doctors                        |
| **Prescription** | Generates ePrescriptions linked with appointments                          |
| **Billing**      | Generates bills based on doctor fee or hospitalization details             |
| **Notification** | Real-time notifications for system events (login/logout/updates)           |
| **Settings**     | System-wide configurations & environment setups                            |

## Installation

1. **Clone the repository**

   ```bash
   git clone https://github.com/your-username/medica-healthcare.git
   cd medica-healthcare
   ```

2. **Configure the Database**

    * Update `application.properties` with your MySQL credentials.
    * Use H2 for local development if needed.

3. **Build the project**

   ```bash
   ./mvnw clean install
   ```

4. **Run the application**

   ```bash
   ./mvnw spring-boot:run
   ```

## Environment Variables

Create an `.env` or configure your environment variables:

* `DB_URL` — Database connection URL
* `DB_USERNAME` — DB username
* `DB_PASSWORD` — DB password
* `JWT_SECRET` — Secret key for JWT
* `AWS_ACCESS_KEY` — AWS credentials
* `AWS_SECRET_KEY` — AWS credentials
* `SMS_API_KEY` — Paid SMS API Key
* `WHATSAPP_API_KEY` — Paid WhatsApp API Key

## Usage

* Access the app at: `http://localhost:8080`
* Use default SUPER\_ADMIN credentials:
  **Email:** `swapnilsudrik.s@gmail.com`
  **Password:** `superadmin123`
* Test APIs using Postman or the provided HTML forms.

## API Documentation

* API specs will be available soon.

## Contributing

1. Fork the repository.
2. Create a feature branch.
3. Commit your changes.
4. Push to your fork.
5. Create a Pull Request.

## License

This project is licensed under the MIT License.

**Note:** Once all requested resources (UI documentation, DevOps Engineer, Frontend Developers, Paid APIs, Backend docs) are in place, the team will finalize project deadlines accordingly.

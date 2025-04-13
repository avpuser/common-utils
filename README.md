# common-utils

A modular Java library containing various utilities and helper classes for backend development.

This project uses:
- **Java**: OpenJDK 23 (Docker image: `openjdk:23-jdk`)
- **Build system**: Maven

---

## 📦 Modules and Features

### 🛠 General Utilities (`utils`)
A collection of utility methods for:
- Date and time manipulation
- String operations
- File and image handling
- Number formatting
- JSON processing
- HTML manipulation
- Various formatting helpers

### 📤 FTPUploader
A simple FTP client to upload and manage files over FTP using `commons-net`.

### 🤖 GPT API Integration (`gpt`)
Client modules for working with:
- [DeepSeek API](https://deepseek.com/)
- [OpenAI API](https://platform.openai.com/)

Supports prompt creation, streaming, and parsing of responses.

### 🗃 MongoDB Utilities (`mongo`)
Tools for interacting with MongoDB using:
- [MongoJack](https://github.com/mongojack/mongojack)
- Native MongoDB Java driver

Includes helper classes for managing DAOs and serializing objects.

### ☁️ S3Uploader
Amazon S3-compatible client for uploading and managing objects via the AWS SDK.

### 📬 Telegram API Wrapper (`telegram`)
Utilities for working with the [Telegram Bots API](https://core.telegram.org/bots/api). Includes:
- Message sending
- Media handling
- Inline keyboards
- File downloads

---

## 🧱 Dependencies

Below are some of the key dependencies used in the project:

- `google-api-services-youtube` for YouTube API
- `pdfbox` for working with PDF files
- `tess4j` for OCR support
- `jackson-datatype-jsr310` and `jackson-datatype-jdk8` for modern Java date/time serialization
- `log4j` and SLF4J for logging
- `jsoup` for working with HTML
- `telegrambots` for Telegram bot integration
- `aws-sdk-s3` for AWS S3 support
- `commons-net`, `commons-io`, `commons-text` for basic I/O utilities
- `reflections` for runtime class scanning
- `JUnit` 4 and 5 for unit testing

Full dependency list is available in the [`pom.xml`](./pom.xml).

---

## 🏗 Build

To build the project, make sure you have Maven and JDK 23 installed, then run:

```bash
mvn clean install
```

---
## Tests

```bash
mvn test 
```

## 📜 License

MIT License

## 🧑‍💻 Author

Created by avpuser

## 🧰 Requirements

	•	Java 23 (OpenJDK)
	•	Maven 3.8+
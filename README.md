# ajaj - AI-powered Toolkit for Understanding and Improving Software Projects

> **⚠️ Experimental Project Notice**  
> **ajaj is currently in a very early, highly experimental and research-oriented state.**  
> This is an initial prototype under active development and many features, APIs, and internal mechanisms are not stable yet.  
> Expect rapid changes, incomplete functionality, and evolving architecture.

---

**ajaj** is an AI-assisted developer tool designed to analyze, explain, and improve software projects written in any mainstream programming language. Its primary goal is to help developers work more efficiently with complex or unfamiliar codebases by combining conversational AI, contextual project understanding, and semantic retrieval.

Unlike backend agent frameworks, **ajaj acts as an intelligent companion for software engineers** - helping you inspect, understand, refactor, document, and improve your projects through natural-language interaction.

---

## What the Project Does

### **Cross-language, AI-driven project analysis**
ajaj is language-agnostic and can work with codebases written in:

- Java / Kotlin
- TypeScript / JavaScript
- Python
- C#
- PHP
- Go
- Rust
- C / C++
- Swift
- …and other mainstream languages.

You can use ajaj to:

- Understand how parts of your system work
- Request architectural or design explanations
- Get refactoring or debugging assistance
- Generate documentation
- Identify improvements and technical debt
- Locate relevant code via semantic search
- Discuss performance, patterns, or best practices

### **Retrieval-Augmented Generation (RAG) based**
ajaj uses a vector store to index your project files and code.  
When you ask a question, RAG retrieves the most relevant pieces so the LLM can answer based on actual project context.

### **Persistent conversational memory**
Conversation history and previous analysis sessions are stored in PostgreSQL, allowing long-term collaboration across multiple sessions.

---

## Running the Project (Development Setup)

To run ajaj locally, you must start it with the **`dev` Spring profile**.

This profile activates all required development features:  
local LLM support, PostgreSQL persistence, vector search, and project-analysis tools.

---

## Requirements

### 1. Install Ollama

Ollama provides the local LLM used during development.

Download from:  
https://ollama.com/download

Pull at least one model, for example:

```bash
ollama pull gpt-oss:20b
ollama pull embeddinggemma
```

### 2. Install PostgreSQL with pgvector support

ajaj requires PostgreSQL with pgvector, since vector embeddings are stored directly in the database.

**Recommended Docker image**

If using Docker, the recommended image is:
```bash
pgvector/pgvector:pg17-trixie
```

This image comes with pgvector preinstalled and ready to use.

Example **`docker-compose.yml` configuration

```yaml
services:
  postgres:
    image: pgvector/pgvector:pg17-trixie
    container_name: postgres
    ports:
      - 127.0.0.1:5432:5432
    environment:
      POSTGRES_PASSWORD: postgres
      LANG: pl_PL.UTF-8
    volumes:
      - C:\Docker\Volumes\postgres:/var/lib/postgresql/data
    restart: unless-stopped
```

---
**Database configuration**

Create a database (e.g., ajaj) and configure it in application-dev.yaml:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ajaj
    username: ajaj
    password: your_password
```

Database Migration with Flyway

The PostgreSQL schema used by ajaj is fully managed and versioned using Flyway.
All database objects - including tables, indexes, and pgvector-related structures - are created automatically at application startup based on Flyway migration scripts stored in the project. This ensures consistent, repeatable setup of the database across all environments and prevents manual schema drift. Whenever you update the project, Flyway will automatically apply the necessary migrations to keep your database up to date.

---
### 3. Run ajaj with the dev profile

From the command line:

```bash
./gradlew bootRun --args="--spring.profiles.active=dev"
```

Or in IntelliJ IDEA:

```yaml
Active Profiles: dev
```

The dev profile enables:
- Ollama integration
- PostgreSQL-backed memory
- Vector embeddings storage (pgvector)
- RAG-based document retrieval
- Development tools for file/project inspection

---
## Purpose and Vision

The vision of ajaj is to become a universal, AI-powered companion for software engineers.

It aims to:
- Understand real-world projects
- Speak natural language
- Explain architecture and design choices
- Assist in debugging and refactoring
- Generate documentation
- Help with framework/language migrations
- Improve maintainability and code quality
- Act as a persistent, long-term collaborator
- Rather than replacing development tools, ajaj enhances them by providing contextual AI insights deeply connected to your actual codebase.

---
## Roadmap

- Interactive UI for project exploration
- Integration with static analysis tools
- Multi-agent reasoning modes (architect, reviewer, refactorer)
- Automatic documentation generation
- Deeper AST-based code ingestion
- Advanced search and visualization features

---
## License

This project is released under a permissive open-source license similar to MIT.

You are free to copy, use, modify, and distribute this project for any purpose,
including commercial use, as long as you include a clear notice that the original
author of the project is the ajaj team (initial creator: **`Przemysław Kotowski`).

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND.

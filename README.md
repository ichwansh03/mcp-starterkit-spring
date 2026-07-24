# Calculator MCP Server

Basic calculator service built as an MCP (Model Context Protocol) server using Spring AI. Exposes arithmetic operations as MCP tools for AI agents to consume.

## Goals

- Demonstrate how to build an MCP server with Spring Boot + Spring AI
- Provide simple calculator tools (add, subtract, multiply, divide, power, squareRoot, modulus, absolute)
- Serve as a reference project for MCP beginners

## How to Run

### Prerequisites

- Java 21+
- Maven

### Build & Run

```bash
mvn clean package -DskipTests
java -jar target/calculator-server-0.0.1-SNAPSHOT.jar
```

Server starts at `http://localhost:8080` — tools served at `POST /v1/tools`.

### Docker

```bash
docker build -t calculator-mcp-server .
docker run -p 8080:8080 calculator-mcp-server
```

### IDE Configuration

`mcp.json` configures the server in JetBrains IDEs: Settings → Tools → MCP.

### MCP Inspector

```bash
npx @modelcontextprotocol/inspector
```

## Learn MCP

- [mcp-for-beginners](https://github.com/microsoft/mcp-for-beginners) — Microsoft's hands-on tutorial for building MCP servers from scratch
- [Model Context Protocol specification](https://modelcontextprotocol.io/) — Official docs
- [Spring AI MCP docs](https://docs.spring.io/spring-ai/reference/api/mcp.html) — Spring AI MCP support
- [LangChain4j MCP](https://docs.langchain4j.dev/integrations/language-models/mcp) — Java MCP client integration

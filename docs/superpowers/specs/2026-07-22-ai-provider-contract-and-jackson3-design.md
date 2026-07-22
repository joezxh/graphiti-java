# AI Provider Capability Contract and Jackson 3 Migration Design

**Status:** approved for provider preservation; pending implementation plan  
**Scope:** Restore real Anthropic and Mistral support after the Spring AI 2 upgrade, synchronize capability documentation, and define—without implementing—the Jackson 3 migration.

## Provider contract

The supported LLM providers are OpenAI-compatible (including Qwen), Anthropic, Ollama, and Mistral. The supported embedding providers are OpenAI-compatible, Ollama, and Mistral. A provider may be advertised only when selecting it creates every required application bean without making a network request during context startup.

`OntoGraphApplication` must not unconditionally exclude Anthropic or Mistral auto-configuration. Model auto-configuration is selected through provider-specific configuration and the existing `graphiti.ai.llm-provider` / `embedding-provider` selectors.

## Implementation shape

- Keep the existing `LlmClientService` and `EmbedderService` interfaces unchanged.
- Keep `AnthropicLlmClientServiceImpl`, remove the auto-configuration exclusion that prevents its `AnthropicChatModel` dependency from existing.
- Add `MistralLlmClientServiceImpl` and `MistralEmbedderServiceImpl`, mirroring the existing provider adapters and guarded by `graphiti.ai` provider selectors.
- Restrict Mistral beans to their corresponding provider selection so they do not conflict with OpenAI-compatible, Anthropic, or Ollama beans.
- Update provider configuration examples to Spring AI 2 property names: remove the obsolete `.options` segment from OpenAI chat and embedding model properties.

## Tests

Use isolated Spring contexts with placeholder credentials and mock HTTP infrastructure. No provider test may call an external API. Test cases must prove:

1. OpenAI-compatible, Anthropic, Ollama, and Mistral selections each create the expected `LlmClientService`.
2. OpenAI-compatible, Ollama, and Mistral selections each create the expected `EmbedderService`.
3. Mistral selection creates both of its adapters and no conflicting provider adapter.
4. The application context remains startable with the default OpenAI-compatible configuration.

## Documentation synchronization

Update the English and Chinese READMEs and Docker Compose comments/environment examples to state the actual provider matrix, current dependency versions, and Azure migration guidance: Azure/Microsoft Foundry uses Spring AI 2's OpenAI-compatible integration rather than a dedicated Azure starter.

## Jackson 3 migration plan

Keep `spring-boot-jackson2` only as a compatibility bridge. A later dedicated migration will replace all `com.fasterxml.jackson.*` imports with Jackson 3 APIs, remove the bridge, and validate REST request/response, JWT error, OpenAPI, and PostgreSQL JSONB contracts. It must not be combined with the provider repair because it changes the application-wide JSON contract.

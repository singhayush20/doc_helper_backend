## Problems / Risks / Limitations

1. Simultaneous Requests (Same Thread)  

If two devices call /stream for the same threadId, both will:

Save the user message.

Start generating an assistant message.

The second one may override the lastMessageSnippet.

Out-of-order messages may occur.

2. Out-of-Order Saving of Assistant Messages

saveAssistantMessage is triggered by doFinally.

If two responses overlap in time:

The second one might complete first.

Leads to interleaving issues.

3. No Locking or Synchronization

MongoDB does not enforce row locks like SQL.

Two parallel writes to same threadId are possible.

LLM streaming does not enforce ordering.

4. Cancellation Registry Memory Leak Risk

If:

Client disconnects unexpectedly

Or you never call cancel()

The generationId → sink map can grow unbounded.

5. If You Don’t Use Locks

You may face:

Duplicate user messages.

Multiple assistant responses for the same message.

“Race finish” issues where smaller response gets saved last.

Thread snippet inconsistent with last message.

**Cancellation registry using Redis - Needs fixes**
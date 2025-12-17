# UDA-Hub: Universal Decision Agent for Customer Support

A multi-agent customer support system built with LangGraph for CultPass, a cultural experiences subscription service.

## Architecture Pattern: Hierarchical (Supervisor)

UDA-Hub uses a **Hierarchical/Supervisor architecture** where the Classifier Agent acts as supervisor, routing requests to specialized agents.

```
                    ┌──────────────────┐
                    │   User Message   │
                    └────────┬─────────┘
                             │
                             ▼
┌────────────────────────────────────────────────────────────┐
│              CLASSIFIER AGENT (Supervisor)                  │
│         Categorizes tickets, decides routing               │
└────────────────────────────┬───────────────────────────────┘
                             │
              ┌──────────────┴──────────────┐
              ▼                             ▼
┌─────────────────────────┐    ┌─────────────────────────────┐
│   ACCOUNT LOOKUP AGENT  │    │     ESCALATION AGENT        │
│   Fetches user context  │    │  Blocked accounts, human    │
└───────────┬─────────────┘    │  requests, low confidence   │
            │                   └─────────────────────────────┘
            ▼
┌─────────────────────────┐
│     RAG RETRIEVAL       │
│  Semantic KB search     │
│  + Confidence scoring   │
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐
│    RESOLVER AGENT       │
│  Generates response     │
│  using KB + context     │
└─────────────────────────┘
```

## 4 Agents

| Agent | Role | Responsibility |
|-------|------|----------------|
| **Classifier** | Supervisor | Categorize tickets, route to appropriate path |
| **Account Lookup** | Context Provider | Fetch user info, subscription status, conversation history |
| **Resolver** | Solution Provider | Generate responses using RAG and user context |
| **Escalation** | Human Handoff | Handle blocked accounts, human requests, low-confidence cases |

## Key Features

- **4 Specialized Agents** - Each with single responsibility
- **Hierarchical Architecture** - Classifier as supervisor
- **RAG with Chroma** - Semantic search using OpenAI embeddings
- **Confidence-Based Escalation** - Low KB match (< 70%) triggers human handoff
- **Long-Term Memory** - Persists to TicketMessage table across sessions
- **Structured Logging** - Full visibility into agent decisions
- **Session Memory** - Conversation context via LangGraph MemorySaver

## Getting Started

### Prerequisites

- Python 3.10+
- OpenAI API key

### Installation

```bash
# 1. Create virtual environment
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate

# 2. Install dependencies
pip install -r requirements.txt

# 3. Setup environment
cp .env.example .env
# Edit .env and add your OPENAI_API_KEY

# 4. Initialize databases
python 01_external_db_setup.py

# Option A: Run notebook (recommended)
jupyter notebook 02_core_db_setup.ipynb

# Option B: Run script
python 02_core_db_setup.py

# 5. Run the application
jupyter notebook 03_agentic_app.ipynb
# OR
python 03_agentic_app.py
```

## Project Structure

```
starter/
├── agentic/
│   ├── agents/
│   │   ├── classifier.py      # Ticket classification (Supervisor)
│   │   ├── account_lookup.py  # User context retrieval
│   │   ├── resolver.py        # Knowledge-based resolution
│   │   └── escalation.py      # Human handoff
│   ├── tools/
│   │   ├── rag_tools.py       # Chroma + OpenAI RAG
│   │   ├── memory.py          # Long-term memory persistence
│   │   └── db_tools.py        # Database lookup tools
│   ├── design/
│   │   └── SYSTEM_DESIGN.md   # Architecture documentation
│   └── workflow.py            # LangGraph orchestration + logging
├── data/
│   ├── core/                  # UDA-Hub DB + Chroma vectors
│   ├── external/              # CultPass DB
│   └── models/                # SQLAlchemy models
├── tests/
├── 01_external_db_setup.py
├── 02_core_db_setup.py
├── 02_core_db_setup.ipynb     # Notebook version (recommended)
├── 03_agentic_app.py
├── 03_agentic_app.ipynb       # Full demo notebook
└── utils.py
```

## How It Works

### Flow

1. **User Message** → Enters workflow
2. **Classifier** → Categorizes (login, billing, subscription, etc.)
3. **Router** → Check if immediate escalation needed
4. **Account Lookup** → Fetch user context + conversation history
5. **RAG Retrieval** → Search KB, calculate confidence score
6. **Confidence Check** → If < 70%, escalate
7. **Resolver/Escalation** → Generate response
8. **Memory Storage** → Persist interaction for future sessions

### Confidence-Based Escalation

The system automatically escalates when KB matches are poor:

| Confidence | Condition | Action |
|------------|-----------|--------|
| 90% | 1+ High relevance match | Resolve |
| 75% | 2+ Medium matches | Resolve |
| 60% | 1 Medium match | Resolve |
| 40% | Only Low matches | **Escalate** |
| 0% | No matches / Error | **Escalate** |

### Long-Term Memory

Conversations persist to the `TicketMessage` table:

```python
# Store interaction
store_interaction(ticket_id, "user", "I need help")
store_interaction(ticket_id, "ai", "How can I assist?")

# Retrieve across sessions
history = get_history(ticket_id, limit=10)
```

### Logging

All agent decisions are logged:

```
14:32:01 | INFO | [CLASSIFIER] Processing: I can't log into my account...
14:32:02 | INFO | [CLASSIFIER] Category: login_access, Escalate: False
14:32:02 | INFO | [ROUTER] Proceeding to account lookup
14:32:02 | INFO | [ACCOUNT_LOOKUP] Checking for user context...
14:32:03 | INFO | [RAG] Searching KB for category: login_access
14:32:03 | INFO | [RAG] Confidence score: 0.90
14:32:04 | INFO | [RESOLVER] Generating response...
```

## RAG Implementation

```
User Query → OpenAI Embeddings → Chroma Vector Search → Top 3 Articles → LLM Response
```

- **Embedding Model**: OpenAI `text-embedding-ada-002` (1536 dimensions)
- **Vector Store**: Chroma (persisted to `data/core/chroma_db/`)
- **Retrieval**: Cosine similarity, Top-K = 3
- **Semantic Search**: "I can't get in" matches "login issues" without exact keywords

## Testing

```bash
# Run all tests
python -m pytest tests/ -v

# Or directly
python tests/test_agents.py
```

### Test Coverage

- **TestClassifierAgent** - Classification accuracy
- **TestAccountLookupAgent** - User context retrieval
- **TestRAGRetrieval** - Semantic search with confidence
- **TestResolverAgent** - Response generation
- **TestEscalationAgent** - Human handoff
- **TestMemoryPersistence** - Long-term memory
- **TestWorkflowIntegration** - End-to-end flows

## Built With

- [LangGraph](https://github.com/langchain-ai/langgraph) - Multi-agent orchestration
- [LangChain](https://github.com/langchain-ai/langchain) - LLM framework
- [Chroma](https://www.trychroma.com/) - Vector database
- [OpenAI](https://openai.com/) - GPT-4o-mini & Embeddings
- [SQLAlchemy](https://www.sqlalchemy.org/) - Database ORM

## Documentation

See `agentic/design/SYSTEM_DESIGN.md` for detailed architecture documentation including:
- Agent roles and responsibilities
- Memory architecture (short-term + long-term)
- Data flow diagrams
- RAG implementation details
- Logging configuration

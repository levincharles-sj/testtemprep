# Document Assistant Project

A sophisticated multi-agent document processing system built with LangChain and LangGraph. This assistant intelligently classifies user intent, routes requests to specialized agents (Q&A, Summarization, Calculation), and maintains conversation context across multiple turns using state management.

## Getting Started

These instructions will get you a copy of the project running on your local machine for development and testing purposes.

### Dependencies

You'll need Python 3.9+ and the following libraries:
```
langchain>=0.1.0           # LLM orchestration framework
langchain-openai>=0.0.5    # OpenAI integration
langgraph>=0.0.20          # State graph workflow engine  
langchain-core>=0.1.0      # Core LangChain utilities
openai>=1.0.0              # OpenAI API client
pydantic>=2.0.0            # Data validation and schemas
python-dotenv>=1.0.0       # Environment variable management
pytest>=7.4.0              # Testing framework
```

You'll also need:
* An OpenAI API key (get one at https://platform.openai.com/api-keys)
* Basic understanding of Python and virtual environments

### Installation

Step-by-step guide to get a development environment running:

**1. Clone or download the project**
```bash
cd doc_assistant_project
```

**2. Create and activate a virtual environment**
```bash
python -m venv venv

# On macOS/Linux:
source venv/bin/activate

# On Windows:
venv\Scripts\activate
```

**3. Install dependencies**
```bash
pip install -r requirements.txt
```

**4. Set up your OpenAI API key**

Option A - Environment variable:
```bash
export OPENAI_API_KEY='sk-your-api-key-here'
```

Option B - Create a `.env` file:
```
OPENAI_API_KEY=sk-your-api-key-here
```

**5. Run the assistant**
```bash
# Interactive mode (recommended for first run)
python main.py interactive

# Single query mode
python main.py "What is the Q3 revenue?"

# Run test scenarios
python main.py test
```

**Example output:**
```
Document Assistant initialized with model: gpt-4
You: What is the Q3 revenue?
[Processing...]
Assistant: According to the Q3 financial report, the revenue is $5,000,000.
[Intent: qa]
[Sources: doc_001]
```

---

## 🗣️ Example Conversations

Here are real conversations demonstrating how the system handles different intent types:

### Example 1: Q&A Intent

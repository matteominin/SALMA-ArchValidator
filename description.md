# Software Project Validation Workflow

This document details the software engineering project validation process, which is divided into two main phases: **structure validation** and **content validation**.

## Phase 1: Structure Validation

The goal of this phase is to ensure that a project report is well-structured and contains the necessary features for a comprehensive review.

### 1. Section Division
The system receives a PDF report and divides it into logical sections based on the document's index.

### 2. Feature Extraction and Comparison
The system analyzes the content of each section to extract key features. These features are then compared against a checklist of verified features sourced from a database of validated projects. The comparison is performed by using a clustering algorithm with **cosine similarity** to find features that are semantically similar to the verified ones.

## Phase 2: Content Validation

This phase evaluates the project's intrinsic quality by performing a deep-dive analysis of the content. This is where specialized LLM agents come into play.

### 1. Data Extraction with Specialized Agents
The validated sections are sent to dedicated LLM agents for in-depth content extraction. Each agent follows a strict prompt to consolidate its specific data into a clean and coherent format.

#### Agent Types:

- **Architecture Agent**: Extracts, consolidates, and refines architectural patterns and components, ensuring a clean, comprehensive list.

- **Use Case Agent**: Extracts detailed use cases, including actors, main flows, and alternative flows.

- **Requirements Agent**: Extracts, filters, and de-duplicates requirements to create a clean list of specific, verifiable statements.

### 2. Verification and Report Generation
The final, consolidated data (lists of requirements, use cases, and architectural components) is passed to a dedicated verification agent. This agent performs a final comparison, mapping each requirement to its supporting use cases and architectural components. 

For each requirement, it assigns a status of:
- **Satisfied**
- **Partially Satisfied** 
- **Unsatisfied**

Each status assignment includes detailed reasoning for the decision.

## Final Output

The final output is a comprehensive verification report that highlights any gaps in the project's design, from the structural coherence of the document to the internal consistency between requirements and architecture.
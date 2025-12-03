#!/usr/bin/env python3
"""
Extract all prompts from the multi-agent workflow for comparison with single-agent
"""

import json
import subprocess

DUMP_PATH = "/Users/matteominin/Desktop/tesi/pdf_extractor/dump/nov16-final/sallma-prod"

def load_bson_collection(collection_name):
    """Load a BSON collection into memory as JSON objects"""
    bson_file = f"{DUMP_PATH}/{collection_name}.bson"
    result = subprocess.run(
        ["bsondump", bson_file],
        stdout=subprocess.PIPE,
        stderr=subprocess.DEVNULL,
        text=True
    )

    objects = []
    for line in result.stdout.strip().split('\n'):
        if line:
            try:
                objects.append(json.loads(line))
            except json.JSONDecodeError:
                continue
    return objects

def main():
    print("Loading nodes from BSON dump...")
    nodes = load_bson_collection("meta_nodes")

    # Load the workflow structure to get node order
    with open("/Users/matteominin/Desktop/tesi/pdf_extractor/workflow-structure.json", 'r') as f:
        structure = json.load(f)

    # Define the agents we want to extract in order
    agent_names = [
        "Index Extractor",
        "Orchestrator",
        "Requirements Agent",
        "Use Case Agent",
        "Architectural Agent",
        "Test Agent",
        "ReqToUc",
        "Architecture consolidation",
        "UcToArc",
        "UcToTest",
        "Treaceability report",
        "Feature Extractor",
        "Checklist verifier"
    ]

    output = []
    output.append("# Multi-Agent Workflow: All Prompts\n")
    output.append(f"**Workflow:** {structure['name']}")
    output.append(f"**Workflow ID:** {structure['id']}")
    output.append(f"**Description:** {structure['description']}\n")
    output.append("=" * 100 + "\n")

    for idx, agent_name in enumerate(agent_names, 1):
        # Find the node
        node = next((n for n in nodes if n.get("name") == agent_name), None)
        if not node:
            print(f"Warning: Node '{agent_name}' not found")
            continue

        prompt = node.get("systemPromptTemplate", "")
        description = node.get("description", "")

        output.append(f"\n## Agent {idx}: {agent_name}\n")
        output.append(f"**Description:** {description}\n")
        output.append(f"**Type:** {node.get('type', 'Unknown')}\n")

        if node.get('modelName'):
            output.append(f"**Model:** {node.get('provider', '')}/{node.get('modelName', '')}\n")

        output.append(f"\n### Prompt:\n")
        output.append("```")
        output.append(prompt)
        output.append("```\n")
        output.append("=" * 100 + "\n")

    # Write output
    output_text = "\n".join(output)
    output_file = "/Users/matteominin/Desktop/tesi/pdf_extractor/multi-agent-prompts.md"
    with open(output_file, 'w') as f:
        f.write(output_text)

    print(f"All prompts extracted to: {output_file}")
    print(f"Total agents with prompts: {len(agent_names)}")

if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""
Analyze the multi-agent workflow and extract all prompts to compare with single-agent approach
"""

import json
import subprocess
from collections import defaultdict

# Workflow ID to analyze
MAIN_WORKFLOW_ID = "9c904b9d-62ae-4d92-b7d0-e9733815f62d"
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

def get_workflow_by_id(workflows, workflow_id):
    """Find a workflow by its _id"""
    for wf in workflows:
        if wf.get('_id') == workflow_id:
            return wf
    return None

def get_node_by_id(nodes, node_id):
    """Find a node by its _id"""
    for node in nodes:
        if node.get('_id') == node_id:
            return node
    return None

def extract_workflow_structure(workflows, nodes, workflow_id, depth=0, visited=None):
    """Recursively extract the structure of a workflow and all its sub-workflows"""
    if visited is None:
        visited = set()

    if workflow_id in visited:
        return None
    visited.add(workflow_id)

    workflow = get_workflow_by_id(workflows, workflow_id)
    if not workflow:
        return None

    indent = "  " * depth
    structure = {
        "id": workflow_id,
        "name": workflow.get("name", "Unknown"),
        "description": workflow.get("description", ""),
        "depth": depth,
        "nodes": [],
        "sub_workflows": []
    }

    print(f"{indent}{'=' * 80}")
    print(f"{indent}WORKFLOW: {structure['name']}")
    print(f"{indent}ID: {workflow_id}")
    print(f"{indent}Description: {structure['description']}")
    print(f"{indent}{'=' * 80}")

    for node in workflow.get("nodes", []):
        node_id = node.get("_id")
        metamodel_id = node.get("metamodelId")
        node_type = node.get("type")

        node_info = {
            "node_id": node_id,
            "metamodel_id": metamodel_id,
            "type": node_type,
            "name": "Unknown"
        }

        if node_type == "SUB_WORKFLOW":
            # This is a sub-workflow, recurse into it
            print(f"{indent}  Node {node_id}: SUB_WORKFLOW -> {metamodel_id}")
            sub_structure = extract_workflow_structure(workflows, nodes, metamodel_id, depth + 1, visited)
            if sub_structure:
                structure["sub_workflows"].append(sub_structure)
                node_info["name"] = sub_structure["name"]
                node_info["sub_workflow"] = sub_structure
        else:
            # This is a regular node, get its metadata
            node_meta = get_node_by_id(nodes, metamodel_id)
            if node_meta:
                node_name = node_meta.get("name", "Unknown")
                node_description = node_meta.get("description", "")
                node_info["name"] = node_name
                node_info["description"] = node_description

                print(f"{indent}  Node {node_id}: {node_name}")
                print(f"{indent}    Description: {node_description}")

                # Check if it's an LLM node with a prompt
                if "systemPromptTemplate" in node_meta:
                    prompt = node_meta["systemPromptTemplate"]
                    if prompt:
                        node_info["prompt"] = prompt
                        print(f"{indent}    LLM Prompt: {prompt[:200]}...")
                        print()

        structure["nodes"].append(node_info)

    print()
    return structure

def count_prompts(structure):
    """Count total nodes and LLM prompts in the structure"""
    total_nodes = len(structure["nodes"])
    llm_nodes = sum(1 for n in structure["nodes"] if "prompt" in n)

    for sub_wf in structure["sub_workflows"]:
        sub_total, sub_llm = count_prompts(sub_wf)
        total_nodes += sub_total
        llm_nodes += sub_llm

    return total_nodes, llm_nodes

def main():
    print("Loading workflows and nodes from BSON dump...")
    workflows = load_bson_collection("meta_workflows")
    nodes = load_bson_collection("meta_nodes")

    print(f"Loaded {len(workflows)} workflows and {len(nodes)} nodes\n")

    print("Analyzing workflow hierarchy...\n")
    structure = extract_workflow_structure(workflows, nodes, MAIN_WORKFLOW_ID)

    if structure:
        print("\n" + "=" * 80)
        print("SUMMARY")
        print("=" * 80)
        total_nodes, llm_nodes = count_prompts(structure)
        print(f"Total nodes in workflow hierarchy: {total_nodes}")
        print(f"LLM nodes with prompts: {llm_nodes}")
        print(f"Tool/API nodes: {total_nodes - llm_nodes}")

        # Save structure to JSON
        output_file = "/Users/matteominin/Desktop/tesi/pdf_extractor/workflow-structure.json"
        with open(output_file, 'w') as f:
            json.dump(structure, f, indent=2)
        print(f"\nFull structure saved to: {output_file}")
    else:
        print("Failed to extract workflow structure")

if __name__ == "__main__":
    main()

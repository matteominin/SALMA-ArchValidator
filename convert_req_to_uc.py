#!/usr/bin/env python3
"""
Convert malformed req_to_uc.json to proper JSON format.
The input file uses `key=value` instead of `"key": "value"` syntax.
"""

import json
import re
import sys
from pathlib import Path
from typing import Any, Dict, List


# Known keys in the objects - helps us parse more accurately
# This will be auto-detected from the first object or can be specified
KNOWN_KEYS = None

def detect_keys(text: str) -> set:
    """
    Detect the keys used in the first object to determine the schema.
    """
    # Find the first object
    start = text.find('{')
    if start == -1:
        return {'req_id', 'covered_by_use_cases', 'status', 'rationale'}  # default

    end = text.find('}', start)
    if end == -1:
        return {'req_id', 'covered_by_use_cases', 'status', 'rationale'}  # default

    first_obj = text[start:end+1]

    # Find all key= patterns
    import re
    pattern = r'\b([a-z_]+)\s*='
    keys = set(re.findall(pattern, first_obj))

    return keys if keys else {'req_id', 'covered_by_use_cases', 'status', 'rationale'}


def parse_object(text: str, start: int, known_keys: set = None) -> tuple[Dict, int]:
    """
    Parse an object starting at position start (should be at '{').
    Returns (dict, next_position)

    Objects have the format: {key1=value1, key2=value2, ...}
    where keys are from known_keys and values can be strings, arrays, etc.
    If known_keys is None, keys are auto-detected.
    """
    if text[start] != '{':
        raise ValueError(f"Expected '{{' at position {start}")

    # If no known keys provided, auto-detect from this object
    if known_keys is None:
        # Find the matching closing brace
        depth = 0
        end = start
        for idx in range(start, len(text)):
            if text[idx] == '{':
                depth += 1
            elif text[idx] == '}':
                depth -= 1
                if depth == 0:
                    end = idx
                    break
        if end == start:
            raise ValueError("Cannot find end of object")
        obj_text = text[start:end+1]
        import re
        pattern = r'\b([a-z_]+)\s*='
        known_keys = set(re.findall(pattern, obj_text))

    result = {}
    i = start + 1

    while i < len(text):
        # Skip whitespace
        while i < len(text) and text[i].isspace():
            i += 1

        if i >= len(text):
            raise ValueError("Unexpected end of object")

        # Check for end of object
        if text[i] == '}':
            return result, i + 1

        # Try to find a known key
        found_key = None
        for key in known_keys:
            if text[i:i+len(key)] == key and (i+len(key) >= len(text) or text[i+len(key)] in '= \t'):
                found_key = key
                i += len(key)
                break

        if not found_key:
            # Unexpected content
            context = text[max(0, i-20):i+20]
            raise ValueError(f"Expected a known key at position {i}, context: ...{context}...")

        # Skip whitespace before =
        while i < len(text) and text[i].isspace():
            i += 1

        # Expect =
        if i >= len(text) or text[i] != '=':
            raise ValueError(f"Expected '=' after key '{found_key}' at position {i}")

        # Skip the =
        i += 1

        # Parse the value based on what comes next
        while i < len(text) and text[i].isspace():
            i += 1

        if i >= len(text):
            raise ValueError(f"No value for key '{found_key}'")

        # Check if it's an array
        if text[i] == '[':
            value, i = parse_array(text, i, known_keys)
        else:
            # It's a simple value - read until we hit ", next_key=" or "}"
            # Build a pattern to look for the next known key
            value = ""
            depth = 0

            while i < len(text):
                char = text[i]

                # Track depth for nested structures (shouldn't happen in simple values but be safe)
                if char in '[{':
                    depth += 1
                    value += char
                    i += 1
                    continue
                elif char in ']}':
                    if depth == 0:
                        # End of our object
                        break
                    depth -= 1
                    value += char
                    i += 1
                    continue

                # If depth is 0, check if we're at the start of a new key-value pair
                if depth == 0:
                    # Check for ", known_key="
                    if char == ',':
                        # Look ahead to see if there's a known key coming
                        temp_i = i + 1
                        while temp_i < len(text) and text[temp_i].isspace():
                            temp_i += 1

                        # Check if any known key starts here
                        found_next_key = False
                        for key in known_keys:
                            if text[temp_i:temp_i+len(key)] == key:
                                # Check if followed by = (possibly with whitespace)
                                check_i = temp_i + len(key)
                                while check_i < len(text) and text[check_i].isspace():
                                    check_i += 1
                                if check_i < len(text) and text[check_i] == '=':
                                    found_next_key = True
                                    break

                        if found_next_key:
                            # This comma is a field separator, not part of the value
                            break
                        else:
                            # This comma is part of the value
                            value += char
                            i += 1
                            continue
                    elif char == '}':
                        # End of object
                        break
                    else:
                        value += char
                        i += 1
                        continue
                else:
                    value += char
                    i += 1
                    continue

            value = value.strip()

        result[found_key] = value

        # Skip whitespace
        while i < len(text) and text[i].isspace():
            i += 1

        # Expect comma or }
        if i < len(text) and text[i] == ',':
            i += 1
        elif i < len(text) and text[i] == '}':
            return result, i + 1

    raise ValueError("Unclosed object")


def parse_array(text: str, start: int, known_keys: set = None) -> tuple[List, int]:
    """
    Parse an array starting at position start (should be at '[').
    Returns (list, next_position)
    """
    if text[start] != '[':
        raise ValueError(f"Expected '[' at position {start}")

    result = []
    i = start + 1

    while i < len(text):
        # Skip whitespace
        while i < len(text) and text[i].isspace():
            i += 1

        if i >= len(text):
            raise ValueError("Unexpected end of array")

        # Check for end of array
        if text[i] == ']':
            return result, i + 1

        # Parse the value
        if text[i] == '{':
            # For nested objects in arrays, auto-detect their keys
            value, i = parse_object(text, i, None)
        else:
            # Simple value - read until comma or ]
            value = ""
            depth = 0
            while i < len(text):
                char = text[i]
                if char in '[{':
                    depth += 1
                    value += char
                elif char in ']}':
                    if depth == 0:
                        break
                    depth -= 1
                    value += char
                elif char == ',' and depth == 0:
                    break
                else:
                    value += char
                i += 1
            value = value.strip()

        result.append(value)

        # Skip whitespace
        while i < len(text) and text[i].isspace():
            i += 1

        # Expect comma or ]
        if i < len(text) and text[i] == ',':
            i += 1
        elif i < len(text) and text[i] == ']':
            return result, i + 1

    raise ValueError("Unclosed array")


def convert_to_proper_json(input_file: str, output_file: str = None) -> None:
    """
    Convert malformed JSON with `key=value` syntax to proper JSON.

    Args:
        input_file: Path to input file
        output_file: Path to output file (defaults to input_file if not specified)
    """
    # Read the malformed content
    with open(input_file, 'r', encoding='utf-8') as f:
        content = f.read()

    # Parse the content
    content = content.strip()

    if not content.startswith('['):
        raise ValueError("Expected array at top level")

    # Detect keys from the first object
    known_keys = detect_keys(content)
    print(f"Detected keys: {sorted(known_keys)}")

    # Parse as array
    try:
        data, _ = parse_array(content, 0, known_keys)
    except Exception as e:
        print(f"Error during parsing: {e}")
        import traceback
        traceback.print_exc()
        raise

    # Validate by converting to JSON and back
    try:
        json_str = json.dumps(data, indent=2, ensure_ascii=False)
        data = json.loads(json_str)
    except Exception as e:
        print(f"Error validating JSON: {e}")
        # Save debug output
        debug_file = input_file.replace('.json', '_debug.json')
        with open(debug_file, 'w', encoding='utf-8') as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
        print(f"Debug output saved to: {debug_file}")
        raise

    # Determine output file
    if output_file is None:
        output_file = input_file

    # Write properly formatted JSON
    with open(output_file, 'w', encoding='utf-8') as f:
        json.dump(data, f, indent=2, ensure_ascii=False)

    print(f"✓ Successfully converted {input_file}")
    print(f"✓ Output written to: {output_file}")
    print(f"✓ Total records: {len(data)}")

    # Print summary statistics
    covered = sum(1 for item in data if isinstance(item, dict) and item.get('status') == 'Covered')
    unsupported = sum(1 for item in data if isinstance(item, dict) and item.get('status') == 'UNSUPPORTED')

    print(f"\nSummary:")
    print(f"  Covered: {covered}")
    print(f"  Unsupported: {unsupported}")
    print(f"  Total: {len(data)}")


def main():
    """Main entry point."""
    # Parse command line arguments
    if len(sys.argv) < 2:
        print("Usage: python3 convert_req_to_uc.py <input_file> [output_file]")
        print("\nExamples:")
        print("  python3 convert_req_to_uc.py report/req_to_uc.json")
        print("  python3 convert_req_to_uc.py report/req_to_uc.json report/req_to_uc_converted.json")
        sys.exit(1)

    input_file = sys.argv[1]
    output_file = sys.argv[2] if len(sys.argv) > 2 else None

    # Check if file exists
    if not Path(input_file).exists():
        print(f"Error: File not found: {input_file}")
        sys.exit(1)

    print(f"Converting {input_file}...\n")

    # Convert (overwrites original file if output_file not specified)
    convert_to_proper_json(input_file, output_file)


if __name__ == '__main__':
    main()

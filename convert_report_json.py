#!/usr/bin/env python3
"""
Generic JSON converter for malformed JSON files using `key=value` syntax.
Converts to proper `"key": "value"` JSON format.

Usage:
    python3 convert_report_json.py <input_file> [output_file]
"""

import json
import re
import sys
from pathlib import Path
from typing import Any, Dict, List, Set


def detect_keys(text: str) -> Set[str]:
    """
    Detect the keys used in the first object to determine the schema.
    """
    # Find the first object
    start = text.find('{')
    if start == -1:
        return set()  # No default, will auto-detect

    end = text.find('}', start)
    if end == -1:
        return set()

    first_obj = text[start:end+1]

    # Find all key= patterns
    pattern = r'\b([a-zA-Z_][a-zA-Z0-9_]*)\s*='
    keys = set(re.findall(pattern, first_obj))

    return keys


def parse_value(text: str, start: int, known_keys: Set[str] = None) -> tuple[Any, int]:
    """
    Parse a value starting at position start.
    Returns (value, next_position)

    Values can be:
    - Objects: {...}
    - Arrays: [...]
    - Strings or other primitives
    """
    # Skip whitespace
    while start < len(text) and text[start].isspace():
        start += 1

    if start >= len(text):
        raise ValueError("Unexpected end of input while parsing value")

    # Check what type of value we have
    if text[start] == '{':
        return parse_object(text, start, known_keys)
    elif text[start] == '[':
        return parse_array(text, start, known_keys)
    else:
        return parse_simple_value(text, start, known_keys)


def parse_simple_value(text: str, start: int, known_keys: Set[str] = None) -> tuple[str, int]:
    """
    Parse a simple value (string, number, boolean, etc.)
    Reads until we hit a delimiter (comma, closing bracket/brace)
    """
    value = ""
    depth = 0
    i = start

    while i < len(text):
        char = text[i]

        # Track depth for nested structures
        if char in '[{':
            depth += 1
            value += char
            i += 1
            continue
        elif char in ']}':
            if depth == 0:
                # End of our value
                break
            depth -= 1
            value += char
            i += 1
            continue

        # If depth is 0, check if we're at a delimiter
        if depth == 0:
            if char == ',':
                # Check if this comma is a field separator
                if known_keys:
                    temp_i = i + 1
                    while temp_i < len(text) and text[temp_i].isspace():
                        temp_i += 1

                    # Check if any known key starts here
                    found_next_key = False
                    for key in known_keys:
                        if text[temp_i:temp_i+len(key)] == key:
                            check_i = temp_i + len(key)
                            while check_i < len(text) and text[check_i].isspace():
                                check_i += 1
                            if check_i < len(text) and text[check_i] == '=':
                                found_next_key = True
                                break

                    if found_next_key:
                        break
                else:
                    # Without known keys, treat comma as end of value
                    break

                value += char
                i += 1
                continue
            elif char in ']}':
                break
            else:
                value += char
                i += 1
                continue
        else:
            value += char
            i += 1
            continue

    return value.strip(), i


def parse_object(text: str, start: int, known_keys: Set[str] = None) -> tuple[Dict, int]:
    """
    Parse an object starting at position start (should be at '{').
    Returns (dict, next_position)

    Objects have the format: {key1=value1, key2=value2, ...}
    """
    if text[start] != '{':
        raise ValueError(f"Expected '{{' at position {start}")

    # If no known keys provided, auto-detect from this object
    if known_keys is None or len(known_keys) == 0:
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
        pattern = r'\b([a-zA-Z_][a-zA-Z0-9_]*)\s*='
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
        if known_keys:
            for key in known_keys:
                if text[i:i+len(key)] == key and (i+len(key) >= len(text) or text[i+len(key)] in '= \t\n\r'):
                    found_key = key
                    i += len(key)
                    break

        if not found_key:
            # Try to parse any key pattern
            match = re.match(r'([a-zA-Z_][a-zA-Z0-9_]*)\s*=', text[i:])
            if match:
                found_key = match.group(1)
                i += len(match.group(0)) - 1  # -1 because we'll skip the = below
            else:
                context = text[max(0, i-20):i+20]
                raise ValueError(f"Expected a key at position {i}, context: ...{context}...")

        # Skip whitespace before =
        while i < len(text) and text[i].isspace():
            i += 1

        # Expect =
        if i >= len(text) or text[i] != '=':
            raise ValueError(f"Expected '=' after key '{found_key}' at position {i}")

        # Skip the =
        i += 1

        # Skip whitespace after =
        while i < len(text) and text[i].isspace():
            i += 1

        if i >= len(text):
            raise ValueError(f"No value for key '{found_key}'")

        # Parse the value
        value, i = parse_value(text, i, known_keys)

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


def parse_array(text: str, start: int, known_keys: Set[str] = None) -> tuple[List, int]:
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
        value, i = parse_value(text, i, None)  # Auto-detect keys for array elements

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

    if not content:
        print(f"Warning: {input_file} is empty")
        return

    # Detect the root structure
    known_keys = detect_keys(content)
    if known_keys:
        print(f"Detected keys: {sorted(known_keys)}")

    # Parse based on root structure
    try:
        if content.startswith('['):
            data, _ = parse_array(content, 0, known_keys)
        elif content.startswith('{'):
            data, _ = parse_object(content, 0, known_keys)
        else:
            raise ValueError("Expected array or object at top level")
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

    # Print summary statistics
    if isinstance(data, list):
        print(f"✓ Total records: {len(data)}")
    elif isinstance(data, dict):
        print(f"✓ Top-level keys: {', '.join(data.keys())}")


def main():
    """Main entry point."""
    # Parse command line arguments
    if len(sys.argv) < 2:
        print("Usage: python3 convert_report_json.py <input_file> [output_file]")
        print("\nExamples:")
        print("  python3 convert_report_json.py report/report.json")
        print("  python3 convert_report_json.py report/data.json report/data_converted.json")
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

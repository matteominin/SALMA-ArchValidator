#!/usr/bin/env python3
"""
Script to convert malformed JSON with Python dict syntax to proper JSON
Handles patterns like {key=value} and converts to {"key":"value"}
"""

import re
import json


def fix_json_content(content):
    """
    Fix JSON content by converting Python dict-like syntax to proper JSON
    Recursively processes all key=value patterns
    """
    # Remove smart quotes
    content = content.replace(''', "'").replace(''', "'")
    content = content.replace('"', '"').replace('"', '"')

    # Use a simple regex replacement that works on the whole content
    # Pattern: word= followed by a value
    # We'll keep replacing until no more matches are found

    max_iterations = 10
    iteration = 0

    while iteration < max_iterations:
        iteration += 1
        original = content

        # Find all key=value patterns and replace them
        # We need to carefully extract the value, which ends at: , } ] or newline (at depth 0)
        pattern = r'(\w+)=([^,}\]]+?)(?=,|(?<!\\)\}|(?<!\\)\]|$)'

        def replace_match(match):
            key = match.group(1)
            value = match.group(2).strip()

            # Check if value is a number
            if re.match(r'^-?\d+(\.\d+)?$', value):
                return f'"{key}":{value}'

            # Check if value is boolean or null
            if value in ['true', 'false', 'null']:
                return f'"{key}":{value}'

            # Check if value starts with [ or { (nested structure)
            if value.startswith('[') or value.startswith('{'):
                return f'"{key}":{value}'

            # Everything else is a string
            # Escape backslashes and quotes
            value = value.replace('\\', '\\\\').replace('"', '\\"')
            return f'"{key}":"{value}"'

        content = re.sub(pattern, replace_match, content)

        # If nothing changed, we're done
        if content == original:
            break

    return content


def main():
    input_file = '/Users/matteominin/Desktop/tesi/pdf_extractor/samples/index.json'
    output_file = '/Users/matteominin/Desktop/tesi/pdf_extractor/samples/index1.json'

    # Read input
    with open(input_file, 'r', encoding='utf-8') as f:
        content = f.read()

    print("Converting malformed JSON to proper format...")

    # Fix the content
    fixed_content = fix_json_content(content)

    # Try to parse and validate
    try:
        data = json.loads(fixed_content)
        # Write properly formatted JSON
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
        print(f"✓ Successfully converted and validated!")
        print(f"✓ Output saved to: {output_file}")
        return 0
    except json.JSONDecodeError as e:
        # Save anyway for inspection
        with open(output_file, 'w', encoding='utf-8') as f:
            f.write(fixed_content)
        print(f"⚠ Conversion complete but validation failed:")
        print(f"  {e}")
        print(f"✓ Saved to: {output_file}")
        print("\nFirst 500 chars:")
        print(fixed_content[:500])
        return 1


if __name__ == '__main__':
    exit(main())

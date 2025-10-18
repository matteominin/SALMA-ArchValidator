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
    """
    # Remove smart quotes
    content = content.replace(''', "'").replace(''', "'")
    content = content.replace('"', '"').replace('"', '"')

    # Strategy: Use regex to find all key=value patterns and convert them

    # Pattern 1: {key=value -> {"key":"value"
    # Pattern 2: , key=value -> , "key":"value"
    # Pattern 3: [key=value -> [{"key":"value"

    # We need to be careful about values that are:
    # - Strings (need quotes)
    # - Numbers (no quotes)
    # - Booleans true/false (no quotes)
    # - Arrays [] (no quotes, keep as is)
    # - Objects {} (no quotes, keep as is)

    def replace_key_value(match):
        key = match.group(1)
        value = match.group(2)

        # Determine if value needs quotes
        value_stripped = value.strip()

        # Check if it's a number
        if value_stripped.replace('.', '').replace('-', '').isdigit():
            return f'"{key}":{value_stripped}'

        # Check if it's a boolean
        if value_stripped in ['true', 'false']:
            return f'"{key}":{value_stripped}'

        # Check if it starts with [ or { (array or object)
        if value_stripped.startswith('[') or value_stripped.startswith('{'):
            return f'"{key}":{value_stripped}'

        # Check if already quoted
        if value_stripped.startswith('"') and value_stripped.endswith('"'):
            return f'"{key}":{value_stripped}'

        # Otherwise it's a string that needs quotes
        # Find where the value ends (at , or } or ])
        return f'"{key}":"{value_stripped}"'

    # This is complex because values can span multiple patterns
    # Let's use a state machine approach

    result = []
    i = 0
    while i < len(content):
        # Check if we're at a key=value pattern
        if i < len(content) - 1:
            # Look for word=
            match = re.match(r'(\w+)=', content[i:])
            if match:
                key = match.group(1)
                result.append(f'"{key}":')
                i += len(match.group(0))

                # Now extract the value until we hit a delimiter
                value_start = i
                in_string = False
                in_array = 0
                in_object = 0

                while i < len(content):
                    char = content[i]

                    if char == '[' and not in_string:
                        in_array += 1
                    elif char == ']' and not in_string:
                        in_array -= 1
                    elif char == '{' and not in_string:
                        in_object += 1
                    elif char == '}' and not in_string:
                        in_object -= 1
                        if in_object < 0:
                            break
                    elif char == ',' and in_array == 0 and in_object == 0:
                        break

                    i += 1

                value = content[value_start:i]
                value_stripped = value.strip()

                # Decide if we need to quote the value
                needs_quotes = True

                if value_stripped.replace('.', '').replace('-', '').isdigit():
                    needs_quotes = False
                elif value_stripped in ['true', 'false', 'null']:
                    needs_quotes = False
                elif value_stripped.startswith('[') or value_stripped.startswith('{'):
                    needs_quotes = False
                elif value_stripped.startswith('"'):
                    needs_quotes = False

                if needs_quotes:
                    # Escape any quotes in the value
                    value_stripped = value_stripped.replace('"', '\\"')
                    result.append(f'"{value_stripped}"')
                else:
                    result.append(value_stripped)

                continue

        result.append(content[i])
        i += 1

    return ''.join(result)


def main():
    input_file = '/Users/matteominin/Desktop/tesi/pdf_extractor/samples/minin_extraction.json'
    output_file = '/Users/matteominin/Desktop/tesi/pdf_extractor/samples/minin_extraction_fixed.json'

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

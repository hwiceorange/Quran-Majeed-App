#!/usr/bin/env python3
import re

file_path = "shaheendevelopersAds_SDK/src/main/java/com/raiadnan/ads/sdk/format/AppOpenAd.java"

with open(file_path, 'r') as f:
    lines = f.readlines()

output_lines = []
i = 0
while i < len(lines):
    line = lines[i]
    stripped = line.strip()
    
    # Check if this line is "case APPLOVIN:" or "case APPLOVIN_MAX:" without already being commented
    if re.match(r'^\s*case (APPLOVIN|APPLOVIN_MAX):\s*$', line):
        # Look ahead to see if the next non-empty line is also a case statement or the start of code
        j = i + 1
        next_is_case = False
        while j < len(lines) and (lines[j].strip() == '' or lines[j].strip().startswith('//')):
            j += 1
        if j < len(lines) and re.match(r'^\s*case ', lines[j]):
            next_is_case = True
        
        if not next_is_case:
            # This is a standalone case or the last case before the code block starts
            # We need to comment out this case and everything until break;
            indent = len(line) - len(line.lstrip())
            output_lines.append(' ' * indent + '/* AppLovin SDK removed\n')
            output_lines.append(line)
            
            # Continue adding lines until we hit break;
            i += 1
            while i < len(lines):
                output_lines.append(lines[i])
                if lines[i].strip() == 'break;':
                    indent = len(lines[i]) - len(lines[i].lstrip())
                    output_lines.append(' ' * indent + '*/\n')
                    i += 1
                    break
                i += 1
            continue
        else:
            # This case is followed by another case, so they share the same code block
            # We need to group them
            indent = len(line) - len(line.lstrip())
            output_lines.append(' ' * indent + '/* AppLovin SDK removed\n')
            output_lines.append(line)
            
            # Add subsequent case lines
            i += 1
            while i < len(lines):
                current_line = lines[i]
                if re.match(r'^\s*case (APPLOVIN|APPLOVIN_MAX):\s*$', current_line):
                    output_lines.append(current_line)
                    i += 1
                elif current_line.strip().startswith('/*') and 'AppLovin' in current_line:
                    # Skip already commented section
                    i += 1
                else:
                    break
            
            # Now handle the code block
            while i < len(lines):
                output_lines.append(lines[i])
                if lines[i].strip() == 'break;':
                    indent = len(lines[i]) - len(lines[i].lstrip())
                    output_lines.append(' ' * indent + '*/\n')
                    i += 1
                    break
                i += 1
            continue
    
    output_lines.append(line)
    i += 1

with open(file_path, 'w') as f:
    f.writelines(output_lines)

print(f"Fixed {file_path}")



## Prompt

You are a system that consolidates multiple related features into a single, abstract, and domain-independent feature.
Input: A list of features, each with:

feature (name)
description (what it means)
evidence (example from text)
category
Task: Generate one consolidated feature that:
Summarizes all similar features into a single abstract, domain-independent feature name.
Provides a clear and general description that can apply to any project.
Gives one representative example from the inputs (can reference content without domain-specific terms).
States the number of original features it summarizes.
Output format (JSON):
{
  "numFeaturesSummarized": <number>,
  "feature": "<abstract, general feature name>",
  "description": "<general description capturing all key aspects>",
  "example": "<representative evidence, generalized if needed>"
}
Rules:
Merge duplicates and minor variations.
Preserve the meaning but remove project-specific details.
The feature should be applicable to any project.
Use the strongest or most illustrative evidence, but generalize domain-specific terms if necessary.
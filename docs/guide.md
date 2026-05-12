# SRM ID Mapping Toolkit — Step by Step Guide

---

## ⚠️ PROOF OF CONCEPT — Please Read First

> **This is not an official Adaptavist product.**
> It comes with no warranty, no support SLA, and no guarantee that it will work on your specific instance.
> **Test every updated script thoroughly before using it in production.**

This tool is a guide, not an auto-fixer. It makes its best attempt at replacing every ID it finds and gives you an updated script ready to test. But it cannot guarantee correctness. You must test every updated script thoroughly before using it in production. You must keep a backup of every original script before making any changes.

Before using it:
- Test it on a non-production instance first
- Review every change the tool suggests before applying it to a live script
- Keep a backup of your original scripts before making any changes

If you have questions, speak to your Adaptavist Customer Success Manager.

---

## What Problem Does This Solve?

When you migrate from Jira Data Center to Jira Cloud using JCMA (Jira Cloud Migration Assistant), all internal numeric IDs change. This includes:

- Custom field IDs (e.g. `customfield_10001` on DC becomes `customfield_10500` on Cloud)
- Custom field option IDs (the IDs for select list values, radio buttons, checkboxes, cascading selects, etc.)
- Issue type IDs, status IDs, priority IDs, resolution IDs, project IDs

If your ScriptRunner scripts reference any of these IDs directly — which is very common — those scripts will silently break after migration. The scripts still exist, but they are looking for IDs that no longer exist on Cloud.

**The key insight:** names do not change during JCMA migration, only IDs do. This toolkit uses names as a stable reference point to match your old DC IDs to their new Cloud equivalents.

---

## What the Toolkit Does

The toolkit has two parts:

**Part 1 — Export your IDs**
Two scripts export all IDs from your DC instance (before migration) and your Cloud instance (after migration) as CSV. You do not need to save these as files — you can copy and paste the output directly into the find-and-replace tool.

**Part 2 — Find and replace**
A third script takes those two CSV outputs and a ScriptRunner script you want to fix. It scans for DC IDs, replaces them where it can, and gives you a full report and an updated script ready to test.

### What it replaces automatically
- `customfield_XXXXX` string patterns — e.g. `issue.get('customfield_11000')`
- Long literals in `getCustomFieldValue()` and `setCustomFieldValue()` — e.g. `getCustomFieldValue(11003L)`
- `cf[XXXXX]` JQL syntax — e.g. `cf[11000] = "Alpha"`
- Project IDs in JQL — replaced with the project key (keys are preserved by JCMA, so this is always safe)
- Bare option IDs where context makes the match clear — e.g. an ID inside `contains('10100')` is treated as an option ID

### What it flags for manual review
- Bare numeric IDs where the context is ambiguous — the tool picks the most likely match and flags it as **BEST GUESS**. You must verify these before using the updated script.

### What it cannot do
- **DC Groovy Behaviour scripts** — Cloud Behaviours use TypeScript, not Groovy. The tool detects these scripts and tells you what to do, The tool detects these and directs you to SMS for conversion. Once SMS has converted the script to TypeScript, paste it back into the tool to fix the custom field IDs. See the Behaviours section below.
- It cannot scan your ScriptRunner scripts automatically — you must paste each script in manually
- It cannot update scripts in ScriptRunner for you — you must copy the fixed script back yourself
- It cannot resolve IDs for entities that did not migrate (e.g. custom issue types not in a project scheme)
- It cannot handle IDs stored in ScriptRunner Script Variables

---

## DC Groovy Behaviours — Important

If you paste a DC Groovy Behaviour script into the find-and-replace tool, it will detect it and show a clear warning. It will not attempt to fix the script.

**Why?** DC Behaviours are written in Groovy. Cloud Behaviours are written in TypeScript. They use completely different APIs. The script cannot be updated — it must be rewritten from scratch.

The find-and-replace tool will tell you:
- Which fields are referenced in the script (so you know what to look up)
- What the equivalent Cloud TypeScript API calls look like
- Where to go for help

**The ScriptRunner Migration Suite can convert DC Groovy Behaviours to Cloud TypeScript automatically.**

- Try it: https://migrationsuite.scriptrunnerhq.com/
- Docs: https://www.scriptrunnerhq.com/atlassian-apps/jira/scriptrunner-migration-suite

### After SMS converts your Behaviour

Once SMS has converted your DC Groovy Behaviour to Cloud TypeScript, the converted script will still contain your original DC custom field IDs (e.g. `customfield_11000`). SMS converts the syntax — it does not update the IDs.

**This is where the find-and-replace tool comes back in.** Paste the SMS-converted TypeScript script into `SCRIPT_TO_FIX` and run it. The tool will detect and replace any `customfield_XXXXX` patterns exactly as it does for Groovy scripts. Option IDs are not a concern for TypeScript Behaviours — SMS uses option names (e.g. `"Alpha"`) rather than numeric IDs.

The full workflow for Behaviours is:
1. Use SMS to convert your DC Groovy Behaviour to Cloud TypeScript
2. Paste the TypeScript output into `SCRIPT_TO_FIX` in the find-and-replace tool
3. Run the tool — custom field IDs will be replaced automatically
4. Copy the updated TypeScript and paste it back into ScriptRunner Cloud
5. Test thoroughly before using in production

---

## Before You Start

You will need:
- Access to your **Jira Data Center** instance with ScriptRunner installed
- Access to your **Jira Cloud** instance with ScriptRunner installed (post-migration)
- A text editor that saves plain text — **use VS Code, not TextEdit on Mac** (TextEdit saves as RTF which will corrupt the CSV if you choose to save the output to a file)

---

## Step 1 — Export IDs from Data Center

Do this **before** running your JCMA migration.

1. Open your Jira Data Center instance
2. Go to **ScriptRunner → Script Console**
3. Open the file `scripts/dc-export.groovy` from this repository
4. Copy the entire script and paste it into the Script Console
5. Click **Run**
6. Click the **Logs tab** (not the Result tab — the output does not appear there)
7. Scroll to the very bottom — the CSV is always last
8. You will see this line — **ignore it, do not copy it:**
   ```
   >>>>>>>>>> CSV START <<<<<<<<<<
   ```
9. Start copying from the **next line** — the one starting with:
   ```
   "EntityType","Name","ParentName","DC_ID","Status"
   ```
10. Keep copying until the last data row
11. Stop when you see — **ignore it, do not copy it:**
    ```
    >>>>>>>>>> CSV END <<<<<<<<<<
    ```
12. You can either:
    - **Paste directly** into `DC_CSV` in the find-and-replace tool (no file needed), or
    - Save to a file in VS Code as `dc-export.csv` if you plan to fix many scripts in one sitting

> **Tip:** The first line you copy should always be `"EntityType","Name","ParentName","DC_ID","Status"`. If it is not, scroll down further — the CSV is always at the very bottom of the Logs tab.

---

## Step 2 — Run Your JCMA Migration

Run your JCMA migration as normal. Come back to this guide once the migration is complete and your Cloud instance is live.

> **Before you migrate:** Make sure any custom issue types and custom statuses you want to preserve are included in a project's issue type scheme and workflow. JCMA only migrates issue types that are in the scheme of a project being migrated, and only migrates statuses that are in a workflow assigned to that project. Anything outside that scope will not migrate and will appear as UNRESOLVED in the mapping.

---

## Step 3 — Export IDs from Cloud

Do this **after** your JCMA migration is complete.

The Cloud export is split into two scripts. Run both. This is intentional — splitting them keeps each script fast and well within the 240 second execution limit.

### Step 3a — Export custom fields

1. Open your Jira Cloud instance
2. Go to **ScriptRunner → Script Console**
3. Open the file `scripts/cloud-export-fields.groovy` from this repository
4. Copy the entire script and paste it into the Script Console
5. Click **Run**
6. Click the **Logs tab** (not the Result tab)
7. Scroll to the very bottom
8. You will see this line — **ignore it, do not copy it:**
   ```
   >>>>>>>>>> CSV START <<<<<<<<<<
   ```
9. Start copying from the **next line** — the one starting with:
   ```
   "EntityType","Name","ParentName","Cloud_ID","Status"
   ```
10. Keep copying until the last `CustomFieldOption` line
11. Stop when you see — **ignore it, do not copy it:**
    ```
    >>>>>>>>>> CSV END <<<<<<<<<<
    ```
12. Keep this output ready — you will paste it into `CLOUD_CSV` in Step 4

> **Note:** This script only fetches options for fields that support them (select lists, multi-selects, radio buttons, checkboxes, cascading selects). All other field types are exported without making extra API calls. This keeps the script fast on large instances.

> **Note:** On instances with a large number of custom fields, this script may still take a minute or two. This is normal. If it times out (240 second limit), contact your Adaptavist Customer Success Manager.

### Step 3b — Export system entities

1. Still in **ScriptRunner → Script Console** on your Cloud instance
2. Open the file `scripts/cloud-export-system.groovy` from this repository
3. Copy the entire script and paste it into the Script Console
4. Click **Run**
5. Click the **Logs tab**
6. Scroll to the very bottom
7. Copy the output between the markers, exactly as in Step 3a
8. Keep this output ready — you will paste it directly below the fields output in Step 4

> **Note:** This script makes only 5 API calls (issue types, statuses, priorities, resolutions, projects) and completes in seconds regardless of instance size.

---

## Step 4 — Fix Your Scripts

Repeat this step for every ScriptRunner script you want to fix.

1. Open your Jira Cloud instance
2. Go to **ScriptRunner → Script Console**
3. Open the file `scripts/find-and-replace.groovy` from this repository
4. Copy the entire script and paste it into the Script Console
5. Find **SECTION 1 — YOUR INPUTS** at the top of the script
6. Paste your DC export output into `DC_CSV`
7. Paste your fields export output (from Step 3a) into `CLOUD_CSV_FIELDS`
8. Paste your system export output (from Step 3b) into `CLOUD_CSV_SYSTEM`
9. Find the script you want to fix in ScriptRunner (e.g. go to ScriptRunner → Listeners, Post Functions, Jobs, Escalation Services — open the script, copy the script body)
10. Paste that script body into `SCRIPT_TO_FIX`
11. Click **Run**
12. Click the **Logs tab** to read the report

> **Tip:** You do not need to save the CSV outputs as files. If you are fixing scripts one at a time in a single session, you can paste the export outputs directly and skip saving to disk entirely. Files are only useful if you plan to come back later or fix many scripts across multiple sessions.

---

## Step 5 — Read the Report and Apply the Changes

The report appears in the **Logs tab**. It has three sections.

### ✅ AUTO-REPLACED
IDs that were found in the mapping and replaced with confidence. These are already updated in the fixed script at the bottom of the report. You do not need to do anything for these — but you should still verify them when you test the script.

This includes:
- `customfield_XXXXX` replacements
- Long literals in `getCustomFieldValue()` / `setCustomFieldValue()`
- `cf[XXXXX]` JQL references
- Project IDs in JQL (replaced with the project key)
- Option IDs where the context made the match clear

### ⚠️ BEST GUESS — PLEASE VERIFY
Bare numeric IDs where the tool made its best attempt but is not certain. The tool picks the most likely match based on context and flags it for you to check.

**How to handle these:**
- Look at the context in your script — what is this ID being used for?
- Check the reason shown in the report — it explains why that match was chosen and lists any other possible matches
- If the guess is wrong, find the correct Cloud ID in your Cloud CSV and update it manually in the fixed script

### ❌ UNRESOLVED — ACTION NEEDED
IDs that exist on DC but could not be matched to a Cloud equivalent. This usually means:
- The entity did not migrate (e.g. a custom issue type not in a project scheme)
- JCMA renamed the field by appending `(migrated)` due to a naming conflict
- The entity name is duplicated and the tool cannot safely pick one

For each UNRESOLVED entry, you will need to find the correct Cloud ID manually and update the script yourself.

### 🔧 Your Updated Script
The modified script with all AUTO-REPLACED changes already applied. Once you have also handled any BEST GUESS and UNRESOLVED entries, copy this script and paste it back into ScriptRunner to replace the original.

> **⚠️ Always test the updated script before using it in production.** The tool makes its best attempt — it does not guarantee correctness.

---

## Step 6 — Optional: View the Full Mapping Reference

If you want to see the complete DC → Cloud ID mapping for your instance without fixing a specific script, set `MODE = 'SHOW_MAPPING'` at the top of the find-and-replace script and run it. The Logs tab will show a formatted table of every entity, its DC ID, its Cloud ID, and its status.

This is useful for manually looking up IDs or verifying the mapping before running it against your scripts.

---

## Known Limitations

This is a proof of concept. The following limitations are known and documented.

1. **You must paste scripts in manually.** ScriptRunner Cloud has no API to list all script bodies, so the tool cannot scan your scripts automatically.

2. **Duplicate field names.** If two custom fields share the same name, the tool cannot safely determine which one maps to which. These are flagged as AMBIGUOUS and must be resolved manually.

3. **Entities that did not migrate.** If a custom issue type, status, or resolution was not included in your JCMA migration scope, it will appear as UNRESOLVED. You will need to recreate it on Cloud and update the ID manually.

4. **JCMA can rename fields.** If JCMA detects a naming conflict on Cloud, it may rename a field by appending `(migrated)`. These fields will appear as UNRESOLVED in the mapping because the name no longer matches. Check your Cloud CSV for fields with `(migrated)` in the name.

5. **Project IDs in JQL are replaced with the project key.** Project keys are preserved by JCMA, so this replacement is always safe. However, if your script uses a project ID outside of a JQL `project =` clause, the tool may not detect it.

6. **Script Variables are not covered.** IDs stored in ScriptRunner Script Variables are not detected or updated by this tool.

7. **The Cloud fields export may be slow on very large instances.** ScriptRunner Cloud scripts have a 240 second execution limit. The fields export script only fetches options for select-type fields and uses pagination, which significantly reduces the number of API calls compared to earlier versions. It has been tested on instances with ~94 custom fields. On instances with significantly more fields, it may still approach the time limit. If it times out, contact your Adaptavist Customer Success Manager.

8. **DC Groovy Behaviours cannot be fixed directly.** Cloud Behaviours use TypeScript, not Groovy. The tool detects DC Groovy Behaviours and directs you to SMS for conversion. However, once SMS has converted the script to TypeScript, you can paste the TypeScript output back into the find-and-replace tool to update the custom field IDs. See the Behaviours section above.

9. **Duplicate system entity names are not flagged in the Cloud export.** Issue types, statuses, and other system entities with duplicate names (e.g. multiple issue types all named "Task") are not flagged as AMBIGUOUS in the Cloud export. The find-and-replace tool will flag them when building the mapping, but you should be aware that system entity matches may be less reliable than custom field matches.

10. **Cascading select separator collision.** The tool uses ` > ` as a separator to identify child options (e.g. `SRM Cascading Field > Category A`). If a parent option value itself contains ` > ` (e.g. `Greater > Than`), the separator may be ambiguous. The export and mapping still work correctly, but be aware of this if you have parent options with `>` in their names.

11. **Long literal replacement assumes standard code formatting.** The tool detects Long literals in `getCustomFieldValue()` and `setCustomFieldValue()` using a pattern that allows up to 10 spaces between the method name and the argument. Standard code formatting always works. Unusual spacing (more than 10 spaces) may not be detected.

12. **This is a proof of concept.** See the disclaimer at the top of this guide.

---

## Findings Log

The following findings were made during development and testing. They are documented here for transparency.

| # | Finding | Impact |
|---|---|---|
| F1 | `.customFieldObjectsByName` does not exist on DC | Use `.customFieldObjects.find { it.name == x }` |
| F2 | `customFields` property does not exist on HAPI Issue in Cloud Script Console | Use `getCustomFieldValue(Long)` or REST API |
| F3 | JCMA renames duplicate field names by appending `(migrated)` | These appear as UNRESOLVED in the mapping |
| F4 | Project keys are preserved by JCMA | Scripts using project keys need no changes |
| F5 | All IDs change after JCMA | Every hardcoded numeric ID will be wrong |
| F6 | JCMA only migrates issue types in the project's issue type scheme | Add custom issue types to scheme before JCMA |
| F7 | JCMA only migrates statuses in a workflow assigned to the project | Ensure custom statuses are in a workflow |
| F8 | Mac TextEdit saves as RTF not CSV | Always use VS Code or Cursor |
| F9 | Cloud export performance acceptable on ~94 field instances | Untested on 500+ fields — documented as limitation |
| F10 | ScriptRunner Cloud Script Console Result tab does not render HTML | HTML approach abandoned — use logger.warn() |
| F11 | ScriptRunner Cloud has no public API to list all script bodies | Customer pastes scripts one at a time |
| F12 | ScriptRunner Cloud scripts have a 240 second execution timeout | Cloud export may time out on large instances |
| F13 | Script-level variables are NOT visible inside Groovy methods in ScriptRunner Cloud | Inline all logic — avoid defining methods |
| F14 | `mapping` is a reserved binding name in ScriptRunner Cloud Script Console | Use `idMapping` instead |
| F15 | DC export output appears in Logs tab | Copy from Logs tab between markers |
| F16 | Cloud export output appears in Logs tab | Copy from Logs tab between markers |
| F17 | Find-and-replace report appears in Logs tab | Read report from Logs tab |
| F18 | `cf[XXXXX]` JQL syntax was not detected by v1/v2 of find-and-replace | Fixed in v3 — now auto-replaced |
| F19 | Field IDs as bare Long literals were not detected by v1/v2 | Fixed in v3 — now auto-replaced |
| F20 | DC Groovy Behaviours got a false green tick in v1/v2 | Fixed in v3 — now shows clear warning + SMS links |
| F21 | Returning a string from Script Console shows it escaped with `\"` and `\n` | Use `logger.warn()` instead of returning the value |
| F22 | DC Script Console Result tab renders newlines as spaces | Use `logger.warn()` — Logs tab renders correctly |
| F23 | Cloud export was making API calls for every field including non-option fields | Fixed — v2 of cloud-export-fields only fetches options for select-type fields |
| F24 | Cloud export had no pagination for options | Fixed — v2 fetches up to 100 options per page and loops |
| F25 | Long literal regex uses bounded variable-length lookbehinds (`\s{0,10}`) | Valid in Java — bounded lookbehinds are supported. Edge case: more than 10 spaces between method name and argument will not be detected. Not a real-world concern with standard formatting. |
| F26 | SMS converts DC Groovy Behaviours to TypeScript but does not update custom field IDs | The find-and-replace tool works on SMS-converted TypeScript Behaviours — `customfield_XXXXX` patterns are detected and replaced correctly. Option IDs are not an issue as SMS uses option names not numeric IDs in TypeScript. |

---

## Toolkit Philosophy

> This tool is a guide, not an auto-fixer.
>
> It shows you what changed, makes its best attempt at replacing every ID, and gives you an updated script ready to test. Some replacements are made with confidence. Others are best guesses based on context. A few will be left for you to resolve manually.
>
> You must test everything before using it in production. The tool cannot guarantee correctness — it can only give you a head start.
>
> **This is a proof of concept.** It was built to demonstrate what is possible, not to replace careful human review.

---

## Getting Help

If you get stuck, contact your Adaptavist Customer Success Manager.

For DC Groovy Behaviour migration specifically, the ScriptRunner Migration Suite is the right tool:
- https://migrationsuite.scriptrunnerhq.com/
- https://www.scriptrunnerhq.com/atlassian-apps/jira/scriptrunner-migration-suite

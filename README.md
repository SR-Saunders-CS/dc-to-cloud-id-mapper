# Jira(DC) to Jira(Cloud) ID Mapping Toolkit

> ## ⚠️ PROOF OF CONCEPT
> **This is not an official Adaptavist product.**
> It comes with no warranty, no support SLA, and no guarantee it will work on your specific instance.
> **Test every updated script thoroughly before using it in production.**
> If you have questions regarding how to migrate from DC to Cloud, speak to a ScriptRunner [Customer Success Manager](https://www.scriptrunnerhq.com/locker/customer-success-team).

A proof-of-concept toolkit that fixes hardcoded IDs in ScriptRunner scripts after a Jira Data Center to Cloud migration.

---

## Where This Fits in Your Migration

The typical migration journey looks like this:

1. **Migrate Jira** — use JCMA to move your Jira Data Center instance to Cloud
2. **Migrate your ScriptRunner scripts** — use the [ScriptRunner Migration Suite (SMS)](https://migrationsuite.scriptrunnerhq.com/) to move your scripts to Cloud
3. **Fix the IDs** — this is where this toolkit comes in

SMS converts your DC scripts into Cloud compatible script. But JCMA can change every internal numeric ID during migration — custom field IDs, option IDs, issue type IDs, status IDs, project IDs — all of them. Scripts that reference those IDs directly will be pointing at IDs that no longer exist (or are now different). **This toolkit finds those IDs and replaces them.** one script at a time, of your choosing.

If that's where you are — scripts on Cloud, IDs are wrong — read on.

---

## How It Works

Names stay the same during JCMA migration — IDs do not. This toolkit exports all IDs from your DC instance and your Cloud instance, uses names as a stable join key to match them up, then scans your scripts and replaces every DC ID it finds with the correct Cloud equivalent.

---

## What You Need

- Your **Jira Data Center** instance with ScriptRunner installed (or your DC export CSV if you already ran the export before migration)
- Your **Jira Cloud** instance with ScriptRunner installed
- The ScriptRunner scripts you want to fix

---

## The Four Steps

**Step 1 — Export from your DC instance**
Run [`scripts/dc-export.groovy`](scripts/dc-export.groovy) in ScriptRunner DC → Script Console. Copy the output from the Logs tab (please read each indivdual scripts comment section, regarding what to copy).

> ⚠️ **Do this before you shut down your DC instance.** Once DC is gone, the IDs are gone with it and this toolkit cannot help you. If DC is still live — even temporarily post-migration — run this now.

**Step 2 — Export from Cloud**
Run [`scripts/cloud-export-fields.groovy`](scripts/cloud-export-fields.groovy) in ScriptRunner Cloud → Script Console. Copy the output from the Logs tab.
Then run [`scripts/cloud-export-system.groovy`](scripts/cloud-export-system.groovy) and copy that output too.

**Step 3 — Fix your script**
Run [`scripts/find-and-replace.groovy`](scripts/find-and-replace.groovy) in ScriptRunner Cloud → Script Console.
Paste your DC export into `DC_CSV`, your fields export into `CLOUD_CSV_FIELDS`, your system export into `CLOUD_CSV_SYSTEM`, and the script you want to fix into `SCRIPT_TO_FIX`. Run it.

**Step 4 — Read the report**
The Logs tab shows you every ID that was found, what it was replaced with, and how confident the tool is. At the bottom is your updated script, ready to test.

---

## What the Tool Replaces Automatically

- `customfield_XXXXX` patterns
- Long literals in `getCustomFieldValue()` and `setCustomFieldValue()`
- `cf[XXXXX]` JQL syntax
- Project IDs in JQL (replaced with the project key — keys are preserved by JCMA)
- Option IDs where the context makes the match clear

Everything else is flagged for you to review manually.

---

## What the Tool Cannot Do

- **DC Groovy Behaviours** — Cloud Behaviours use TypeScript, not Groovy. The tool detects DC Groovy Behaviours and directs you to SMS for conversion. Once SMS has converted the script to TypeScript, paste it back into the tool to fix the custom field IDs automatically.
- Scan your scripts automatically — you paste them in one at a time
- Resolve IDs for entities that did not migrate
- Detect IDs stored in ScriptRunner Script Variables

---

## ⚠️ Before You Use the Output

The tool gives you a head start — it does not give you a finished script. You must:

1. Read every entry in the report, especially anything marked **BEST GUESS** or **UNRESOLVED**
2. Test the updated script on a non-production instance before deploying it
3. Keep a backup of your original script

---

## Full Documentation

For detailed step-by-step instructions, the full limitations list, and the findings log, see the [Step by Step Guide](docs/guide.md).

---

## Questions?

Speak to a ScriptRunner [Customer Success Manager](https://www.scriptrunnerhq.com/locker/customer-success-team).
